package top.cutestar.networkTools

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value

object Config : AutoSavePluginConfig("config") {
    @ValueDescription("设置是否使用代理 在http(s)测墙 生效")
    var proxyEnabled by value(false)

    @ValueDescription("设置代理地址")
    var proxyAddress by value("")

    @ValueDescription("设置代理端口")
    var proxyPort by value(10808)

    @ValueDescription("设置代理类型 可选http,socks 大小写随意")
    var proxyType by value("http")

    @ValueDescription("设置控制台编码，如出现乱码请修改")
    var consoleCharset by value("GBK")

    @ValueDescription("设置web编码，如出现乱码请修改")
    var webCharset by value("UTF-8")

    @ValueDescription("Web连接超时时间(毫秒)")
    var webTimeout:Long by value((10_000).toLong())

    @ValueDescription("DNS命令查询默认服务器")
    var dnsAddress by value("8.8.8.8")

    @ValueDescription("DNS默认查询类型")
    var dnsTypes: MutableSet<String> by value(mutableSetOf("A","AAAA","CNAME","TXT","NS","SOA","MX"))

    @ValueDescription("端口扫描超时时间(毫秒)")
    var nmapTimeout by value(400)

    @ValueDescription("路由追踪 Windows系统命令")
    var tracertWCommand by value("tracert -w 400 -d ${"$"}address")

    @ValueDescription("路由追踪 其他系统命令")
    var tracertOCommand by value("traceroute -w 1 -n -q 1 ${"$"}address")

    @ValueDescription("DoH服务器 cf DoH被封锁 需要代理")
    var dohAddress by value("https://cloudflare-dns.com/dns-query?name=${"$"}s&type=${"$"}type")

    @ValueDescription("nmap端口扫描线程池大小")
    var nmapPoolSize by value(50)

    @ValueDescription("GFWList定时更新间隔(秒) 0为不预加载和自动更新")
    var gfwListUpdateInterval:Long by value((0).toLong())

    @ValueDescription("因qq特性可能会查看不了转发信息 设置成true发送文本信息")
    var noForwardMsg by value(false)

    @ValueDescription("纯文本信息字数限制")
    var textMsgLimit by value(200)
}