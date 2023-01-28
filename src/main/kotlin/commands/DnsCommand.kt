package top.cutestar.networkTools.commands

import kotlinx.coroutines.*
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.SimpleCommand
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import top.cutestar.networkTools.Config
import top.cutestar.networkTools.NetworkTools
import top.cutestar.networkTools.utils.Util.autoToForwardMsg
import top.cutestar.networkTools.utils.Util.dnsQuery
import top.cutestar.networkTools.utils.Util.getLocation
import top.cutestar.networkTools.utils.Util.getValue
import top.cutestar.networkTools.utils.Util.withHelper
import kotlin.system.measureTimeMillis

object DnsCommand : SimpleCommand(
    owner = NetworkTools,
    primaryName = "dns",
    description = "dns查询"
) {
    @OptIn(ConsoleExperimentalApi::class)
    @Handler
    fun CommandSender.onHandler(
        @Name("记录名称") s: String,
        @Name("记录类型") type: String = "default",
        @Name("自定义DNS") dns: String = Config.dnsAddress
    ) = withHelper {
        val types = if (type == "default") Config.dnsTypes else getValue(type)
        executeDnsQuery(getValue(s), types, dns)
    }

    private fun CommandSender.executeDnsQuery(
        hosts: MutableSet<String>,
        types: MutableSet<String>,
        dns: String
    ) = launch {
        val words = mutableListOf<CharSequence>("查询完成,DNS:$dns")

        hosts.forEach { host ->
            val sb = StringBuilder(host)
            var count = 0

            types.forEach { type ->
                val list = dnsQuery(host, type, dns)
                if (list.isNotEmpty()) {
                    sb.append(" \n").append(type).append(":")
                    launch {
                        list.forEach {
                            count++
                            if (type.equals("A", true) || type.equals("AAAA", true)) {
                                launch(Dispatchers.IO) l@{
                                    val locationAsync = async { getLocation(it) }
                                    val address = PingCommand.getAddress(it) ?: return@l
                                    val time = run {
                                        val isReachable: Boolean
                                        val time = measureTimeMillis { isReachable = address.isReachable(2000) }
                                        if (isReachable) "${time}ms" else "超时"
                                    }

                                    val location = locationAsync.await()
                                    sb.run {
                                        append("\n")
                                        append(it)
                                        append(" ")
                                        append(time)
                                        append(" ")
                                        append(location)
                                        append("\n")
                                    }
                                }
                            } else sb.append("\n").append(it)
                        }
                    }.join()
                }
            }
            words.add(sb.append("\n共").append(count).append("个记录"))
        }

        autoToForwardMsg(words)
    }
}