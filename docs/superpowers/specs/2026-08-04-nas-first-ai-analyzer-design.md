# NAS-First AI Analyzer Design

## Summary

SafeClip will start its AI work with a low-cost PC-assisted analyzer. The NAS remains the video store, review server, and durable result store. A local Windows PC with an i7-7700 and GTX 1060 runs batch analysis when it is available.

The first target is not automatic legal judgment. The first target is to analyze one uploaded blackbox video, produce object and plate candidates, save a structured JSON result, and let a human reviewer correct the result. Those corrections become future training data.

## Goals

- Keep cloud AI costs at zero for the first prototype.
- Use the NAS aggressively for storage and review workflow.
- Use the PC for GPU-assisted batch inference.
- Produce analysis output that the company review web can display later.
- Store AI predictions and human corrections separately so the data can be used for learning.

## Non-Goals

- Do not implement fully automatic reporting.
- Do not treat AI output as legally final.
- Do not run expensive full-frame, full-FPS analysis for every uploaded video.
- Do not require a paid cloud AI provider for the first version.
- Do not put GPU-heavy inference directly on the Synology DS214 NAS.

## Recommended Order

### B. Analyzer Contract First

Create the analyzer as a small standalone worker with a clear input and output contract before wiring it to NAS automation.

Input:

```text
video file path
optional submission id
optional user-entered incident time
optional user-entered incident location text
output directory
```

Output:

```text
analysis JSON
sample frames
annotated preview frames
plate candidate crops
worker log
```

The first command shape should be simple:

```text
python analyze_video.py --video path/to/video.mp4 --submission-id sample-001 --output output/sample-001
```

### A. PC AI Environment Second

Install the AI runtime on the Windows analysis PC after the analyzer contract is stable.

Initial dependencies:

```text
Python 3.10 or 3.11
FFmpeg
NVIDIA driver
CUDA-compatible PyTorch build
Ultralytics YOLO
OpenCV
EasyOCR or PaddleOCR
```

Start with small models to fit the GTX 1060 comfortably:

```text
YOLOv8n or YOLOv8s for vehicle, person, motorcycle, bus, truck, and traffic light detection
EasyOCR first if setup speed matters
PaddleOCR later if Korean plate OCR quality needs improvement
```

### C. NAS Integration Third

Once one local video can be analyzed, connect the analyzer to NAS folders.

Recommended NAS folder shape:

```text
/volume1/SafeClipUploads/
  original/
  analysis-json/
  frames/
  annotated-frames/
  plate-crops/
  labels/
  reviewed/
```

The first NAS integration can be manual:

```text
copy or map NAS video folder to Windows
run analyzer on one selected video
write output back to the NAS result folders
```

After that works, add a queue:

```text
NAS upload complete
create pending analysis job
PC worker polls pending jobs
PC analyzes one job at a time
PC writes result JSON
review web reads result JSON
reviewer corrects fields
corrections are saved as labels
```

## First Analyzer Behavior

The first version should analyze videos in two passes.

Pass 1: broad scan

```text
sample 1 frame per second
detect vehicles, people, motorcycles, buses, trucks, and traffic lights
measure rough motion change between sampled frames
record candidate event windows
```

Pass 2: focused scan

```text
for candidate windows, sample up to 5 frames per second
save useful frames
crop likely plate regions near vehicle boxes
run OCR on candidate plate crops when OCR is enabled
```

This keeps GTX 1060 processing realistic and avoids wasting time on every frame of long videos.

## Analysis JSON

Each analysis should write a single JSON document next to its generated assets.

Example:

```json
{
  "schemaVersion": 1,
  "submissionId": "sample-001",
  "video": {
    "fileName": "sample.mp4",
    "durationSec": 63.4,
    "width": 1920,
    "height": 1080,
    "fps": 30.0
  },
  "analysis": {
    "status": "completed",
    "startedAt": "2026-08-04T10:00:00+09:00",
    "completedAt": "2026-08-04T10:03:00+09:00",
    "framesAnalyzed": 96,
    "modelNames": ["yolov8n"]
  },
  "objectDetections": [
    {
      "sec": 12.0,
      "label": "car",
      "confidence": 0.87,
      "box": { "x": 413, "y": 522, "w": 288, "h": 144 }
    }
  ],
  "eventCandidates": [
    {
      "startSec": 22.0,
      "endSec": 31.0,
      "type": "rapid_motion_change",
      "confidence": 0.62
    }
  ],
  "plateCandidates": [
    {
      "sec": 24.2,
      "cropPath": "plate-crops/sample-001_024.2_01.jpg",
      "ocrText": "12GA3456",
      "ocrConfidence": 0.78
    }
  ],
  "violationCandidates": [
    {
      "type": "signal_violation",
      "confidence": 0.54,
      "evidenceSec": [22.0, 31.0],
      "reason": "traffic light and vehicle movement appeared in the same candidate window"
    }
  ],
  "review": {
    "status": "waiting_review",
    "finalViolationType": null,
    "finalPlateText": null,
    "reviewerMemo": null
  }
}
```

## Violation Candidate Scope

The first violation candidates should be conservative:

```text
collision_or_near_miss
signal_violation
lane_change_or_cut_in
center_line_crossing
pedestrian_risk
unknown_traffic_risk
```

The analyzer may recommend these labels, but the company reviewer must confirm or correct them.

## Review And Training Data

Human review must preserve both the AI prediction and the final correction.

Store review labels separately:

```json
{
  "submissionId": "sample-001",
  "aiViolationCandidates": ["signal_violation"],
  "finalViolationType": "lane_change_or_cut_in",
  "aiPlateCandidates": ["12GA3458", "12GA3456"],
  "finalPlateText": "12GA3456",
  "finalEventStartSec": 23.0,
  "finalEventEndSec": 30.0,
  "reportable": true,
  "reviewerMemo": "Reviewer corrected the violation type after watching the event window."
}
```

This label file is more valuable than the first AI result because it becomes SafeClip's future training data.

## Error Handling

- If the video cannot be opened, write `analysis.status = "failed"` and include an error message.
- If YOLO runs but OCR fails, keep object detections and mark OCR as unavailable.
- If GPU inference fails, allow a CPU fallback for short test videos only.
- If output folders are missing, create them before analysis.
- If the NAS is unavailable, write results locally and retry copying later.

## Privacy And Safety

- Do not expose raw uploaded videos through public URLs.
- Treat plate crops as sensitive evidence.
- Keep AI results labeled as candidates until human review.
- Do not auto-submit legal reports from AI output.
- Keep user consent requirements from the submission MVP before storing or using videos for training.

## First Acceptance Criteria

- A developer can run one command against a local sample video.
- The analyzer creates an output folder for that video.
- The output folder contains an analysis JSON file.
- The JSON includes video metadata, object detections, event candidates, and review status.
- The analyzer saves at least one sampled or annotated frame when detections exist.
- OCR can be disabled without failing the whole analysis.
- The design can later connect to NAS folders without changing the JSON contract.

## Beginner Notes

- The NAS stores videos and analysis results.
- The PC runs the heavy AI work.
- AI results are candidates, not final answers.
- Human corrections become future training data.
- The first prototype should favor quick experiments, small models, and low frame sampling.
