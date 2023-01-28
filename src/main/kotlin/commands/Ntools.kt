package top.cutestar.networkTools.commands

import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CompositeCommand
import top.cutestar.networkTools.Config
import top.cutestar.networkTools.NetworkTools
import top.cutestar.networkTools.NetworkTools.reload

object Ntools: CompositeCommand(
    owner = NetworkTools,
    primaryName = "ntools",
    description = "重载配置"
) {
    @SubCommand
    @Description("重载配置")
    suspend fun CommandSender.reload() {
        Config.reload()
        sendMessage("已重载配置")
    }

    @SubCommand
    @Description("查看帮助命令")
    suspend fun CommandSender.help() {
        sendMessage("""◆ /tracert <目标名称>    # 路由追踪
◆ /ping <目标名称> [ping次数] [超时时间(毫秒)]    # Ping连接测试
◆ /dns <记录名称> [记录类型] [自定义DNS]    # dns查询
◆ /web <URL链接> [编码]    # 网页测试
◆ /nmap <目标名称>    # 端口扫描
  /nmap <目标名称> <起始端口> <结束端口>    # 端口扫描
  /nmap <目标名称> <端口>    # 端口扫描
◆ /doh <域名> [类型]    # HTTPS加密DNS查询
◆ /gc <目标名称>    # 查墙
◆ /ntools help    # 查看帮助命令
  /ntools reload    # 重载配置""".trimIndent())
    }
}