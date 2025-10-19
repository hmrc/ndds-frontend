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

package viewmodels.govuk

import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.pagination.{Pagination, PaginationItem, PaginationLink}
import utils.Utils.emptyString

object PaginationFluency {

  object PaginationViewModel {

    def apply(): PaginationViewModel =
      PaginationViewModel(
        items         = Nil,
        previous      = None,
        next          = None,
        landmarkLabel = "site.pagination.landmark",
        classes       = emptyString,
        attributes    = Map.empty
      )

    def apply(items: Seq[PaginationItemViewModel]): PaginationViewModel =
      PaginationViewModel(
        items         = items,
        previous      = None,
        next          = None,
        landmarkLabel = "site.pagination.landmark",
        classes       = emptyString,
        attributes    = Map.empty
      )
  }

  case class PaginationViewModel(
    items: Seq[PaginationItemViewModel] = Nil,
    previous: Option[PaginationLinkViewModel] = None,
    next: Option[PaginationLinkViewModel] = None,
    landmarkLabel: String = "site.pagination.landmark",
    classes: String = emptyString,
    attributes: Map[String, String] = Map.empty
  ) {

    def withItems(items: Seq[PaginationItemViewModel]): PaginationViewModel =
      copy(items = items)

    def withPrevious(previous: PaginationLinkViewModel): PaginationViewModel =
      copy(previous = Some(previous))

    def withNext(next: PaginationLinkViewModel): PaginationViewModel =
      copy(next = Some(next))

    def withLandmarkLabel(landmarkLabel: String): PaginationViewModel =
      copy(landmarkLabel = landmarkLabel)

    def withClasses(classes: String): PaginationViewModel =
      copy(classes = classes)

    def withAttributes(attributes: Map[String, String]): PaginationViewModel =
      copy(attributes = attributes)

    def asPagination(implicit messages: Messages): Pagination = {
      Pagination(
        items         = Some(items.map(_.asPaginationItem)),
        previous      = previous.map(_.asPaginationLink),
        next          = next.map(_.asPaginationLink),
        landmarkLabel = Some(landmarkLabel),
        classes       = classes,
        attributes    = attributes
      )
    }
  }

  object PaginationItemViewModel {

    def apply(number: String, href: String): PaginationItemViewModel =
      PaginationItemViewModel(
        number             = number,
        href               = href,
        visuallyHiddenText = None,
        current            = false,
        ellipsis           = false,
        attributes         = Map.empty
      )

    def ellipsis(): PaginationItemViewModel =
      PaginationItemViewModel(
        number             = emptyString,
        href               = emptyString,
        visuallyHiddenText = None,
        current            = false,
        ellipsis           = true,
        attributes         = Map.empty
      )
  }

  case class PaginationItemViewModel(
    number: String,
    href: String,
    visuallyHiddenText: Option[String],
    current: Boolean,
    ellipsis: Boolean,
    attributes: Map[String, String]
  ) {

    def withVisuallyHiddenText(visuallyHiddenText: String): PaginationItemViewModel =
      copy(visuallyHiddenText = Some(visuallyHiddenText))

    def withCurrent(current: Boolean): PaginationItemViewModel =
      copy(current = current)

    def withAttributes(attributes: Map[String, String]): PaginationItemViewModel =
      copy(attributes = attributes)

    def asPaginationItem(implicit messages: Messages): PaginationItem = {
      PaginationItem(
        number             = if (ellipsis) None else Some(number),
        href               = if (ellipsis) emptyString else href,
        visuallyHiddenText = visuallyHiddenText.map(messages(_)),
        current            = if (current) Some(true) else None,
        ellipsis           = if (ellipsis) Some(true) else None,
        attributes         = attributes
      )
    }
  }

  object PaginationLinkViewModel {

    def apply(href: String): PaginationLinkViewModel =
      PaginationLinkViewModel(
        href       = href,
        text       = None,
        html       = None,
        labelText  = None,
        attributes = Map.empty
      )
  }

  case class PaginationLinkViewModel(
    href: String,
    text: Option[String],
    html: Option[String],
    labelText: Option[String],
    attributes: Map[String, String]
  ) {

    def withText(text: String): PaginationLinkViewModel =
      copy(text = Some(text))

    def withHtml(html: String): PaginationLinkViewModel =
      copy(html = Some(html))

    def withLabelText(labelText: String): PaginationLinkViewModel =
      copy(labelText = Some(labelText))

    def withAttributes(attributes: Map[String, String]): PaginationLinkViewModel =
      copy(attributes = attributes)

    def asPaginationLink(implicit messages: Messages): PaginationLink = {
      PaginationLink(
        href       = href,
        text       = text.map(messages(_)),
        labelText  = labelText.map(messages(_)),
        attributes = attributes
      )
    }
  }
}

trait PaginationFluency
