package io.github.antonkw.model

import java.time.LocalDateTime

case class Metrics(uniqueUsers: Long, impressions: Long, clicks: Long, bucket: LocalDateTime) {
  def toShortString = s"users=$uniqueUsers;imp=$impressions;cl=$clicks"
  def toReport: String =
    s"""|unique_users,$uniqueUsers
        |clicks,$clicks
        |impressions,$impressions""".stripMargin
}

object Metrics {
  val emptyReport =
    s"""|unique_users,0
        |clicks,0
        |impressions,0""".stripMargin

}
