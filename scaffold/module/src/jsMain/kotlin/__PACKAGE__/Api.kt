package __PACKAGE__

/**
 * Import name of the wrapped npm package.
 *
 * Keep this constant for the scaffold smoke test, or delete it together with that test once the
 * real API surface exists.
 */
const val UPSTREAM_PACKAGE: String = "__NPM_PACKAGE__"

/**
 * Named exports of the `__NPM_PACKAGE__` package.
 *
 * Declare one `external` member per upstream export and keep the KDoc derived from the upstream
 * source or README:
 *
 * ```kotlin
 * @JsModule("__NPM_PACKAGE__")
 * external object UpstreamModule {
 *     fun create(options: CreateOptions): UpstreamResult
 * }
 * ```
 *
 * Map the upstream `.d.ts` types as described in `TYPE-MAPPING.md`; never fall back to `dynamic`.
 *
 * ESM note: an `external object` with named members compiles to named imports. A top-level
 * `external val` with `@JsName` compiles to a default import, which fails for packages that only
 * have named exports. For an ESM-only Node package, import a subpath
 * (`@scope/package/lib/index.js`) instead of the bare package name.
 */
@JsModule("__NPM_PACKAGE__")
external object UpstreamModule
