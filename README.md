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

## Screens

- **Overlay bubble**: captures voice input and guides the user through event creation.
- **Home events bottom sheet**: lists cached events with title/date/time (and an “all-day” fallback).
- **Event prompt**: appears when an event intent is recognized, then transitions through title capture and submission.

## Screenshots

There are currently no screenshot assets committed to this repository. Add images under `docs/screenshots/` and update the links below.

- `docs/screenshots/home_events_bottom_sheet.png` (Home screen calendar button + bottom sheet)
- `docs/screenshots/overlay_bubble.png` (floating bubble overlay)
- `docs/screenshots/event_prompt.png` (event prompt UI when Calamari parses an intent)

## How it works (high level)

1. The overlay service runs and listens for voice intents.
2. Calendar event timing is computed and an event prompt is shown for title capture.
3. When the user submits, events are inserted into the system calendar provider.
4. The repository keeps an in-memory cache of events created by Calamari for the Home sheet.

## Development

Build/test:

```bash
./gradlew :app:testDebugUnitTest
```

## Notes

The event cache (`CalendarRepository`) is process-lifetime only. If the app process is killed, callers will need to rehydrate the list (not implemented yet).

