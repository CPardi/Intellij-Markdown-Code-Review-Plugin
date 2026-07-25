# Releasing

## Steps

1. **Add changelog entries** under `## [Unreleased]` in `CHANGELOG.md`.
   `getChangelog --unreleased` and `patchChangelog` only work if this section
   exists.
2. **Bump the version** in `gradle.properties` (e.g. `version = 0.0.2`).
3. **Open a PR and merge to `main`.** That's it for the first half, the
   build, the draft release, and the `Changelog update` PR are produced
   automatically.
4. **Wait for `Build` to finish** on `main`. Watch the Actions tab; if it's
   red, fix and push, don't continue.
5. **Wait for `Release` to finish.** It produces a *draft* GitHub release
   (tagged `v<VERSION>`) with the plugin ZIP attached and opens a
   `Changelog update` PR against `main`.
6. Kick the tires on the plugin available in `Release`'s assets.
7. **Merge the `Changelog update` PR.** Merging it triggers the `Publish`
   workflow, which publishes the plugin to the JetBrains Marketplace,
   flips the draft release to public, and then — as the final step —
   creates the annotated `v<VERSION>` tag with the release notes as the
   tag message at the merge commit.
8. **Confirm the release.** Check that the GitHub release is no longer a
   draft and that the plugin is visible on the JetBrains Marketplace listing.

For re-runs and recovery, the `Publish` workflow is also available via
manual trigger (GitHub → **Actions** → **Publish** → **Run workflow**).

## Troubleshooting

- **`Build` failed.** Fix and push again. No draft will exist until the
  build is green.
- **`Release` didn't appear.** It only fires after a *successful* `Build`
  on `main`; re-runs of a failed build do not trigger it.
- **Empty release notes / empty `Changelog update` PR.** A
  `## [Unreleased]` section is missing from `CHANGELOG.md`.
- **Tag was not created after merging the `Changelog update` PR.** Check the
  `Publish` workflow's Actions tab. The tag step only runs when the PR was
  actually merged (not closed unmerged) and only on branches whose name
  starts with `changelog-update-`. Note that the tag is the final step, so a
  failed publish or draft-flip will leave the workflow in a state where no
  tag has been pushed — re-run `Publish` via `workflow_dispatch` with the
  version input to retry.
- **`Publish` failed: "No release found for tag v…".** The tag was created
  but the matching draft release is missing. Confirm `Release` ran
  successfully on the same `main` commit that the changelog PR merged.
- **`Publish` skipped: "Release is no longer a draft".** The release has
  already been published. This is the idempotent behaviour on a re-run or
  tag re-push — no action needed unless a re-publish was intended.
- **`Publish` failed mid-way.** The GitHub release stays a draft (the draft
  flip is gated on marketplace success). Fix the cause and re-run
  `Publish` via `workflow_dispatch`.

## Assumed Repository State

The GitHub repository must already have these secrets configured, otherwise
`Publish` cannot push to the marketplace:

- `PUBLISH_TOKEN`, JetBrains Marketplace publisher token.
- `PRIVATE_KEY`, `PRIVATE_KEY_PASSWORD`, `CERTIFICATE_CHAIN`, required for
  plugin signing.

See https://plugins.jetbrains.com/docs/intellij/plugin-signing.html for
details on generating and providing these.
