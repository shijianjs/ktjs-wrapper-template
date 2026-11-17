package io.github.shijianjs.ktjs.wrappertemplate.demo

import io.github.shijianjs.ktjs.wrappertemplate.demo.page.demoPageTag
import io.github.shijianjs.ktjs.wrappertemplate.demo.page.header.headerBarTag
import io.github.shijianjs.ktjs.wrappertemplate.demo.page.section.sectionShellTag
import io.github.shijianjs.ktjs.wrappertemplate.demo.page.section.sections.apiSectionTag
import io.github.shijianjs.ktjs.wrappertemplate.demo.page.section.sections.buttonDemoSectionTag
import io.github.shijianjs.ktjs.wrappertemplate.demo.page.section.sections.conventionsSectionTag

/**
 * Entry point: define the page's custom elements, innermost first.
 *
 * `<ktjs-demo-page>` is already in the markup, so defining its tag upgrades it immediately and its
 * constructor creates the children — the section tags must be defined first, otherwise those children
 * would be unknown elements that upgrade later, in an order nobody controls. `<ktjs-section>` comes
 * before the sections for the same reason one level down: each section's constructor creates its shell.
 *
 * Nothing is appended to `document.body`: the page element in the markup is the whole page.
 */
public fun main() {
    sectionShellTag()
    headerBarTag()
    buttonDemoSectionTag()
    apiSectionTag()
    conventionsSectionTag()
    demoPageTag()
}
