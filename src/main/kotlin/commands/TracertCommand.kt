package top.cutestar.networkTools.commands

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.SimpleCommand
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import top.cutestar.networkTools.Config
import top.cutestar.networkTools.NetworkTools
import top.cutestar.networkTools.utils.Util
import top.cutestar.networkTools.utils.Util.autoToForwardMsg
import top.cutestar.networkTools.utils.Util.getValue
import top.cutestar.networkTools.utils.Util.withHelper
import java.nio.charset.Charset
import java.util.regex.Pattern
import kotlin.system.measureTimeMillis

object TracertCommand : SimpleCommand(
    owner = NetworkTools,
    primaryName = "tracert",
    "tr",
    description = "路由追踪"
) {
    val rule: Pattern = Pattern.compile("[a-zA-Z0-9.:]")

    @OptIn(ConsoleExperimentalApi::class)
    @Handler
    suspend fun CommandSender.onHandler(@Name("目标名称") hosts: String) = withHelper {
        val set = mutableSetOf<String>()
        getValue(hosts).forEach {
            val sb = StringBuilder()
            val m = rule.matcher(it)
            while (m.find()) {
                sb.append(m.group())
            }
            set.add(sb.toString())
        }

        sendMessage("正在追踪 请稍等")
        executeTracert(set)
    }

    private fun CommandSender.executeTracert(hosts: MutableSet<String>) = launch {
        val words = mutableListOf("路由追踪")
        launch {
            hosts.forEach { host ->
                launch(Dispatchers.IO) {
                    val sb = StringBuilder()
                    val command =
                        (if ("windows" in System.getProperty("os.name").lowercase())
                            Config.tracertWCommand
                        else
                            Config.tracertOCommand).replace("${"$"}address", host)
                    val process = Runtime.getRuntime().exec(command)
                    val time = measureTimeMillis { process.waitFor() }
                    val buffer = process.inputStream.bufferedReader(Charset.forName(Config.consoleCharset))
                    var s = buffer.readLine()
                    while (s != null) {
                        val msg = StringBuilder(s).append("\n")
                        Util.getIp(s).let { ip ->
                            if (ip != null) msg.append(Util.getLocation(ip)).append("\n\n")
                        }
                        sb.append(msg.toString())
                        s = buffer.readLine()
                    }
                    sb.append("\n用时:${time.toDouble() / 1000}秒")
                    words.add(sb.toString())
                }
            }
        }.join()

        autoToForwardMsg(words)
    }
}

