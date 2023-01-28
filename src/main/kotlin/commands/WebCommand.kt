package top.cutestar.networkTools.commands

import kotlinx.coroutines.launch
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.SimpleCommand
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import top.cutestar.networkTools.Config
import top.cutestar.networkTools.NetworkTools
import top.cutestar.networkTools.utils.HttpUtil
import top.cutestar.networkTools.utils.Util
import top.cutestar.networkTools.utils.Util.autoToForwardMsg
import top.cutestar.networkTools.utils.Util.getValue
import top.cutestar.networkTools.utils.Util.withHelper
import java.util.regex.Pattern
import kotlin.system.measureTimeMillis

object WebCommand : SimpleCommand(
    owner = NetworkTools,
    primaryName = "web",
    description = "网页测试"
) {
    @OptIn(ConsoleExperimentalApi::class)
    @Handler
    fun CommandSender.onHandler(
        @Name("URL链接") s: String,
        @Name("编码") charset: String = Config.webCharset
    ) = withHelper {
        val urls = mutableSetOf<String>()
        getValue(s).forEach {
            val url = if (":/" in it) it else "https://$it"
            urls.add(url)
        }
        executeWeb(urls, charset)
    }

    private fun CommandSender.executeWeb(urls: MutableSet<String>, charset: String) = launch{
        val words = mutableListOf<String>()
        urls.forEach { url ->
            val http: HttpUtil
            val time = measureTimeMillis { http = HttpUtil(url) }
            val connection = http.getConnection()
            var server = ""
            connection.headerFields.forEach headerFields@{ (k, v) ->
                if (k.equals("server", true)) {
                    server = v[0]
                    return@headerFields
                }
            }
            val s = http.getString(charset, false).replace("\n", "")
            val status = "${connection.responseCode}(${connection.responseMessage})"

            sendMessage(
                    ("""---Web测试---
$url
标题:${getTitle(s)}
状态码:$status
http延时:${time}ms
服务端:$server
""".trimIndent())
            )

            words.addAll(arrayOf("描述:\n${getDescription(s)}", "关键词:\n${getKeywords(s)}", "链接:"))
            var length = 0

            getKeyLink(s, url).forEach link@{
                val text = "${it.key}\n${it.value}"
                length += text.length
                words.add(if (length <= 3000) text else return@link)
            }
            http.close()
        }
        autoToForwardMsg(words)
    }

    fun getTitle(s: String) = Util.matchText(s, "<title>((.*?){1,50})</title>")

    fun getDescription(s: String): String {
        return Util.matchText(s, "<meta.*?name=\"description\".*?content=\"(.*?)\"")
            .replace(" ", "\n")
            .replace(",", "\n")
    }

    fun getKeywords(s: String) =
        Util.matchText(s, "<meta.*?name=\"keywords\".*?content=\"(.*?)\"")
            .replace(" ", "\n")
            .replace(",", "\n")

    fun getKeyLink(s: String, url: String): MutableMap<String, String> {
        val map = mutableMapOf<String, String>()
        val m = Pattern.compile("<a.*?href=\"(.*?)\".*?>(.*?)</a").matcher(s)
        while (m.find()) {
            val g1 = m.group(1)
            val httpRule = ".*https?://.*"
            val url = if (Pattern.matches(httpRule, g1)) g1 else url + g1
            m.group(2).run {
                if (
                    length <= 100 &&
                    isNotEmpty() &&
                    url.isNotEmpty() &&
                    !Pattern.matches(httpRule, this)
                ) map[this] = url
            }
        }
        return map
    }
}