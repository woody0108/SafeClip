# AI Workspace Direction

SafeClip keeps Android, NAS upload, company review, and product documents in this repository. Heavy AI prototype work lives outside this repo at:

```text
C:\SafeClipAI
```

## Why AI Work Is Separate

- AI dependencies such as PyTorch, YOLO, OpenCV, OCR libraries, models, sample videos, frames, and plate crops can become large.
- The SafeClip repository should stay focused on app, server, review workflow, and product direction.
- The AI workspace can be rebuilt or adjusted without touching Android or NAS web code.

## Current AI Direction

The first AI prototype should run on the local Windows PC with i7-7700 and GTX 1060.

The first target is:

```text
one blackbox video
-> frame extraction
-> vehicle/person/traffic-light detection
-> plate candidate crop
-> analysis JSON
```

The AI output is a review aid, not a final legal judgment. A company reviewer must confirm or correct plate text, event time, violation type, and reportability.

## Expected Connection To SafeClip

SafeClip should eventually consume AI results from NAS-side files or records:

```text
NAS uploaded video
-> C:\SafeClipAI worker analyzes it
-> analysis JSON is written back to NAS
-> company web reads the JSON
-> reviewer corrections are stored as labels
```

The detailed AI analyzer design has moved to:

```text
C:\SafeClipAI\2026-08-04-nas-first-ai-analyzer-design.md
```

## Repository Rule

Do not commit model weights, extracted frames, plate crops, sample videos, or AI virtual environments to the SafeClip repository.
