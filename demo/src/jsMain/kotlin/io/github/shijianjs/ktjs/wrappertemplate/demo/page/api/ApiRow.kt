package io.github.shijianjs.ktjs.wrappertemplate.demo.page.api

import io.github.shijianjs.ktjs.dom.css.dsl.invoke
import io.github.shijianjs.ktjs.dom.element.invoke
import web.html.HTMLTableCellElement

/** One row, kept so a language switch rewrites its first cell instead of rebuilding the table. */
internal class ApiRow(val labelCell: HTMLTableCellElement, val valueCell: HTMLTableCellElement)
