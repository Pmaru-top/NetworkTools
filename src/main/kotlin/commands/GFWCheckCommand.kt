package top.cutestar.networkTools.commands

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.SimpleCommand
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import top.cutestar.networkTools.Config
import top.cutestar.networkTools.NetworkTools
import top.cutestar.networkTools.utils.GFWCheck.listCheck
import top.cutestar.networkTools.utils.GFWCheck.localTest
import top.cutestar.networkTools.utils.GFWCheck.onlineCheck
import top.cutestar.networkTools.utils.GFWCheck.proxyTest
import top.cutestar.networkTools.utils.Util.autoToForwardMsg
import top.cutestar.networkTools.utils.Util.getValue
import top.cutestar.networkTools.utils.Util.withHelper

object GFWCheckCommand: SimpleCommand(
    owner = NetworkTools,
    primaryName = "gc",
    description = "查墙"
) {
    @OptIn(ConsoleExperimentalApi::class)
    @Handler
    fun CommandSender.onHandler(
        @Name("目标名称")hosts: String
    ) = withHelper{
        executeGC(getValue(hosts))
    }

    private fun CommandSender.executeGC(hosts: MutableSet<String>) = launch{
        val words = mutableListOf("查墙")

        launch {
            hosts.forEach { s ->
                launch {
                    var port = -1
                    val host: String = if (":" in s) {
                        port = s.substring(s.indexOf(":") + 1).toInt()
                        s.substring(0, s.indexOf(":"))
                    } else s

                    val onlineTestAsync = async { onlineCheck(host) }
                    val listCheckAsync = async { listCheck(host) }

                    var localTestAsync: Deferred<Boolean>? = null
                    var proxyTestAsync: Deferred<Boolean>? = null
                    if (port != -1 && Config.proxyEnabled) {
                        localTestAsync = async { localTest(host, port) }
                        proxyTestAsync = async { proxyTest(host, port) }
                    }

                    val onlineTest = StringBuilder()
                    onlineTestAsync.await().forEach { (k, v) ->
                        onlineTest.append("\n").append("$k : $v")
                    }

                    val localTest = StringBuilder()
                    if (localTestAsync != null && proxyTestAsync != null) {
                        localTest.append("本地测试:${localTestAsync.await()}").append("\n")
                        localTest.append("代理测试:${proxyTestAsync.await()}")
                    }

                    words.add(
                        """$host
在线测试:$onlineTest
GFWList:${if(listCheckAsync.await()) "在黑名单" else "不在黑名单"}
$localTest""".trimIndent()
                    )
                }
            }
        }.join()
        autoToForwardMsg(words)
    }
}