package o.lartifa.jam.model.behaviors

import cc.moecraft.icq.event.events.message.{EventGroupOrDiscussMessage, EventPrivateMessage}
import cc.moecraft.icq.sender.message.components.ComponentAt
import cc.moecraft.icq.sender.returndata.ReturnData
import cc.moecraft.icq.sender.returndata.returnpojo.send.RMessageReturnData
import o.lartifa.jam.model.CommandExecuteContext

import scala.async.Async.{async, await}
import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions

/**
 * 行为：回复给朋友
 *
 * Author: sinar
 * 2020/9/2 00:56
 */
trait ReplyToFriend {
  /**
   * 消息中对方的昵称
   * 群：尝试获取群昵称，不存在/失败则使用 QQ 昵称
   * 私聊：QQ 昵称
   *
   * @param context 指令上下文
   * @param exec    执行上下文
   * @return 昵称
   */
  protected def senderNickName(implicit context: CommandExecuteContext, exec: ExecutionContext): Future[String] = async {
    context.eventMessage match {
      case _: EventGroupOrDiscussMessage =>
        await(context.tempVars.get("对方群昵称")) match {
          case Some(value) => value
          case None => await(context.tempVars.get("对方昵称")).getOrElse("这个人")
        }
      case _: EventPrivateMessage =>
        await(context.tempVars.get("对方昵称")).getOrElse("你")
      case _ => "这个人"
    }
  }

  /**
   * At 消息发送者
   *
   * @param context 指令上下文
   * @return at 的 CQ 码
   */
  protected def atSender(implicit context: CommandExecuteContext): String =
    new ComponentAt(context.eventMessage.getSenderId).toString

  /**
   * 快速回复消息
   *
   * @param message 消息内容
   * @param raw     是否不转义（默认自动转义）
   * @param context 指令上下文
   * @return 操作结果
   */
  protected def reply(message: String, raw: Boolean = false)(implicit context: CommandExecuteContext): ReturnData[RMessageReturnData] =
    context.eventMessage.respond(message, raw)

  /**
   * 快速回复消息
   *
   * @param anyThing 任何对象
   * @param context  指令上下文
   * @return 操作结果
   */
  protected def reply(anyThing: Any)(implicit context: CommandExecuteContext): ReturnData[RMessageReturnData] =
    context.eventMessage.respond(anyThing.toString)
}
