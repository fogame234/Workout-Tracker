# Workout Tracker

A local-first Android workout tracker built for a structured 4-day training program. All data stays on your device — no accounts, no cloud, no ads.

Built with Kotlin, Jetpack Compose, Room, and Hilt. Targets Android 15+.

## Features

### Workout Logging
- Pre-loaded 4-day workout plan (upper body, lower body, conditioning splits)
- Log weight, sets, and reps for strength exercises
- Log duration and difficulty for timed exercises (plank, farmer carries, conditioning circuits)
- Log distance and duration for cardio (running, treadmill walking, intervals)
- Sets selector (1-5 dropdown) and required field validation
- Same-day override — logging the same exercise twice on one day replaces the earlier entry

### Walking
- Standalone walking tracker separate from workout days
- Dual-unit distance entry — type in miles and kilometers auto-fills, or vice versa
- Duration tracking with pace calculation

### Overview Dashboard
- At-a-glance stats for key lifts (bench press, squat, deadlift, pull-ups, push-ups)
- Timed exercise and cardio stats with best times and distances
- Walking stats with best distance and pace
- Time period filters: Today, Week, Month, 3 Months
- Percentage change with trend indicators

### Progress Charts
- Per-exercise line charts showing improvement over time
- Metric tabs: weight, total volume, reps, duration, distance, pace
- Overall change summary with percentage delta
- Single data point renders as a starting point

### Data Backup
- Export all data to a JSON file via the system file picker
- Import from a backup file to restore data
- No permissions required beyond storage access

## License

See [LICENSE](LICENSE) for details.
