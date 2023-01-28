package top.cutestar.networkTools.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.cutestar.networkTools.Config
import top.cutestar.networkTools.NetworkTools
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.Socket
import java.util.*
import java.util.regex.Pattern

object GFWCheck {
    private var gfwList: Array<String>? = null
    private val logger = NetworkTools.logger

    fun onlineCheck(host: String): MutableMap<String, String> {
        val s = HttpUtil("http://www.chinafirewalltest.com/?siteurl=$host").getString()

        var table = ""
        val m = Pattern.compile("<table .*?id=\"testresults\">(.*?)</table>").matcher(s)
        while (m.find()) {
            table = m.group(1)
        }

        val map = mutableMapOf<String, String>()
        Pattern.compile("<td class=\"resultlocation\">(.*?)</td>.*?<td class=\"resultstatus .*?\">(.*?)</td>")
            .matcher(table).run {
                while (find()) {
                    val key = group(1)
                        .replace("Beijing", "北京")
                        .replace("Shenzhen", "上海")
                        .replace("Inner Mongolia", "内蒙古")
                        .replace("Heilongjiang Province", "黑龙江")
                        .replace("Yunnan Province", "云南")
                    val value = group(2)
                        .replace("BLOCKED", "被封锁")
                    map[key] = value
                }
            }
        return map
    }

    private fun initGFWList() {
        val mirrors = arrayOf(
            "https://raw.githubusercontent.com/gfwlist/gfwlist/master/gfwlist.txt",//github main
            "https://pagure.io/gfwlist/raw/master/f/gfwlist.txt",
            "http://repo.or.cz/gfwlist.git/blob_plain/HEAD:/gfwlist.txt",
            "https://bitbucket.org/gfwlist/gfwlist/raw/HEAD/gfwlist.txt",
            "https://gitlab.com/gfwlist/gfwlist/raw/master/gfwlist.txt",
            "https://git.tuxfamily.org/gfwlist/gfwlist.git/plain/gfwlist.txt"
        )

        val base64: String
        val s: String
        try {
            base64 = HttpUtil(mirrors.random()).getString().replace("\n", "")
            s = String(Base64.getDecoder().decode(base64))
        } catch (e: Exception) {
            logger.warning("GFWList更新失败:${e.message}\n重试中...")
            initGFWList()
            return
        }
        val inp = ByteArrayInputStream(s.toByteArray())

        val reader = BufferedReader(InputStreamReader(inp))
        val gfwList = ArrayList<String>()
        reader.forEachLine {
            if (it.isEmpty()) return@forEachLine
            val domain = it.run {
                val s: String = when {
                    startsWith("||") -> it.substring(2)
                    startsWith("|") -> it.substring(1)
                        .replace("http://", "")
                        .replace("https://", "")
                    startsWith(".") -> it.substring(1)
                    startsWith("[")
                            || startsWith("!")
                            || startsWith("@@")
                    -> return@forEachLine
                    else -> if ("." in it) it else return@forEachLine
                }
                if ("/" in s) s.substring(0, s.indexOf("/"))
                else s
            }
            gfwList.add(domain)
        }
        this.gfwList = gfwList.toTypedArray()
        logger.info("GFWList更新已完成")
    }

    fun startTask() = CoroutineScope(Dispatchers.IO).launch {
        while (true) {
            val interval = (Config.gfwListUpdateInterval * 1000)
            val delay = if(Config.gfwListUpdateInterval > 0) interval else 10_000
            if (Config.gfwListUpdateInterval > 0) initGFWList()
            delay(delay)
        }
    }

    fun listCheck(domain: String): Boolean {
        if (gfwList == null) initGFWList()
        var b = false
        gfwList?.forEach {
            if (it.isNotEmpty() && it in domain) b = true
        }
        return b
    }

    fun localTest(address: String, port: Int) = try {
        Socket().connect(InetSocketAddress(address, port))
        true
    } catch (e: IOException) {
        false
    }

    fun proxyTest(address: String, port: Int) = try {
        val proxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress(Config.proxyAddress, Config.proxyPort))
        Socket(proxy).connect(InetSocketAddress(address, port))
        true
    } catch (e: IOException) {
        false
    }
}