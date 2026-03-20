# Calamari

Calamari is a voice-driven Android companion that runs an always-on floating “bubble” overlay. The bubble listens for the user’s hot word and intents, guides the user through creating calendar events, and provides a Home screen UI for viewing those created events.

## Features

- Floating overlay bubble for voice interaction.
- Permission-gated onboarding (calendar, microphone, and overlay permissions).
- Home screen with a calendar button.
- Bottom sheet listing events created by Calamari.
- Event deep-link behavior:
  - Events created **within the last 24 hours** open the user’s default calendar view (no deep link).
  - Events created **24+ hours ago** attempt to open the specific event by `eventId`.
  - Events flagged as **Deleted** (missing from the system calendar) show a deleted indicator and are non-interactive.

## Screens

- **Overlay bubble**: captures voice input and guides the user through event creation.
- **Home events bottom sheet**: lists cached events with title/date/time (and an “all-day” fallback).
- **Event prompt**: appears when an event intent is recognized, then transitions through title capture and submission.

## Screenshots

The flow below is organized to mirror the typical user journey through the app.

1) Idle / wake-word ready:

![Listen for Calamari](docs/screenshots/listenforcalamari.png)

2) Listening for event details:

![Listen for event](docs/screenshots/listenforevent.png)

3) Parsed date/intent confirmation (prompt context before title capture):

![Confirm date heard](docs/screenshots/confirmdateheard.png)

4) Listening for event title:

![Listen for event name](docs/screenshots/listenforeventname.png)

5) Ready to submit:

![Submit event](docs/screenshots/submitevent.png)

6) Submission while overlay is above other apps:

![Submit event over OS](docs/screenshots/submiteventoverOS.png)

## How it works (high level)

1. The overlay service runs and listens for voice intents.
2. Calendar event timing is computed and an event prompt is shown for title capture.
3. When the user submits, events are inserted into the system calendar provider.
4. The repository persists a bounded history of Calamari-created events in a local Room database, which powers the Home bottom sheet.

## Notes

Persisted event history is pruned to the most recent `250` Calamari-created rows (drop-oldest).

A background verifier periodically checks stored `eventId`s against `CalendarContract.Events` and flags events that have been deleted from the system calendar. Flagged events show as **Deleted** in the Home bottom sheet and tapping them is disabled.

