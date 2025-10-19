/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import base.SpecBase
import org.scalatest.matchers.must.Matchers
import views.html.components.Pagination
import play.api.test.FakeRequest
import play.api.i18n.Messages
import org.jsoup.Jsoup
import viewmodels.govuk.PaginationFluency._

class PaginationSpec extends SpecBase with Matchers {

  "Pagination component" - {

    "must render basic pagination with page numbers" in new Setup {
      val pagination = PaginationViewModel(
        items = Seq(
          PaginationItemViewModel("1", "/page/1"),
          PaginationItemViewModel("2", "/page/2").withCurrent(true),
          PaginationItemViewModel("3", "/page/3")
        )
      )

      val html = paginationComponent(pagination)
      val doc = Jsoup.parse(html.body)

      doc.select(".govuk-pagination").size mustBe 1
      doc.select(".govuk-pagination__list").size mustBe 1
      doc.select(".govuk-pagination__item").size mustBe 3
      doc.select(".govuk-pagination__item--current").size mustBe 1
      doc.select(".govuk-pagination__item--current a").attr("aria-current") mustBe "page"
    }

    "must render pagination with previous link" in new Setup {
      val pagination = PaginationViewModel(
        items = Seq(
          PaginationItemViewModel("1", "/page/1"),
          PaginationItemViewModel("2", "/page/2").withCurrent(true)
        ),
        previous = Some(PaginationLinkViewModel("/page/1").withText("Previous page"))
      )

      val html = paginationComponent(pagination)
      val doc = Jsoup.parse(html.body)

      doc.select(".govuk-pagination__prev").size mustBe 1
      doc.select(".govuk-pagination__prev a").attr("href") mustBe "/page/1"
      doc.select(".govuk-pagination__prev a").attr("rel") mustBe "prev"
      doc.select(".govuk-pagination__icon--prev").size mustBe 1
    }

    "must render pagination with next link" in new Setup {
      val pagination = PaginationViewModel(
        items = Seq(
          PaginationItemViewModel("1", "/page/1").withCurrent(true),
          PaginationItemViewModel("2", "/page/2")
        ),
        next = Some(PaginationLinkViewModel("/page/2").withText("Next page"))
      )

      val html = paginationComponent(pagination)
      val doc = Jsoup.parse(html.body)

      doc.select(".govuk-pagination__next").size mustBe 1
      doc.select(".govuk-pagination__next a").attr("href") mustBe "/page/2"
      doc.select(".govuk-pagination__next a").attr("rel") mustBe "next"
      doc.select(".govuk-pagination__icon--next").size mustBe 1
    }

    "must render pagination with ellipsis" in new Setup {
      val pagination = PaginationViewModel(
        items = Seq(
          PaginationItemViewModel("1", "/page/1"),
          PaginationItemViewModel.ellipsis(),
          PaginationItemViewModel("5", "/page/5").withCurrent(true),
          PaginationItemViewModel("6", "/page/6")
        )
      )

      val html = paginationComponent(pagination)
      val doc = Jsoup.parse(html.body)

      doc.select(".govuk-pagination__item").size mustBe 4
      doc.select(".govuk-pagination__item").get(1).text() mustBe "⋯"
    }

    "must render pagination with custom landmark label" in new Setup {
      val pagination = PaginationViewModel(
        items = Seq(
          PaginationItemViewModel("1", "/page/1")
        )
      ).withLandmarkLabel("Search results")

      val html = paginationComponent(pagination)
      val doc = Jsoup.parse(html.body)

      doc.select(".govuk-pagination").attr("aria-label") mustBe "Search results"
    }

    "must render pagination with custom classes" in new Setup {
      val pagination = PaginationViewModel(
        items = Seq(
          PaginationItemViewModel("1", "/page/1")
        )
      ).withClasses("custom-pagination")

      val html = paginationComponent(pagination)
      val doc = Jsoup.parse(html.body)

      doc.select(".govuk-pagination").hasClass("custom-pagination") mustBe true
    }

    "must render pagination with visually hidden text for accessibility" in new Setup {
      val pagination = PaginationViewModel(
        items = Seq(
          PaginationItemViewModel("1", "/page/1").withVisuallyHiddenText("Go to page 1"),
          PaginationItemViewModel("2", "/page/2").withCurrent(true).withVisuallyHiddenText("Current page, page 2")
        )
      )

      val html = paginationComponent(pagination)
      val doc = Jsoup.parse(html.body)

      doc.select(".govuk-pagination__item a").first().attr("aria-label") mustBe "Go to page 1"
      doc.select(".govuk-pagination__item--current a").attr("aria-label") mustBe "Current page, page 2"
    }

    "must render pagination with previous and next links having custom text" in new Setup {
      val pagination = PaginationViewModel(
        items = Seq(
          PaginationItemViewModel("2", "/page/2").withCurrent(true)
        ),
        previous = Some(PaginationLinkViewModel("/page/1").withText("Go back")),
        next = Some(PaginationLinkViewModel("/page/3").withText("Continue"))
      )

      val html = paginationComponent(pagination)
      val doc = Jsoup.parse(html.body)

      doc.select(".govuk-pagination__prev .govuk-pagination__link-title").text() mustBe "Go back"
      doc.select(".govuk-pagination__next .govuk-pagination__link-title").text() mustBe "Continue"
    }

    "must render pagination with label text for previous and next links" in new Setup {
      val pagination = PaginationViewModel(
        items = Seq(
          PaginationItemViewModel("2", "/page/2").withCurrent(true)
        ),
        previous = Some(PaginationLinkViewModel("/page/1").withText("Previous").withLabelText("Search results")),
        next = Some(PaginationLinkViewModel("/page/3").withText("Next").withLabelText("More results"))
      )

      val html = paginationComponent(pagination)
      val doc = Jsoup.parse(html.body)

      doc.select(".govuk-pagination__prev .govuk-pagination__link-label").text() mustBe ""
      doc.select(".govuk-pagination__next .govuk-pagination__link-label").text() mustBe ""
    }

    "must render empty pagination when no items provided" in new Setup {
      val pagination = PaginationViewModel()

      val html = paginationComponent(pagination)
      val doc = Jsoup.parse(html.body)

      doc.select(".govuk-pagination").size mustBe 1
      doc.select(".govuk-pagination__list").size mustBe 0
      doc.select(".govuk-pagination__prev").size mustBe 0
      doc.select(".govuk-pagination__next").size mustBe 0
    }
  }

  trait Setup {
    val app = applicationBuilder().build()
    val paginationComponent = app.injector.instanceOf[Pagination]
    implicit val request: play.api.mvc.Request[?] = FakeRequest()
    implicit val messages: Messages = play.api.i18n.MessagesImpl(
      play.api.i18n.Lang.defaultLang,
      app.injector.instanceOf[play.api.i18n.MessagesApi]
    )
  }
}
