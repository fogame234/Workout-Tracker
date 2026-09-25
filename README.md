# Workout Tracker

A local-first Android workout tracker I built for a structured 4-day training program to help with my military PT requirements. All data stays on your device. No accounts, no cloud, no ads.

Requires Android 15+

## Recent updates

**September 2026**

- Added an Exercises tab with a reference guide for each exercise, covering what it is, how to perform it, form cues, and the muscles it works.
- The Overview now opens with a summary of your most recent session, so you can see what you logged without opening a workout day. On a rest day it shows the last session you completed.
- Overview time filters are now Week, Month, and 3 Months, and default to Week. The single day filter was removed because a one day window has nothing to compare against.
- Importing a backup now shows what the file contains and asks for confirmation first. A file that is not a valid backup is rejected without touching existing data.
- Android's automatic cloud backup is switched off, so the workout database is never copied off the device.
- Fixed number entry in regions that use a comma as the decimal separator, which could block a save or silently drop a distance.
- Tidied up screen transitions, the top bar, and chart colors in dark mode.

## What it does

**Workouts.** A pre-loaded 4-day plan covering upper body, lower body, and conditioning. Each exercise is logged the way it is trained: weight, sets, and reps for lifts; duration and difficulty for holds and carries; distance and time for cardio. Logging the same exercise twice in a day replaces the earlier entry.

**Walking.** A standalone tracker that sits outside the workout days. Distance can be entered in miles or kilometers, and pace is calculated for you.

**Exercises.** A reference guide for the exercises in the plan, so you can check form without leaving the app.

**Overview.** Your most recent session at a glance, plus best lifts, times, and distances with the change over the past week, month, or three months.

**Progress.** Per-exercise charts for weight, total volume, reps, duration, distance, and pace.

**Backup.** Export everything to a JSON file through the system file picker, and import it again to restore. No permissions required.

## License

See [LICENSE](LICENSE) for details.
