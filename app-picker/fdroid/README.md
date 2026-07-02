# Self-hosted F-Droid repo

The `app-picker Release` workflow publishes signed release APKs to a personal
F-Droid repository served from GitHub Pages:

**Repo URL:** `https://placeybordeaux.github.io/misc/fdroid/repo`

## How it works

- **`metadata/`** — the app's F-Droid listing (name, summary, description). Edit
  it here; it's version-controlled with the app.
- **`config.template.yml`** — rendered into `config.yml` in CI with secrets, used
  by `fdroid update`, then deleted before publishing (it holds passwords).
- The **gh-pages** branch holds the live site: accumulated APKs + the signed index.
  Each release carries prior versions forward, so old releases stay installable.

## Two signing keys

| Key | Signs | Where it lives |
|-----|-------|----------------|
| App key | the APK | `APP_KEYSTORE_*` secrets — **never rotate**; updates require the same key |
| Repo key | the F-Droid index | `FDROID_KEYSTORE_*` secrets |

## Adding the repo on a device

1. Install the F-Droid client.
2. Settings → Repositories → add `https://placeybordeaux.github.io/misc/fdroid/repo`.
3. Verify the fingerprint matches the repo key (printed by `fdroid update` and
   recoverable via `keytool -list -keystore <repo-keystore>`).

## Cutting a release

```sh
git tag app-picker-v1.2.3
git push origin app-picker-v1.2.3
```

`versionName` = `1.2.3`; `versionCode` = `1*1000000 + 2*1000 + 3` = `1002003`.
