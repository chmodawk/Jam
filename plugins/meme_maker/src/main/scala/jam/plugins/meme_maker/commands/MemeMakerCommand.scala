package jam.plugins.meme_maker.commands

import cc.moecraft.icq.sender.message.components.ComponentImage
import jam.plugins.meme_maker.engine.MemeAPIResponse.TemplateInfo
import jam.plugins.meme_maker.engine.MemeMakerAPI
import o.lartifa.jam.cool.qq.listener.asking.{Answerer, Result}
import o.lartifa.jam.model.CommandExecuteContext
import o.lartifa.jam.model.commands.Command

import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

/**
 * Meme maker 指令
 *
 * Author: sinar
 * 2020/11/18 22:19
 */
object MemeMakerCommand extends Command[Unit] {
  /**
   * 执行
   *
   * @param context 执行上下文
   * @param exec    异步上下文
   * @return 异步返回执行结果
   */
  override def execute()(implicit context: CommandExecuteContext, exec: ExecutionContext): Future[Unit] = {
    val templates = MemeMakerAPI.allTemplates
    if (templates.nonEmpty) {
      context.eventMessage.respond(
        """已启用表情动图制作工具，
          |请选择你想制作的模板编号
          |---------------------
          |输入"上一页"，"下一页"进行翻页
          |输入"预览"加上模板编号查看示例
          |输入模板编号开始制作
          |输入"退出"结束制作""".stripMargin)
      step1SelectTemplate(templates, 1, math.ceil(templates.size / 5).toInt)
    } else {
      context.eventMessage.respond("模板尚未准备好，请稍后重试")
    }
    Future.unit
  }

  /**
   * 第一步，选择模板（一页十条）
   *
   * @param templates 全部模板
   * @param page      当前页数
   * @param total     总页数
   * @param context   执行上下文
   * @param exec      异步上下文
   */
  private def step1SelectTemplate(templates: List[TemplateInfo], page: Int, total: Int)(implicit context: CommandExecuteContext, exec: ExecutionContext): Unit = {
    val info = templates.slice((page - 1) * 5, page * 5).map { it =>
      import it._
      s"$id：$name（${like_count}人喜欢）"
    }.mkString("\n")

    context.eventMessage.respond(info)

    Answerer.sender ? { ctx =>
      ctx.event.message match {
        case "退出" =>
          reply("已退出")
          Future.successful(Result.Complete)
        case "上一页" =>
          if (page == 1) {
            reply("已经是第一页啦")
            step1SelectTemplate(templates, 1, total)
            Future.successful(Result.Complete)
          } else {
            step1SelectTemplate(templates, page - 1, total)
            Future.successful(Result.Complete)
          }
        case "下一页" =>
          if (page == total) {
            reply("已经是最后一页啦")
            Future.successful(Result.KeepCountAndContinueAsking)
          } else {
            step1SelectTemplate(templates, page + 1, total)
            Future.successful(Result.Complete)
          }
        case msg if msg.startsWith("预览") =>
          Try(msg.stripPrefix("预览").trim.toLong) match {
            case Failure(_) =>
              reply("请输入正确的序号！")
              Future.successful(Result.KeepCountAndContinueAsking)
            case Success(id) =>
              templates.find(_.id == id) match {
                case Some(template) =>
                  reply(new ComponentImage(template.example_gif).toString)
                  Future.successful(Result.KeepCountAndContinueAsking)
                case None =>
                  reply("没有该模板！")
                  Future.successful(Result.KeepCountAndContinueAsking)
              }
          }
        case other =>
          Try(other.toLong) match {
            case Failure(_) =>
              reply("请输入正确的序号！")
              Future.successful(Result.KeepCountAndContinueAsking)
            case Success(id) =>
              templates.find(_.id == id) match {
                case Some(template) =>
                  context.eventMessage.respond(
                    s"""已选择模板${template.name}，
                       |开始填充模板内容
                       |---------------------
                       |发送消息填充模板
                       |输入"=预览"加上模板编号查看示例
                       |输入"=退出"结束制作""".stripMargin)
                  fillTemplate(template)
                  Future.successful(Result.Complete)
                case None =>
                  reply("没有该模板！")
                  Future.successful(Result.KeepCountAndContinueAsking)
              }
          }
      }
    }
  }

  /**
   * 填充当前模板
   *
   * @param templateInfo 模板信息
   * @param context      执行上下文
   * @param exec         异步上下文
   */
  private def fillTemplate(templateInfo: TemplateInfo)(implicit context: CommandExecuteContext, exec: ExecutionContext): Unit = {
    val slots = templateInfo.templateSlots
    val sentences: ListBuffer[String] = ListBuffer.empty
    reply(
      s"""请填写第${sentences.size + 1}条句子
         |---------------------
         |示例：${slots(sentences.size)}""".stripMargin)
    Answerer.sender ? { ctx =>
      ctx.event.message match {
        case "=退出" =>
          reply("已退出")
          Future.successful(Result.Complete)
        case "=预览" =>
          reply(new ComponentImage(templateInfo.example_gif).toString)
          Future.successful(Result.KeepCountAndContinueAsking)
        case sentence =>
          sentences += sentence
          if (sentences.sizeIs == slots.size) {
            reply("填充完毕！正在生成...")
            Try(reply(MemeMakerAPI
              .generate(templateInfo.id, sentences.toList)
              .map(_.toString)
              .getOrElse("生成失败，请稍后重试")))
            Future.successful(Result.Complete)
          } else {
            reply(
              s"""请填写第${sentences.size + 1}条句子
                 |---------------------
                 |示例：${slots(sentences.size)}""".stripMargin)
            Future.successful(Result.KeepCountAndContinueAsking)
          }
      }
    }
  }
}
