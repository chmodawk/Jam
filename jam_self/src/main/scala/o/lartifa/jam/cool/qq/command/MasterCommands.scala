package o.lartifa.jam.cool.qq.command

import cc.moecraft.icq.command.interfaces.IcqCommand
import cc.moecraft.icq.event.events.message.EventMessage
import cc.moecraft.icq.user.User
import o.lartifa.jam.common.config.JamConfig
import o.lartifa.jam.cool.qq.command.base.MasterEverywhereCommand
import o.lartifa.jam.engine.JamLoader
import o.lartifa.jam.model.tasks.{GoASleep, WakeUp}
import o.lartifa.jam.model.{ChatInfo, CommandExecuteContext}
import o.lartifa.jam.plugins.JamPluginLoader
import o.lartifa.jam.pool.JamContext

import java.util
import java.util.concurrent.Executors
import scala.async.Async._
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

/**
 * 监护人指令
 *
 * Author: sinar
 * 2020/1/5 03:12
 */
object MasterCommands {

  private implicit val exec: ExecutionContextExecutor = ExecutionContext.fromExecutor(
    Executors.newFixedThreadPool(Runtime.getRuntime.availableProcessors()))

  def commands: List[IcqCommand] = List(
    Ping, SessionInfo, Refresh, ReloadSSDL,
    ListVariable, ClearVariableInChat, SetVariable, RemoveVariable,
    ListPlugins, EnablePlugin, DisablePlugin, UninstallPlugin,
    WakeUpNow, GoASleepNow, ShowRawMessage
  ) ++ JamPluginLoader.loadedComponents.masterCommands

  private object Ping extends MasterEverywhereCommand("ping", "在吗") {
    /**
     * 指令操作
     *
     * @param event   消息事件
     * @param sender  发送者
     * @param command 指令内容
     * @param args    参数
     * @return 输出内容
     */
    override def task(event: EventMessage, sender: User, command: String, args: util.ArrayList[String]): Future[String] =
      Future.successful("我在")
  }

  private object ListVariable extends MasterEverywhereCommand("列出变量") {
    /**
     * 指令操作
     *
     * @param event   消息事件
     * @param sender  发送者
     * @param command 指令内容
     * @param args    参数
     * @return 输出内容
     */
    override def task(event: EventMessage, sender: User, command: String, args: util.ArrayList[String]): Future[String] = async {
      val pool = JamContext.variablePool
      val result: String = await(pool.listAll()).sortBy(_.chatId).map(row => s"ID：${row.chatId}，变量名：${row.name}，值：${row.value}").mkString("\n")
      if ("" == result) event.respond("当前会话没有变量哦")
      result
    }
  }

  private object ClearVariableInChat extends MasterEverywhereCommand("清空变量", "清理变量") {
    /**
     * 指令操作
     *
     * @param event   消息事件
     * @param sender  发送者
     * @param command 指令内容
     * @param args    参数
     * @return 输出内容
     */
    override def task(event: EventMessage, sender: User, command: String, args: util.ArrayList[String]): Future[String] = async {
      val pool = JamContext.variablePool
      await(pool.cleanAllInChat(event))
      "我已经把它们忘掉啦！"
    }
  }

  private object SetVariable extends MasterEverywhereCommand("设置变量") {
    /**
     * 指令操作
     *
     * @param event   消息事件
     * @param sender  发送者
     * @param command 指令内容
     * @param args    参数
     * @return 输出内容
     */
    override def task(event: EventMessage, sender: User, command: String, args: util.ArrayList[String]): Future[String] = async {
      if (args.size() != 2) {
        // 输入不合法
        "？"
      } else {
        val name = args.get(0)
        val value = args.get(1)
        val pool = JamContext.variablePool
        await(pool.updateOrElseSet(name, value)(CommandExecuteContext(event)).map(_ => "变量设置成功！"))
      }
    }
  }

  private object RemoveVariable extends MasterEverywhereCommand("删除变量") {
    /**
     * 指令操作
     *
     * @param event   消息事件
     * @param sender  发送者
     * @param command 指令内容
     * @param args    参数
     * @return 输出内容
     */
    override def task(event: EventMessage, sender: User, command: String, args: util.ArrayList[String]): Future[String] = async {
      if (args.isEmpty) {
        "？？"
      } else {
        val name = args.get(0)
        val pool = JamContext.variablePool
        await(pool.delete(name)(CommandExecuteContext(event)).map(_ => "变量删除成功！"))
      }
    }
  }

  private object ReloadSSDL extends MasterEverywhereCommand("重新解析", "R") {
    /**
     * 指令操作
     *
     * @param event   消息事件
     * @param sender  发送者
     * @param command 指令内容
     * @param args    参数
     * @return 输出内容
     */
    override def task(event: EventMessage, sender: User, command: String, args: util.ArrayList[String]): Future[String] = async {
      await(JamLoader.reloadSSDL()(CommandExecuteContext(event)))
      NO_RESPONSE
    }
  }

  private object SessionInfo extends MasterEverywhereCommand("聊天信息", "会话信息") {
    /**
     * 指令操作
     *
     * @param event   消息事件
     * @param sender  发送者
     * @param command 指令内容
     * @param args    参数
     * @return 输出内容
     */
    override def task(event: EventMessage, sender: User, command: String, args: util.ArrayList[String]): Future[String] = async {
      val ChatInfo(chatType, chatId) = ChatInfo(event)
      s"""会话类型为：$chatType
         |会话 ID 为：$chatId""".stripMargin
    }
  }

  private object Refresh extends MasterEverywhereCommand("刷新", "刷新缓存") {
    /**
     * 指令操作
     *
     * @param event   消息事件
     * @param sender  发送者
     * @param command 指令内容
     * @param args    参数
     * @return 输出内容
     */
    override def task(event: EventMessage, sender: User, command: String, args: util.ArrayList[String]): Future[String] = async {
      event.getBot.getAccountManager.refreshCache()
      "缓存刷新成功"
    }
  }

  private object ListPlugins extends MasterEverywhereCommand("列出已安装插件", "列出插件") {
    /**
     * 指令操作
     *
     * @param event   消息事件
     * @param sender  发送者
     * @param command 指令内容
     * @param args    参数
     * @return 输出内容
     */
    override def task(event: EventMessage, sender: User, command: String, args: util.ArrayList[String]): Future[String] = async {
      val plugins = await(JamPluginLoader.listPlugin()).map(it => {
        s"${it.id}：[${if (it.isEnabled) "启用" else "禁用"}]${it.name}，" +
          s"包名：${it.`package`}，作者：${it.author}"
      })
      if (plugins.isEmpty) "当前没有安装任何插件"
      else "当前已安装的插件列表如下：\n" + plugins.mkString("\n")
    }
  }

  private object EnablePlugin extends MasterEverywhereCommand("启用插件") {
    /**
     * 指令操作
     *
     * @param event   消息事件
     * @param sender  发送者
     * @param command 指令内容
     * @param args    参数
     * @return 输出内容
     */
    override def task(event: EventMessage, sender: User, command: String, args: util.ArrayList[String]): Future[String] = {
      if (args.isEmpty) {
        Future.successful("请指定要禁用的插件编号")
      } else {
        Future.sequence {
          argsToIds(event, args, "插件")
            .map(id => JamPluginLoader.enablePlugin(event, id))
        }.map(_ => NO_RESPONSE)
      }
    }
  }

  private object DisablePlugin extends MasterEverywhereCommand("禁用插件") {
    /**
     * 指令操作
     *
     * @param event   消息事件
     * @param sender  发送者
     * @param command 指令内容
     * @param args    参数
     * @return 输出内容
     */
    override def task(event: EventMessage, sender: User, command: String, args: util.ArrayList[String]): Future[String] = {
      if (args.isEmpty) {
        Future.successful("请指定要启用用的插件编号")
      } else {
        Future.sequence {
          argsToIds(event, args, "插件")
            .map(id => JamPluginLoader.disablePlugin(event, id))
        }.map(_ => NO_RESPONSE)
      }
    }
  }

  private object UninstallPlugin extends MasterEverywhereCommand("卸载插件") {
    /**
     * 指令操作
     *
     * @param event   消息事件
     * @param sender  发送者
     * @param command 指令内容
     * @param args    参数
     * @return 输出内容
     */
    override def task(event: EventMessage, sender: User, command: String, args: util.ArrayList[String]): Future[String] = {
      if (args.isEmpty) {
        Future.successful("请指定要启用用的插件编号")
      } else {
        Future.sequence {
          argsToIds(event, args, "插件")
            .map(id => JamPluginLoader.uninstallPlugin(event, id))
        }.map(_ => NO_RESPONSE)
      }
    }
  }

  private object WakeUpNow extends MasterEverywhereCommand("唤醒", "嗅盐治晕倒") {
    /**
     * 指令操作
     *
     * @param event   消息事件
     * @param sender  发送者
     * @param command 指令内容
     * @param args    参数
     * @return 输出内容
     */
    override def task(event: EventMessage, sender: User, command: String, args: util.ArrayList[String]): Future[String] = {
      WakeUp.wakeUp()
      Future.successful(s"${JamConfig.name}已苏醒")
    }
  }

  private object GoASleepNow extends MasterEverywhereCommand("休眠", "昏睡红茶") {
    /**
     * 指令操作
     *
     * @param event   消息事件
     * @param sender  发送者
     * @param command 指令内容
     * @param args    参数
     * @return 输出内容
     */
    override def task(event: EventMessage, sender: User, command: String, args: util.ArrayList[String]): Future[String] = {
      GoASleep.goASleep()
      Future.successful(s"${JamConfig.name}已休眠")
    }
  }

  private object ShowRawMessage extends MasterEverywhereCommand("原始信息", "raw") {
    /**
     * 指令操作
     *
     * @param event   消息事件
     * @param sender  发送者
     * @param command 指令内容
     * @param args    参数
     * @return 输出内容
     */
    override def task(event: EventMessage, sender: User, command: String, args: util.ArrayList[String]): Future[String] = Future {
      event.respond(event.getRawMessage, true)
      NO_RESPONSE
    }
  }

}
