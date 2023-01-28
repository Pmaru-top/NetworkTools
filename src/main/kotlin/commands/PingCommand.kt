package top.cutestar.networkTools.commands

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.SimpleCommand
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import top.cutestar.networkTools.NetworkTools
import top.cutestar.networkTools.utils.Util.autoToForwardMsg
import top.cutestar.networkTools.utils.Util.getLocation
import top.cutestar.networkTools.utils.Util.getValue
import top.cutestar.networkTools.utils.Util.withHelper
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.text.NumberFormat
import kotlin.system.measureTimeMillis

object PingCommand : SimpleCommand(
    owner = NetworkTools,
    primaryName = "ping",
    description = "Ping连接测试"
) {
    @OptIn(ConsoleExperimentalApi::class)
    @Handler
    fun CommandSender.onHandler(
        @Name("目标名称") s: String,
        @Name("ping次数") count: Int = 3,
        @Name("超时时间(毫秒)") timeout: Int = 3000
    ) = withHelper {
        if (count !in 1..1000) throw java.lang.IllegalArgumentException("次数过大,只能在1-1000")
        executePing(getValue(s), timeout, count)
    }

    private fun CommandSender.executePing(hosts: MutableSet<String>, timeout: Int, count: Int) = launch {
        val words = mutableListOf<String>()
        hosts.forEach { host ->
            val splitStr = host.split(":")
            val timeList = mutableListOf<Long>()
            val ip: String
            val port: Int
            val pingName: String
            val location: Deferred<String>?
            val info: String
            val stats: String

                when (splitStr.size) {
                    1 -> {
                        pingName = "Ping"
                        port = -1
                        val address = getAddress(host)
                        if (address != null) {
                            ip = address.hostAddress
                            location = async { getLocation(ip) }//异步获取位置
                            repeat(count) {
                                val isReachable: Boolean
                                val time = measureTimeMillis { isReachable = address.isReachable(timeout) }
                                if (isReachable) timeList.add(time)
                            }
                        } else {
                            ip = "未知主机"
                            location = null
                        }
                    }

                    2 -> {
                        pingName = "TcpPing"
                        val address = getAddress(splitStr[0])
                        port = splitStr[1].toIntOrNull() ?: return@forEach
                        if (address != null) {
                            ip = address.hostAddress
                            location = async { getLocation(ip) }
                            repeat(count) {
                                val socket = Socket()
                                try {
                                    val time =
                                        measureTimeMillis { socket.connect(InetSocketAddress(ip, port), timeout) }
                                    timeList.add(time)
                                } catch (_: IOException) {}
                            }
                        } else {
                            ip = "未知主机"
                            location = null
                        }
                    }
                    else -> return@forEach
                }

                val size = timeList.size
                val loss = count - size
                val success = count - loss
                val format = NumberFormat.getPercentInstance().apply { maximumFractionDigits = 2 }
                val lossRate = format.format(loss.toDouble() / count.toDouble())
                val time: String = when (success == 0) {
                    true -> "请求超时"
                    false -> {
                        timeList.sort()
                        "${timeList[(size - 1) / 2]}"
                    }
                }
                info = when (success > 0) {
                    true -> "最短:${timeList.minOrNull()}ms 最长:${timeList.maxOrNull()}ms 平均:${time}ms"
                    false -> "连接超时"
                }

                stats = "已发送:$count 成功:$size 丢包:$loss($lossRate)"
                words.add(
                        """$pingName
${if (host != ip) "$host\n" else ""}$ip${if (port != -1) ":$port" else ""}
$info
地区:${location?.await() ?: "null"}
$stats""".trimIndent()
                    )

        }
        autoToForwardMsg(words)
    }

    fun getAddress(host: String) = try {
        InetAddress.getByName(host)
    } catch (_: IOException) {
        null
    }
}