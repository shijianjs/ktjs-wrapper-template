package io.github.shijianjs.ktjs.wrappertemplate.demo.page.overview

import io.github.shijianjs.ktjs.dom.css.dsl.invoke
import io.github.shijianjs.ktjs.dom.element.invoke
import io.github.shijianjs.ktjs.vaadin.card.CardElement
import web.html.HTMLAnchorElement
import web.html.HTMLElement

/** One card of the index, kept typed so a language switch rewrites its title and summary in place. */
internal class CardLink(val id: String, val link: HTMLAnchorElement, val card: CardElement, val summary: HTMLElement)
