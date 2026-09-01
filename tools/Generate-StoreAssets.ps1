param(
    [string]$ProjectRoot = (Split-Path -Parent $PSScriptRoot)
)

Add-Type -AssemblyName System.Drawing

$outDir = Join-Path $ProjectRoot "build\store-assets"
$previewPath = Join-Path $ProjectRoot "build\safeclip-preview.png"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

function New-RoundedPath {
    param([float]$X, [float]$Y, [float]$Width, [float]$Height, [float]$Radius)
    $path = [System.Drawing.Drawing2D.GraphicsPath]::new()
    $diameter = $Radius * 2
    $path.AddArc($X, $Y, $diameter, $diameter, 180, 90)
    $path.AddArc($X + $Width - $diameter, $Y, $diameter, $diameter, 270, 90)
    $path.AddArc($X + $Width - $diameter, $Y + $Height - $diameter, $diameter, $diameter, 0, 90)
    $path.AddArc($X, $Y + $Height - $diameter, $diameter, $diameter, 90, 90)
    $path.CloseFigure()
    return $path
}

function Draw-SafeClipIcon {
    param(
        [System.Drawing.Graphics]$Graphics,
        [float]$X,
        [float]$Y,
        [float]$Size
    )

    $scale = $Size / 108.0
    $rect = [System.Drawing.RectangleF]::new($X, $Y, $Size, $Size)
    $gradient = [System.Drawing.Drawing2D.LinearGradientBrush]::new(
        $rect,
        [System.Drawing.ColorTranslator]::FromHtml("#06224A"),
        [System.Drawing.ColorTranslator]::FromHtml("#00A8C8"),
        45.0
    )
    $Graphics.FillRectangle($gradient, $rect)
    $gradient.Dispose()

    $lineColor = [System.Drawing.ColorTranslator]::FromHtml("#DDEEFF")
    $focusPen = [System.Drawing.Pen]::new($lineColor, 6 * $scale)
    $focusPen.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
    $focusPen.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
    foreach ($segment in @(
        @(25,25,43,25), @(25,25,25,43), @(83,25,65,25), @(83,25,83,43),
        @(25,83,43,83), @(25,83,25,65), @(83,83,65,83), @(83,83,83,65)
    )) {
        $Graphics.DrawLine($focusPen,
            $X + $segment[0] * $scale, $Y + $segment[1] * $scale,
            $X + $segment[2] * $scale, $Y + $segment[3] * $scale)
    }
    $focusPen.Dispose()

    $shield = [System.Drawing.Drawing2D.GraphicsPath]::new()
    $shield.StartFigure()
    $shield.AddBezier($X+54*$scale,$Y+24*$scale,$X+44*$scale,$Y+30*$scale,$X+36*$scale,$Y+31*$scale,$X+32*$scale,$Y+35*$scale)
    $shield.AddBezier($X+32*$scale,$Y+35*$scale,$X+29*$scale,$Y+50*$scale,$X+35*$scale,$Y+65*$scale,$X+54*$scale,$Y+77*$scale)
    $shield.AddBezier($X+54*$scale,$Y+77*$scale,$X+73*$scale,$Y+65*$scale,$X+79*$scale,$Y+50*$scale,$X+76*$scale,$Y+35*$scale)
    $shield.AddBezier($X+76*$scale,$Y+35*$scale,$X+72*$scale,$Y+31*$scale,$X+64*$scale,$Y+30*$scale,$X+54*$scale,$Y+24*$scale)
    $shield.CloseFigure()
    $shieldBrush = [System.Drawing.SolidBrush]::new([System.Drawing.ColorTranslator]::FromHtml("#F7FBFF"))
    $shieldPen = [System.Drawing.Pen]::new([System.Drawing.ColorTranslator]::FromHtml("#68C8FF"), 2.4*$scale)
    $Graphics.FillPath($shieldBrush, $shield)
    $Graphics.DrawPath($shieldPen, $shield)
    $shieldBrush.Dispose(); $shieldPen.Dispose(); $shield.Dispose()

    foreach ($circle in @(
        @(54,54,14,"#051126"), @(54,54,10.5,"#123E70"),
        @(54,54,7,"#159BFF"), @(54,54,4,"#071A33"), @(49.2,48.8,2.4,"#EAF8FF")
    )) {
        $brush = [System.Drawing.SolidBrush]::new([System.Drawing.ColorTranslator]::FromHtml($circle[3]))
        $r = [float]$circle[2] * $scale
        $Graphics.FillEllipse($brush, $X+$circle[0]*$scale-$r, $Y+$circle[1]*$scale-$r, 2*$r, 2*$r)
        $brush.Dispose()
    }

    $stemPen = [System.Drawing.Pen]::new($lineColor, 3.2*$scale)
    $stemPen.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
    $stemPen.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
    $Graphics.DrawLine($stemPen, $X+54*$scale,$Y+69*$scale,$X+54*$scale,$Y+89*$scale)
    $stemPen.Dispose()

    $lightBrush = [System.Drawing.SolidBrush]::new($lineColor)
    $Graphics.FillPolygon($lightBrush, @(
        [System.Drawing.PointF]::new($X+51.2*$scale,$Y+74*$scale),
        [System.Drawing.PointF]::new($X+56.8*$scale,$Y+74*$scale),
        [System.Drawing.PointF]::new($X+57.8*$scale,$Y+79*$scale),
        [System.Drawing.PointF]::new($X+50.2*$scale,$Y+79*$scale)))
    $Graphics.FillPolygon($lightBrush, @(
        [System.Drawing.PointF]::new($X+49.8*$scale,$Y+83*$scale),
        [System.Drawing.PointF]::new($X+58.2*$scale,$Y+83*$scale),
        [System.Drawing.PointF]::new($X+59.6*$scale,$Y+90*$scale),
        [System.Drawing.PointF]::new($X+48.4*$scale,$Y+90*$scale)))
    $lightBrush.Dispose()

    $alertBrush = [System.Drawing.SolidBrush]::new([System.Drawing.ColorTranslator]::FromHtml("#FF5A1F"))
    $Graphics.FillEllipse($alertBrush, $X+70.2*$scale,$Y+27*$scale,11.6*$scale,11.6*$scale)
    $alertBrush.Dispose()
}

$icon = [System.Drawing.Bitmap]::new(512, 512)
$iconGraphics = [System.Drawing.Graphics]::FromImage($icon)
$iconGraphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$iconGraphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
Draw-SafeClipIcon -Graphics $iconGraphics -X 0 -Y 0 -Size 512
$icon.Save((Join-Path $outDir "app-icon-512.png"), [System.Drawing.Imaging.ImageFormat]::Png)
$iconGraphics.Dispose(); $icon.Dispose()

$feature = [System.Drawing.Bitmap]::new(1024, 500)
$g = [System.Drawing.Graphics]::FromImage($feature)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
$background = [System.Drawing.SolidBrush]::new([System.Drawing.ColorTranslator]::FromHtml("#001020"))
$g.FillRectangle($background, 0, 0, 1024, 500)
$background.Dispose()

Draw-SafeClipIcon -Graphics $g -X 54 -Y 112 -Size 180
$white = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::White)
$cyan = [System.Drawing.SolidBrush]::new([System.Drawing.ColorTranslator]::FromHtml("#1FD1F2"))
$muted = [System.Drawing.SolidBrush]::new([System.Drawing.ColorTranslator]::FromHtml("#B8CCE7"))
$titleFont = [System.Drawing.Font]::new("Malgun Gothic", 48, [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
$tagFont = [System.Drawing.Font]::new("Malgun Gothic", 25, [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
$bodyFont = [System.Drawing.Font]::new("Malgun Gothic", 18, [System.Drawing.FontStyle]::Regular, [System.Drawing.GraphicsUnit]::Pixel)
$tagline = [System.Text.Encoding]::UTF8.GetString(
    [Convert]::FromBase64String("67iU656Z67CV7IqkIOyYgeyDgeydhCDsib3qs6Ag67mg66W06rKMIOygnOy2nA=="))
$description = [System.Text.Encoding]::UTF8.GetString(
    [Convert]::FromBase64String("7ZmV7J24IMK3IO2OuOynkSDCtyDsoJzstpwg7ZiE7Zmp7J2EIO2VnOqzs+yXkOyEnA=="))
$g.DrawString("SafeClip", $titleFont, $white, 52, 306)
$g.DrawString($tagline, $tagFont, $cyan, 52, 370)
$g.DrawString($description, $bodyFont, $muted, 54, 418)

if (Test-Path $previewPath) {
    $preview = [System.Drawing.Image]::FromFile($previewPath)
    $framePath = New-RoundedPath -X 590 -Y 32 -Width 390 -Height 436 -Radius 18
    $oldClip = $g.Clip
    $g.SetClip($framePath)
    $source = [System.Drawing.Rectangle]::new(0, 110, $preview.Width, 1450)
    $destination = [System.Drawing.Rectangle]::new(590, 32, 390, 436)
    $g.DrawImage($preview, $destination, $source, [System.Drawing.GraphicsUnit]::Pixel)
    $g.Clip = $oldClip
    $border = [System.Drawing.Pen]::new([System.Drawing.ColorTranslator]::FromHtml("#1B6A9A"), 3)
    $g.DrawPath($border, $framePath)
    $border.Dispose(); $framePath.Dispose(); $preview.Dispose()
}

$accent = [System.Drawing.SolidBrush]::new([System.Drawing.ColorTranslator]::FromHtml("#FF5A1F"))
$g.FillRectangle($accent, 0, 486, 1024, 14)
$accent.Dispose()
$white.Dispose(); $cyan.Dispose(); $muted.Dispose()
$titleFont.Dispose(); $tagFont.Dispose(); $bodyFont.Dispose()
$feature.Save((Join-Path $outDir "feature-graphic-1024x500.png"), [System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose(); $feature.Dispose()

Write-Output "Generated store assets in $outDir"
