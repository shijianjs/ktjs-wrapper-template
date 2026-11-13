package io.github.shijianjs.ktjs.wrappertemplate.docs

import kotlin.js.JsExport

/**
 * The site's client module, re-exported by the generated `client-module.mjs`.
 *
 * Docusaurus evaluates this module both in the browser and during the Node prerender pass, so the DOM
 * access is guarded.
 *
 * The bundle that defines the landing page's `<ktjs-…>` elements ships as a resource of the kit's klib
 * (the kit is UI, not an API anyone compiles against) and is copied to `static/ktjs-client/` by the
 * Gradle plugin. It is inserted **after hydration**: `onRouteDidUpdate` runs once React has rendered
 * the tags, so the elements upgrade on the markup React already produced instead of racing the
 * hydration pass.
 */
private val isBrowser: Boolean = js("typeof window !== 'undefined'")

private val loadBrowserBundle: (String) -> Unit = js(
    """
    function (url) {
      if (document.querySelector('script[data-ktjs-client]')) {
        return;
      }
      var script = document.createElement('script');
      script.type = 'module';
      script.src = url;
      script.setAttribute('data-ktjs-client', '');
      document.head.appendChild(script);
    }
    """,
)

/**
 * Called by Docusaurus after every route change.
 *
 * [siteBaseUrl] is the base URL Docusaurus resolved for this locale — `/ktjs-wrapper-template/` for
 * English and `…/zh-Hans/` for Chinese, in the built site *and* in the dev server, which serves a
 * single locale under its own prefix. The bundle is served under that prefix, so the URL cannot be a
 * compile-time constant.
 */
@JsExport
fun onRouteDidUpdate(siteBaseUrl: String) {
    if (!isBrowser) return
    loadBrowserBundle(siteBaseUrl + "ktjs-client/index.mjs")
}
