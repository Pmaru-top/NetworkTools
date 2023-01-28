package top.cutestar.networkTools.commands

import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.SimpleCommand
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import top.cutestar.networkTools.Config
import top.cutestar.networkTools.NetworkTools
import top.cutestar.networkTools.utils.HttpUtil
import top.cutestar.networkTools.utils.Util.autoToForwardMsg
import top.cutestar.networkTools.utils.Util.getValue
import top.cutestar.networkTools.utils.Util.withHelper

object DoHCommand : SimpleCommand(
    owner = NetworkTools,
    primaryName = "doh",
    description = "HTTPS加密DNS查询"
) {
    @Serializable
    data class DoHObject(@SerialName("Status") val status: Int, @SerialName("Answer") val answer: List<Answer>)

    @Serializable
    data class Answer(val data: String)

    @OptIn(ConsoleExperimentalApi::class)
    @Handler
    fun CommandSender.onHandler(
        @Name("域名") s: String,
        @Name("类型") type: String = "A"
    ) = withHelper {
        executeDoH(getValue(s), getValue(type))
    }

    fun CommandSender.executeDoH(hosts: MutableSet<String>, types: MutableSet<String>) = launch{
        val json = Json { ignoreUnknownKeys = true }
        val headers = mutableMapOf("accept" to "application/dns-json")
        val word = mutableListOf<CharSequence>("---DNS over HTTPS---")
        hosts.forEach { host ->
            val sb = StringBuilder(host)
            types.forEach { type ->
                sb.append("\n").append(type)
                val url = Config.dohAddress.replace("${"$"}s", host).replace("${"$"}type", type)
                try {
                    json.decodeFromString<DoHObject>(HttpUtil(url, headers).getString()).run {
                        if (status == 0) answer.forEach { sb.append("\n").append(it.data) }
                        else sb.append("\n").append("查询失败")
                    }
                } catch (_: Exception) {
                }
            }
            word.add(sb)
        }

        autoToForwardMsg(word)
    }
}