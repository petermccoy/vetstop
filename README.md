# VetStop

An Android app for people who distribute pet-related brochures (e.g. for a
rescue or veterinary outreach program). It maintains a self-updating database
of **veterinarians, pet stores, and animal shelters** inside areas you draw on
a map, lets you **log brochure-drop visits** at each location, and helps you
**plan trips** that string together stops along a driving route — finishing
with a hand-off to Google Maps for turn-by-turn navigation.

## Features

- **Area tab** — draw one or more freeform polygon search areas directly on a
  Google Map (tap to place boundary points, save with a name). Tap an existing
  area to delete it.
- **Weekly sync** — a background job (WorkManager) runs about once a week and
  searches every saved area for vets, pet stores, and animal shelters via the
  Google Places API. New places are added to the local database; places that
  disappear from results are marked inactive (your visit history is kept).
  You can also trigger a sync manually from Settings.
- **Locations tab** — browse the database as a list or a map. Filter by
  category (vet / pet store / shelter) and by date of last visit
  (never, 1+ week, 1+ month, 3+ months). Tap a location for details, visit
  history, one-tap navigation, and visit logging.
- **Log a visit** — records the location, your name, date/time, how many
  brochures you found remaining, how many you left, and free-form notes.
- **Trip tab** — enter a start (or use your current GPS position) and a
  destination. The app computes the driving route, then shows every saved
  location within an adjustable estimated detour time of that route
  (filterable by last-visit date). Detour time is approximated from off-route
  distance at an assumed 30 mph local speed, so no extra API calls are made
  per candidate. Once a route is planned the entry form folds into a compact
  summary bar (tap the pencil to edit the trip) so the stop list gets the
  screen space. Check off the stops you want — they appear green on the map —
  and press **Navigate**: Google Maps opens with those locations already added
  as ordered stops.
- **Visits tab** — reverse-chronological list of every logged visit, with
  in-place editing (reopens the visit form pre-filled) and dismissal.

> **Note on Google Maps integration:** Google doesn't let third-party apps
> read or modify a route that is *already running* inside Google Maps, and
> there is no supported way to embed an add-on inside the Google Maps app.
> The closest supported integration — used here — is launching Google Maps
> with the full multi-stop route pre-loaded via the official Maps URL scheme.

## One-time setup (Google Cloud)

The app uses three Google Maps Platform products: **Maps SDK for Android**
(the in-app maps), **Places API (New)** (the weekly place search), and
**Directions API** (trip routes). All three run under one API key.

1. Create a project at <https://console.cloud.google.com/>.
2. Enable billing for the project (required even inside the free tier;
   Google Maps Platform includes a recurring monthly free credit that
   comfortably covers personal weekly-sync usage).
3. Enable these APIs (**APIs & Services → Library**):
   - *Maps SDK for Android*
   - *Places API (New)*
   - *Directions API*
4. Create an API key (**APIs & Services → Credentials → Create credentials**).
5. Recommended: restrict the key to those three APIs. If you add an Android
   application restriction (package `com.vetstop.app` + your signing SHA-1),
   note that it only applies to the Maps SDK; the Places/Directions REST calls
   are made from the app with the same key, so keep the API restriction as the
   primary control.

## Building the app

1. Install [Android Studio](https://developer.android.com/studio) (Koala or
   newer) and open this project folder.
2. Create a file named `local.properties` in the project root (Android Studio
   creates it automatically with the SDK path) and add your key:

   ```properties
   MAPS_API_KEY=AIza...your-key...
   ```

   `local.properties` is git-ignored, so the key never lands in source
   control. The [Secrets Gradle plugin](https://github.com/google/secrets-gradle-plugin)
   injects it into the manifest (Maps SDK) and `BuildConfig` (REST calls).
3. Let Gradle sync, then run the `app` configuration on a device or emulator
   with Google Play services (API 26+).

Command-line build (requires the Android SDK and a `local.properties` with
both `sdk.dir` and `MAPS_API_KEY`):

```bash
./gradlew :app:assembleDebug
```

## Using the app

1. **Grant permissions** on first launch (location is used for the map's
   blue dot and the "use current location" trip origin; notifications let
   WorkManager surface long-running sync status on some devices).
2. **Area tab** → *Draw area* → tap at least three points around the region
   you care about → *Save area* and give it a name. Saving immediately kicks
   off the first place search; the weekly refresh is scheduled automatically.
3. **Locations tab** → watch it fill in, filter, and tap through to log your
   first visit.
4. **Trip tab** → set start/destination → *Plan route* → widen or narrow the
   corridor slider → check the stops you want → *Navigate*.

## Architecture

| Layer | Tech |
| --- | --- |
| UI | Jetpack Compose + Material 3, Navigation Compose, Maps Compose |
| DI | Hilt |
| Storage | Room (`search_areas`, `locations`, `visits`), DataStore (settings) |
| Background | WorkManager periodic worker (7-day interval, network-constrained) |
| Remote | Retrofit + Gson → Places API (New) text search, Directions API |

Sync flow: for each saved polygon, the app computes its bounding box, runs a
Places text search per category restricted to that box (paginated, up to 60
results each), keeps only results inside the actual polygon
(point-in-polygon), then reconciles against the database — inserting new
places and marking vanished ones inactive.

Trip flow: Directions API returns the route polyline; every active location's
distance to the polyline is computed (local equirectangular projection), and
those within the corridor are listed in driving order. Selected stops are
passed to Google Maps as URL waypoints (Google Maps supports at most 9
intermediate stops per request).
