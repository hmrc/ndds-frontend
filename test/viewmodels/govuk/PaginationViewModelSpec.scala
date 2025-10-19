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
import viewmodels.govuk.PaginationFluency.*
import play.api.test.FakeRequest
import play.api.i18n.Messages

class PaginationViewModelSpec extends SpecBase with Matchers {

  "PaginationViewModel" - {

    "must create basic pagination with default values" in new Setup {
      val pagination = PaginationViewModel()

      pagination.items mustBe Nil
      pagination.previous mustBe None
      pagination.next mustBe None
      pagination.landmarkLabel mustBe "Pagination"
      pagination.classes mustBe ""
      pagination.attributes mustBe Map.empty
    }

    "must create pagination with items" in new Setup {
      val items = Seq(
        PaginationItemViewModel("1", "/page/1"),
        PaginationItemViewModel("2", "/page/2")
      )
      val pagination = PaginationViewModel(items)

      pagination.items mustBe items
    }

    "must support fluent API for building pagination" in new Setup {
      val pagination = PaginationViewModel()
        .withItems(Seq(PaginationItemViewModel("1", "/page/1")))
        .withPrevious(PaginationLinkViewModel("/prev"))
        .withNext(PaginationLinkViewModel("/next"))
        .withLandmarkLabel("Custom Label")
        .withClasses("custom-class")
        .withAttributes(Map("data-test" -> "pagination"))

      pagination.items.size mustBe 1
      pagination.previous mustBe defined
      pagination.next mustBe defined
      pagination.landmarkLabel mustBe "Custom Label"
      pagination.classes mustBe "custom-class"
      pagination.attributes mustBe Map("data-test" -> "pagination")
    }

    "must convert to GovUK Pagination correctly" in new Setup {
      val pagination = PaginationViewModel(
        items = Seq(
          PaginationItemViewModel("1", "/page/1"),
          PaginationItemViewModel("2", "/page/2").withCurrent(true)
        ),
        previous      = Some(PaginationLinkViewModel("/prev").withText("Previous")),
        next          = Some(PaginationLinkViewModel("/next").withText("Next")),
        landmarkLabel = "Test Pagination",
        classes       = "test-class",
        attributes    = Map("data-test" -> "value")
      )

      val govukPagination = pagination.asPagination

      govukPagination.items.get.size mustBe 2
      govukPagination.previous mustBe defined
      govukPagination.next mustBe defined
      govukPagination.landmarkLabel mustBe Some("Test Pagination")
      govukPagination.classes mustBe "test-class"
      govukPagination.attributes mustBe Map("data-test" -> "value")
    }
  }

  "PaginationItemViewModel" - {

    "must create basic item with number and href" in new Setup {
      val item = PaginationItemViewModel("1", "/page/1")

      item.number mustBe "1"
      item.href mustBe "/page/1"
      item.visuallyHiddenText mustBe None
      item.current mustBe false
      item.ellipsis mustBe false
      item.attributes mustBe Map.empty
    }

    "must create ellipsis item" in new Setup {
      val item = PaginationItemViewModel.ellipsis()

      item.number mustBe ""
      item.href mustBe ""
      item.ellipsis mustBe true
    }

    "must support fluent API for building items" in new Setup {
      val item = PaginationItemViewModel("1", "/page/1")
        .withVisuallyHiddenText("Go to page 1")
        .withCurrent(true)
        .withAttributes(Map("data-test" -> "item"))

      item.visuallyHiddenText mustBe Some("Go to page 1")
      item.current mustBe true
      item.attributes mustBe Map("data-test" -> "item")
    }

    "must convert to GovUK PaginationItem correctly" in new Setup {
      val item = PaginationItemViewModel("1", "/page/1")
        .withVisuallyHiddenText("Go to page 1")
        .withCurrent(true)
        .withAttributes(Map("data-test" -> "item"))

      val govukItem = item.asPaginationItem

      govukItem.number mustBe Some("1")
      govukItem.href mustBe "/page/1"
      govukItem.visuallyHiddenText mustBe Some("Go to page 1")
      govukItem.current mustBe Some(true)
      govukItem.ellipsis mustBe None
      govukItem.attributes mustBe Map("data-test" -> "item")
    }

    "must convert ellipsis item to GovUK PaginationItem correctly" in new Setup {
      val item = PaginationItemViewModel.ellipsis()
      val govukItem = item.asPaginationItem

      govukItem.number mustBe None
      govukItem.href mustBe ""
      govukItem.ellipsis mustBe Some(true)
    }
  }

  "PaginationLinkViewModel" - {

    "must create basic link with href" in new Setup {
      val link = PaginationLinkViewModel("/page/1")

      link.href mustBe "/page/1"
      link.text mustBe None
      link.html mustBe None
      link.labelText mustBe None
      link.attributes mustBe Map.empty
    }

    "must support fluent API for building links" in new Setup {
      val link = PaginationLinkViewModel("/page/1")
        .withText("Next page")
        .withLabelText("More results")
        .withAttributes(Map("data-test" -> "link"))

      link.text mustBe Some("Next page")
      link.labelText mustBe Some("More results")
      link.attributes mustBe Map("data-test" -> "link")
    }

    "must convert to GovUK PaginationLink correctly" in new Setup {
      val link = PaginationLinkViewModel("/page/1")
        .withText("Next page")
        .withLabelText("More results")
        .withAttributes(Map("data-test" -> "link"))

      val govukLink = link.asPaginationLink

      govukLink.href mustBe "/page/1"
      govukLink.text mustBe Some("Next page")
      govukLink.labelText mustBe Some("More results")
      govukLink.attributes mustBe Map("data-test" -> "link")
    }

    "must handle HTML content in links" in new Setup {
      val link = PaginationLinkViewModel("/page/1")
        .withHtml("<span>Next</span>")

      val govukLink = link.asPaginationLink

      govukLink.href mustBe "/page/1"
      govukLink.text mustBe None
    }
  }

  trait Setup {
    val app = applicationBuilder().build()
    implicit val request: play.api.mvc.Request[?] = FakeRequest()
    implicit val messages: Messages = play.api.i18n.MessagesImpl(
      play.api.i18n.Lang.defaultLang,
      app.injector.instanceOf[play.api.i18n.MessagesApi]
    )
  }
}
