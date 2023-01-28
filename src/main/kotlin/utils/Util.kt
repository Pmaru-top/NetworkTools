package top.cutestar.networkTools.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.message.data.ForwardMessageBuilder
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.buildMessageChain
import top.cutestar.networkTools.Config
import java.util.*
import java.util.regex.Pattern
import javax.naming.NameNotFoundException
import javax.naming.directory.InitialDirContext

object Util {
    @Serializable
    data class LocationData(val data: LocationData2)

    @Serializable
    data class LocationData2(val location: String)

    private const val LOCAL_TEXT = "本地局域网"
    private const val IP46_RULE =
        "((((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?))|((([a-fA-F0-9]){1,4}:)+(:?(([a-fA-F0-9]){1,4}:?)+)?))"

    val charset = ('a'..'z').toList() + ('A'..'Z').toList() + ('0'..'9').toList()

    fun getLocation(address: String): String {
        when {
            "192.168." in address -> return LOCAL_TEXT
            "172." in address -> {
                val second = address.split(".")[1].toInt()
                if (second in 16..31) return LOCAL_TEXT
            }
        }
        /**
         * from "ip.zxinc.org"
         */
        val url = "https://ip.zxinc.org/api.php?type=json&ip=$address"
        val s = HttpUtil(url, useProxy = false).getString(Config.webCharset)
        val json = Json { ignoreUnknownKeys = true }
        return json.decodeFromString<LocationData>(s).data.location
    }

    fun CommandSender.withHelper(block: suspend () -> Unit) = launch(Dispatchers.IO) {
        try {
            block.invoke()
        } catch (e: Exception) {
            sendMessage("执行失败:${e.message ?: "未知"}")
            e.printStackTrace()
        }
    }

    fun getIp(s: String): String? {
        val m = Pattern.compile(IP46_RULE).matcher(s)
        return if (m.find()) m.group() else null
    }

    fun dnsQuery(name: String, type: String, dns: String): MutableList<String> = try {
        val env = Hashtable<String, String>()
        val list = mutableListOf<String>()
        env["java.naming.factory.initial"] = "com.sun.jndi.dns.DnsContextFactory"
        env["java.naming.provider.url"] = "dns://$dns"

        val context =
            InitialDirContext(env).getAttributes(name, arrayOf(type)).get(type)
        if (context != null) repeat(context.size()) {
            list.add(context.get(it).toString())
        }
        list
    } catch (_: NameNotFoundException) {
        mutableListOf()
    }

    fun matchText(s: String, regex: String, default: String = ""): String {
        val m = Pattern.compile(regex).matcher(s.lowercase())
        return if (m.find()) m.group(1) else default
    }

    fun getValue(text: String, splitStr: Char = ',') = text.split(splitStr).toMutableSet().apply { remove("") }

    suspend fun CommandSender.autoToForwardMsg(words: Collection<CharSequence>) {
        var length = 0
        var forwardCount = 0
        val sb = StringBuilder()
        val forwardMsg = ForwardMessageBuilder(subject!!)

        run run@{
            //遍历words
            repeat(words.size) { count ->
                val word = words.toList()[count]
                length += word.length

                if (length < 3000) when (Config.noForwardMsg) {
                    true -> {
                        val limit = Config.textMsgLimit
                        if (length < limit) {
                            sb.append(word)
                            if (count + 1 != words.size) sb.append("\n\n")
                        } else {
                            sb.append(word.substring(0, word.length - (length - limit))).append("\n...")
                            return@run
                        }
                    }

                    false -> {
                        if (count >= 99) {
                            if (forwardCount <= 3) {
                                sendMessage(forwardMsg.build())

                                forwardMsg.clear()
                                forwardCount++
                            } else sendMessage("结果过多，请分批查询")
                        } else {
                            forwardMsg.add(bot!!.id, bot!!.nick, PlainText(word))
                        }
                    }

                } else {
                    sendMessage("结果过多，已尽量显示")
                    return@run
                }
            }
        }

        //发送
        when {
            sb.isNotEmpty() -> {
                sendMessage(buildMessageChain { add(PlainText(sb)) })
            }
        }
        if (!forwardMsg.isEmpty()) sendMessage(forwardMsg.build())
    }
}