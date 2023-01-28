package top.cutestar.networkTools.commands

import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.SimpleCommand
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import top.cutestar.networkTools.Config
import top.cutestar.networkTools.NetworkTools
import top.cutestar.networkTools.utils.Util.autoToForwardMsg
import top.cutestar.networkTools.utils.Util.getValue
import top.cutestar.networkTools.utils.Util.withHelper
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.Executors
import kotlin.system.measureTimeMillis

@OptIn(ConsoleExperimentalApi::class)
object NmapCommand : SimpleCommand(
    owner = NetworkTools,
    primaryName = "nmap",
    description = "端口扫描",
) {
    @Handler
    fun CommandSender.onHandler(
        @Name("目标名称") hosts: String,
    ) {
        executeNmap(getValue(hosts), (1..1000).toMutableSet())
    }

    @Handler
    fun CommandSender.onHandler(
        @Name("目标名称") hosts: String,
        @Name("端口") ports: String
    ) {
        val set = mutableSetOf<Int>()
        ports.toIntOrNull().let {
            if (it != null) set.add(it)
        }
        executeNmap(getValue(hosts), set)
    }

    @Handler
    fun CommandSender.onHandler(
        @Name("目标名称") hosts: String,
        @Name("起始端口") startPort: Int,
        @Name("结束端口") endPort: Int,
    ) = withHelper {
        when {
            startPort > endPort -> throw IllegalArgumentException("起始端口大于结束端口")
            startPort !in 1..65535 || endPort !in 1..65535 -> throw IllegalArgumentException("端口号错误\n1-65535")
        }
        executeNmap(getValue(hosts), (startPort..endPort).toMutableSet())
    }

    private fun CommandSender.executeNmap(hosts: MutableSet<String>, ports: MutableSet<Int>) = launch{
        sendMessage("正在扫描${hosts.size * ports.size}个端口，这需要一段时间")
        val nmapDispatcher = Executors.newFixedThreadPool(Config.nmapPoolSize).asCoroutineDispatcher()
        val words = mutableListOf("TCP端口扫描")
        hosts.forEach { host ->
            val activePorts = mutableListOf<Int>()
            val time = measureTimeMillis {
                launch {
                    ports.forEach { port ->
                        launch(nmapDispatcher) {
                            Socket().run {
                                try {
                                    connect(InetSocketAddress(host, port), Config.nmapTimeout)
                                    activePorts.add(port)
                                } catch (_: Exception) {
                                } finally {
                                    close()
                                }
                            }
                        }
                    }
                }.join()
            }
            activePorts.sort()//排序 顺序

            words.add(
                """$host
用时:${time.toDouble() / 1000}秒
端口数量:${activePorts.size}
开放的TCP端口:
${activePorts.joinToString("\n")}""".trimIndent()
            )
        }

        autoToForwardMsg(words)
    }
}