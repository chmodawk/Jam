package o.lartifa.jam.common.config

import com.typesafe.config.{Config, ConfigFactory}

/**
 * 果酱配置
 *
 * Author: sinar
 * 2020/1/2 22:01
 */
object JamConfig {
  val config: Config = ConfigFactory.load().getConfig("bot")

  // Bot 姓名
  val name: String = config.getString("name")

  // Bot QQ
  val qID: Long = config.getLong("jam_qq")

  // Bot password
  val password: String = config.getString("password")

  // Master 用户
  val masterQID: Long = config.getLong("master_qq")

  // 响应频率
  val responseFrequency: Int = config.getInt("response_frequency")

  // 自动接受好友申请
  val autoAcceptFriendRequest: Boolean = config.getBoolean("auto_accept_friend_request")

  // 自动接收群申请
  val autoAcceptGroupRequest: Boolean = config.getBoolean("auto_accept_group_request")

  // 是否让关键词匹配乱序执行
  val matchOutOfOrder: Boolean = config.getBoolean("match_out_of_order")
}
