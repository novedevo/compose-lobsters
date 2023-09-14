/*
 * Copyright © 2022-2023 Harsh Shandilya.
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package dev.msfjarvis.claw.api

import com.slack.eithernet.ApiResult.Companion.success
import com.slack.eithernet.test.EitherNetController
import com.slack.eithernet.test.enqueue
import dev.msfjarvis.claw.api.converters.CSRFTokenConverter
import dev.msfjarvis.claw.model.LobstersPost
import dev.msfjarvis.claw.model.LobstersPostDetails
import dev.msfjarvis.claw.model.User
import dev.msfjarvis.claw.util.TestUtils.getResource
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import okhttp3.MediaType
import okhttp3.ResponseBody

@OptIn(ExperimentalSerializationApi::class)
class ApiWrapper(controller: EitherNetController<LobstersApi>) {
  private val json = Json {
    ignoreUnknownKeys = true
    namingStrategy = JsonNamingStrategy.SnakeCase
  }
  private val hottest: List<LobstersPost> = json.decodeFromString(getResource("hottest.json"))
  private val postDetails: LobstersPostDetails =
    json.decodeFromString(getResource("post_details_tdfoqh.json"))
  private val user: User = json.decodeFromString(getResource("msfjarvis.json"))

  val api = controller.api

  init {
    controller.enqueue(LobstersApi::getHottestPosts) { success(hottest) }
    controller.enqueue(LobstersApi::getHottestPosts) { success(hottest) }
    controller.enqueue(LobstersApi::getPostDetails) { success(postDetails) }
    controller.enqueue(LobstersApi::getUser) { success(user) }
    controller.enqueue(LobstersApi::getCSRFToken) {
      success(
        CSRFTokenConverter.convert(
          ResponseBody.create(MediaType.get("text/html"), getResource("csrf_page.html"))
        )
      )
    }
  }
}
