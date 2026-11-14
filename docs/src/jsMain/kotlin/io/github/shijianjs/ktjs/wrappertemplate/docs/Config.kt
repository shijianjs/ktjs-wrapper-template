package io.github.shijianjs.ktjs.wrappertemplate.docs

import io.github.shijianjs.ktjs.docusaurus.config.Config
import io.github.shijianjs.ktjs.docusaurus.config.Directions
import io.github.shijianjs.ktjs.docusaurus.config.I18nConfig
import io.github.shijianjs.ktjs.docusaurus.config.I18nLocaleConfig
import io.github.shijianjs.ktjs.docusaurus.config.MarkdownConfig
import io.github.shijianjs.ktjs.docusaurus.config.PluginConfig
import io.github.shijianjs.ktjs.docusaurus.config.PluginOptions
import io.github.shijianjs.ktjs.docusaurus.config.PresetConfig
import io.github.shijianjs.ktjs.docusaurus.config.ReportingSeverities
import io.github.shijianjs.ktjs.docusaurus.config.StringOrDouble
import io.github.shijianjs.ktjs.docusaurus.config.StringOrFalse
import io.github.shijianjs.ktjs.docusaurus.plugins.contentdocs.DocsOptions
import io.github.shijianjs.ktjs.docusaurus.plugins.themeclassic.StringOrStringArray
import io.github.shijianjs.ktjs.docusaurus.plugins.themeclassic.ThemeClassicOptions
import io.github.shijianjs.ktjs.docusaurus.presets.BlogOptionsOrFalse
import io.github.shijianjs.ktjs.docusaurus.presets.DocsOptionsOrFalse
import io.github.shijianjs.ktjs.docusaurus.presets.classicPreset
import io.github.shijianjs.ktjs.docusaurus.sidebars.SidebarItem
import io.github.shijianjs.ktjs.docusaurus.sidebars.SidebarItemTypes
import io.github.shijianjs.ktjs.docusaurus.sidebars.SidebarsConfig
import io.github.shijianjs.ktjs.docusaurus.theme.ColorModes
import io.github.shijianjs.ktjs.docusaurus.theme.Footer
import io.github.shijianjs.ktjs.docusaurus.theme.FooterStyles
import io.github.shijianjs.ktjs.docusaurus.theme.MermaidTheme
import io.github.shijianjs.ktjs.docusaurus.theme.MermaidThemeByColorMode
import io.github.shijianjs.ktjs.docusaurus.theme.MermaidThemeConfig
import io.github.shijianjs.ktjs.docusaurus.theme.NavbarItem
import io.github.shijianjs.ktjs.docusaurus.theme.NavbarItemPositions
import io.github.shijianjs.ktjs.docusaurus.theme.NavbarItemTypes
import io.github.shijianjs.ktjs.docusaurus.theme.NavbarLogo
import io.github.shijianjs.ktjs.docusaurus.theme.PrismThemes
import io.github.shijianjs.ktjs.docusaurus.theme.SimpleFooter
import io.github.shijianjs.ktjs.docusaurus.theme.ThemeConfig
import js.objects.Record
import js.objects.unsafeJso
import kotlin.js.JsAny
import kotlin.js.JsExport
import kotlin.js.JsName
import kotlin.js.unsafeCast

private const val REPOSITORY = "https://github.com/shijianjs/ktjs-wrapper-template"

private const val SITE_URL = "https://shijianjs.github.io"

/**
 * Where the site is served from: the GitHub Pages root. `pagesSite` puts the documentation here, the
 * demo under `/demo/` and the API reference under `/api/`.
 */
private const val SITE_BASE_URL = "/ktjs-wrapper-template/"

/**
 * The two artifacts that sit next to the documentation on the same Pages deployment.
 *
 * Full URLs rather than paths: neither is a route of this Docusaurus build, and Docusaurus reports an
 * absolute internal path (`/ktjs-wrapper-template/demo/`) as a broken link. A full URL is treated as external, and
 * it also survives the locale prefixes, where a relative link would resolve to nothing.
 */
private const val DEMO_URL = "$SITE_URL$SITE_BASE_URL" + "demo/"
private const val API_URL = "$SITE_URL$SITE_BASE_URL" + "api/"

/**
 * The site configuration, imported by the generated `docusaurus.config.mjs`.
 *
 * Everything a `docusaurus.config.ts` can express is a Kotlin value here; the Gradle plugin turns
 * the exported function into the default export Docusaurus loads.
 */
@JsExport
fun docusaurusConfig(): Config = unsafeJso {
    title = "ktjs-wrapper-template"
    tagline = "A Kotlin/JS wrapper project you can copy"
    url = SITE_URL
    baseUrl = SITE_BASE_URL
    favicon = "img/ktjs-wrapper-template-logo.png"
    trailingSlash = true
    // A broken internal link fails the build instead of shipping a 404 to the Pages site.
    onBrokenLinks = ReportingSeverities.`throw`

    // ```mermaid fences render as diagrams. Two halves, and both are needed: this flag is what
    // compiles the fence into a `<Mermaid>` element, and the theme registered below is what provides
    // that element in the browser.
    markdown = unsafeJso<MarkdownConfig> {
        mermaid = true
    }

    // The kit's element bundle is loaded from here after every route change; see `ClientModule.kt`.
    //
    // `d3-transition` is imported for its side effect, not its exports: it is what puts `transition()`
    // on d3 selections, and mermaid calls it for the flowchart node tooltips. Mermaid only reaches it
    // through `d3`'s re-exports, and `d3` declares `sideEffects: false`, so webpack drops the module
    // and hovering any flowchart node throws "tooltipElem.transition is not a function". Importing the
    // package itself keeps it (its own package.json says `sideEffects: true`). The path is relative to
    // the site directory, which the Gradle plugin fixes at `<root>/build/js/docusaurus/<module>`.
    clientModules = arrayOf(
        "./client-module.mjs",
        "../../node_modules/d3-transition",
    )

    // English is the source language; every page under `src/jsMain/resources` has a translation in
    // `i18n/zh-Hans/`, in the directory owned by the plugin that serves it.
    i18n = unsafeJso<I18nConfig> {
        defaultLocale = "en"
        locales = arrayOf("en", "zh-Hans")
        localeConfigs = unsafeJso<LocaleConfigs> {
            en = unsafeJso<I18nLocaleConfig> {
                label = "English"
                direction = Directions.ltr
                htmlLang = "en"
            }
            zhHans = unsafeJso<I18nLocaleConfig> {
                label = "简体中文"
                direction = Directions.ltr
                htmlLang = "zh-Hans"
            }
        }.unsafeCast<Record<JsAny, I18nLocaleConfig>>()
    }

    // Two themes: `@docusaurus/theme-mermaid` provides `@theme/Mermaid`, the element a
    // ```mermaid fence compiles into. Its palette and look are site configuration
    // (`themeConfig.mermaid` below).
    //
    // The empty options object is not decoration: the wrapper this site builds against
    // (`3.10.2-kt.2`) turns `PluginConfig(name)` into a one-element array, and Docusaurus' schema
    // accepts only a bare string or an exactly two-element `[name, options]` tuple — the
    // one-argument form fails at config load with "Bad Docusaurus theme value themes[0]". Once a
    // wrapper carrying the fix is released the bare name is enough again.
    themes = arrayOf<PluginConfig?>(
        PluginConfig("@docusaurus/theme-mermaid", unsafeJso<PluginOptions> { }),
    )

    presets = arrayOf<PresetConfig?>(
        classicPreset(
            unsafeJso {
                docs = DocsOptionsOrFalse(
                    unsafeJso<DocsOptions> {
                        // Generated by the Gradle plugin from `sidebars()` below.
                        sidebarPath = StringOrFalse("sidebars.mjs")
                        // The docs are the whole site, so they sit at the base URL instead of
                        // under an extra `/docs/` segment.
                        routeBasePath = "/"
                        breadcrumbs = true
                    },
                )
                // No blog: a wrapper project documents an API, and an empty plugin still builds a
                // blog route.
                blog = BlogOptionsOrFalse(false)
                theme = unsafeJso<ThemeClassicOptions> {
                    customCss = StringOrStringArray("src/css/custom.css")
                }
            },
        ),
    )

    themeConfig = unsafeJso<ThemeConfig> {
        navbar = unsafeJso {
            title = "ktjs-wrapper-template"
            // The same file the favicon uses, so the navbar logo costs no extra download; `alt` is
            // left out because the title next to it already names the site.
            logo = unsafeJso<NavbarLogo> {
                src = "img/ktjs-wrapper-template-logo.png"
                width = StringOrDouble(32.0)
                height = StringOrDouble(32.0)
            }
            hideOnScroll = true
            items = arrayOf(
                unsafeJso<NavbarItem> {
                    type = NavbarItemTypes.docSidebar
                    sidebarId = "guide"
                    label = "Docs"
                    position = NavbarItemPositions.left
                },
                // The documentation is the entry point, so the demo and the API reference are jumps
                // out of it rather than the other way around.
                unsafeJso<NavbarItem> {
                    label = "Demo"
                    href = DEMO_URL
                    position = NavbarItemPositions.left
                },
                unsafeJso<NavbarItem> {
                    label = "API"
                    href = API_URL
                    position = NavbarItemPositions.left
                },
                unsafeJso<NavbarItem> {
                    type = NavbarItemTypes.localeDropdown
                    position = NavbarItemPositions.right
                },
                // A plain link has no `type`: Docusaurus rejects an explicit "default" or "link".
                unsafeJso<NavbarItem> {
                    label = "GitHub"
                    href = REPOSITORY
                    position = NavbarItemPositions.right
                },
            )
        }
        colorMode = unsafeJso {
            defaultMode = ColorModes.light
            respectPrefersColorScheme = true
        }
        docs = unsafeJso {
            sidebar = unsafeJso { hideable = true }
        }
        prism = unsafeJso {
            theme = PrismThemes.github
            darkTheme = PrismThemes.dracula
            additionalLanguages = arrayOf("bash", "kotlin", "json")
        }
        // Both names are mermaid's own and both come from its `redux` family: a white card with a
        // dark outline in light mode, its dark counterpart in dark mode. `MermaidThemes` lists the
        // five older names (default / neutral / dark / forest / base), so these two enter through the
        // factory that wraps a value mermaid defines.
        //
        // `options` is spread into `mermaid.initialize` as `{startOnLoad: false, ...options, theme}`,
        // the theme above winning over anything set here. Mermaid *ignores* a value it does not
        // recognise rather than reporting it, so the palette and `look: 'neo'` are verified in a
        // browser, never by a build.
        mermaid = unsafeJso<MermaidThemeConfig> {
            theme = unsafeJso<MermaidThemeByColorMode> {
                light = MermaidTheme("redux-color")
                dark = MermaidTheme("redux-dark-color")
            }
            options = unsafeJso<MermaidOptions> {
                look = "neo"
            }
        }
        footer = Footer(
            unsafeJso<SimpleFooter> {
                style = FooterStyles.dark
                links = arrayOf(
                    unsafeJso {
                        label = "Demo"
                        href = DEMO_URL
                    },
                    unsafeJso {
                        label = "API"
                        href = API_URL
                    },
                    unsafeJso {
                        label = "GitHub"
                        href = REPOSITORY
                    },
                )
                copyright = "Built with Docusaurus from a Kotlin/JS project."
            },
        )
    }
}

/**
 * The sidebars, imported by the generated `sidebars.mjs`.
 *
 * One id-matched sidebar; the navbar links to it by id. Doc ids are file paths without the
 * extension (`guide/getting-started` is `docs/guide/getting-started.md`).
 */
@JsExport
fun sidebars(): SidebarsConfig = unsafeJso<Sidebars> {
    guide = arrayOf<SidebarItem>(
        unsafeJso {
            type = SidebarItemTypes.doc
            id = "intro"
        },
        unsafeJso {
            type = SidebarItemTypes.doc
            id = "guide/getting-started"
        },
        unsafeJso {
            type = SidebarItemTypes.doc
            id = "guide/deriving-a-project"
        },
    )
}.unsafeCast<SidebarsConfig>()

/** The locale record the site declares; keys must match the ids in `Config.i18n.locales`. */
private external interface LocaleConfigs {
    var en: I18nLocaleConfig?

    @JsName("zh-Hans")
    var zhHans: I18nLocaleConfig?
}

/**
 * The one key this site sets in mermaid's `Config`; upstream type is the whole `mermaidAPI.Config`,
 * which the wrapper leaves opaque. Declared rather than built from a raw object literal so the key
 * name and the value are checked here — add a property per key this site needs.
 */
private external interface MermaidOptions {
    var look: String
}

/** The sidebar record: one property per sidebar id. */
private external interface Sidebars {
    var guide: Array<SidebarItem>?
}
