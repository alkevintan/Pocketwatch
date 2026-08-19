<div align="center">

<img src="images/icon.png" width="112" alt="Pocketwatch icon">

# Pocketwatch

**A phone and tablet client for YouTube**

Forked from [yuliskov/SmartTube](https://github.com/yuliskov/SmartTube)

</div>

Upstream SmartTube is built exclusively for Android TV — its interface is Leanback, designed for a
remote control and a 10-foot viewing distance, and it explicitly does not support phones. This fork
replaces that interface with a touch-first one while reusing upstream's entire presenter and
playback layer unchanged.

> [!WARNING]
> **Early and unreleased.** Version 0.1.0. It runs, plays video and is usable day to day, but it is
> not feature-complete against upstream and there are no published builds yet — see
> [Status](#status) and [Build](#build).

## What this fork changes

Upstream already separates presenters from views: everything in the `common` module is UI-agnostic
and contains no Leanback code, and `ViewManager` maps each view interface to an activity. That
mapping is the only seam this fork touches.

| Layer | Status |
| --- | --- |
| Data, networking, playback engine | Upstream, unchanged |
| Presenters (`common`) | Upstream, plus two small hooks |
| Views | Rewritten for touch |

Because the shared layer is untouched, upstream fixes remain mergeable.

## Status

**Working**

- Browse — bottom navigation, with an overflow sheet so any number of pinned sections is reachable
- Responsive grids and shelves — column count follows screen width, so tablets get more columns
- Player — video, seek bar, all 24 of upstream's player actions, double-tap-to-seek,
  landscape fullscreen
- Comments — shown inline beneath the video actions, paging as you scroll
- Settings and context menus as bottom sheets
- Search with live suggestion chips and voice input
- Channel, channel uploads, sign-in, add-device, in-app browser
- Light and dark themes following the system setting

**Not done yet**

- No MediaSession, so no background-audio controls or notification
- No trigger to enter picture-in-picture (the manifest supports it)
- SponsorBlock segments are skipped but not shaded on the seek bar
- No storyboard thumbnails when scrubbing
- Rotate and flip appear for parity but are inert; they need a TextureView-backed surface
- Auto Frame Rate and screen dimming are TV-oriented and carried over untouched
- Tablet layout is responsive but not specifically designed
- The Leanback code is still in the tree, unreachable through `ViewManager`

Signing in is required for the Home feed — that is upstream behaviour, not a fork limitation.
Other sections work signed out.

## Build

No releases are published. Build from source:

```bash
git clone --recursive git@github.com:alkevintan/Pocketwatch.git
cd Pocketwatch
chmod +x gradlew
./gradlew :smarttubetv:assembleStfdroidDebug
```

Notes that will save you time:

- **`--recursive` matters.** `settings.gradle` reads build constants from the `SharedModules`
  submodule, so without submodules the project will not even sync. If you already cloned, run
  `git submodule update --init --recursive`.
- **JDK 17.** Gradle 7.5 and AGP 7.4.2 do not support JDK 21, which is what current Android Studio
  bundles. Set `JAVA_HOME` to a JDK 17 before building.
- **`lint` and instrumentation tests fail**, on this fork and on upstream alike — Espresso 3.2.0
  against `targetSdk 34` produces a manifest merge error. Unrelated to these changes.

APKs land in `smarttubetv/build/outputs/apk/<flavor>/<type>/`. Install the split matching your
device ABI (`arm64-v8a` for most phones).

### Flavors

| Flavor | applicationId |
| --- | --- |
| `ststable` | `io.github.alkevintan.pocketwatch` |
| `stbeta` | `io.github.alkevintan.pocketwatch.beta` |
| `stfdroid` | `io.github.alkevintan.pocketwatch.fdroid` |

The applicationId differs from upstream's, so this installs **alongside** SmartTube rather than
replacing it. The Java package namespace is deliberately left as upstream's to keep merges
tractable.

Firebase and Crashlytics are disabled: upstream's `google-services.json` is tied to their Firebase
project and cannot work under a different applicationId.

## Contributing

Issues and pull requests are welcome, but note this is a personal fork maintained in spare time.

Bugs in playback, data loading or settings behaviour most likely belong
[upstream](https://github.com/yuliskov/SmartTube/issues), since that code is shared. Report here
anything about layout, touch interaction, navigation or the mobile player UI.

## Licence and attribution

MIT, inherited from upstream. Original work is Copyright (c) 2020-present yuliskov — see
[LICENSE](LICENSE). All credit for SmartTube and everything this fork stands on belongs to
yuliskov and the SmartTube contributors.

Not affiliated with, endorsed by or supported by upstream SmartTube, Google or YouTube. MIT grants
no trademark rights; the SmartTube name is used only to describe this project's origin.
