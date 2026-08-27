# DeepWave – Binaural Beats & Brainwave Entrainment

DeepWave is a fully offline Android app that generates binaural beats, isochronic
tones and Solfeggio frequencies for sleep, focus, meditation and relaxation.

- 🎧 40+ presets across 12 categories (Sleep, Focus, Meditation, Energy, Creativity, Sports, …)
- 🎛️ Flexible phase editor – build your own multi-phase sessions
- 🌬️ Breathing guide (Box, 4-7-8, Calming, Energizing)
- 📊 Session journal with mood tracking and statistics
- 🔊 Background noise (pink/brown), WAV export, background playback
- 🔒 100% offline – no internet permission, no ads, no tracking, no data collection
- 🆓 Open source under GPL-3.0

## 📣 Beta testers wanted!

DeepWave is preparing its **Google Play release** and needs **12+ testers** for the
mandatory 14-day closed test. Helping out takes two minutes:

1. **Send your Google account e-mail address** — either by commenting on the
   [pinned tester issue](../../issues/1) or by mail to <deepwave-tools@proton.me>
2. You will be added to the tester list and receive an opt-in link
3. Install the app via the link and **keep it installed for 14 days** — that's it!

Testers get the paid version **for free** (via Play license testing) and are more
than welcome to report feedback and bugs as GitHub issues. Danke! 🙏

## Availability

| Store | Status |
|-------|--------|
| F-Droid | Submission under review ([fdroiddata MR !39383](https://gitlab.com/fdroid/fdroiddata/-/merge_requests/39383)) |
| Google Play | Closed testing starting soon (see above) |

The FOSS flavor (F-Droid) contains the core feature set; the paid Play flavor
additionally unlocks premium preset programs, WAV export, statistics and
unlimited custom presets.

## Privacy

DeepWave collects no data whatsoever. See the
[privacy policy](https://mandres747.github.io/DeepWave/) (also available
[auf Deutsch](https://mandres747.github.io/DeepWave/datenschutz.html)).

## Building

```bash
# FOSS flavor (F-Droid variant)
gradlew.bat assembleFossRelease   # Windows
# Play flavor
gradlew.bat assemblePremiumRelease
```

Requires JDK 17 and the Android SDK (compileSdk 36).

## License

[GPL-3.0-only](LICENSE) – your brain, your data, your code.
