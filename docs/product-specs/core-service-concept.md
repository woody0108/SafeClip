# Core Service Concept

## Summary

SafeClip is an Android-first service that helps drivers import blackbox dashcam videos from a USB-C microSD reader, preview the relevant event videos, and submit them for company review.

The company reviews submitted videos, organizes evidence details, prepares a report package, and records structured traffic-risk data. Early operation should keep final public-agency submission with the user until legal review or a qualified partner confirms a safer agency model.

## Service Promise

Make blackbox traffic-risk videos easy to find, preview, submit, and turn into usable report material without requiring the driver to record continuously with a phone.

## MVP Direction

MVP 1 is the submission MVP:

1. User connects a blackbox microSD card through a USB-C reader.
2. User selects the SD card or blackbox video folder through Android's folder picker.
3. The app finds recent candidate videos.
4. User previews and selects the incident video.
5. User enters basic incident information.
6. User separately agrees to report review, video storage, and traffic-risk data use.
7. The app uploads the video and metadata.
8. User checks submission status in the app.

## Core Value

- Reduces the friction of finding blackbox videos.
- Reduces the friction of preparing report material.
- Creates a structured review workflow for company operation.
- Builds labeled data for future AI assistance.
- Avoids early phone camera, heat, battery, navigation conflict, and background recording risk.

## Initial Feature Areas

- Android folder selection for SD card or USB reader storage.
- Blackbox video candidate search and sorting.
- Video preview.
- Incident metadata entry.
- Consent collection.
- Upload with progress and retry.
- Submission history and status.
- Backend submission record storage.
- Admin review workflow in a later or parallel web system.

## Out Of Scope For MVP 1

- Phone-mounted continuous recording.
- Background camera recording.
- iOS app.
- Fully automatic violation judgment.
- Fully automatic legal report submission.
- AI number-plate recognition in production.
- Paid subscription, ads, and billing integration.
- Full blackbox brand database.

## Open Questions

- Which Firebase project or backend account will be used first?
- Should login be phone number, email, Google login, or anonymous device account for MVP?
- What exact incident fields are required for first submission?
- How long should uploaded raw videos be retained?
- Which blackbox file formats and brands should be tested first?
