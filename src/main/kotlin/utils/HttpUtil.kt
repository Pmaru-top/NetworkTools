package top.cutestar.networkTools.utils

import io.ktor.util.*
import top.cutestar.networkTools.Config
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URL
import java.nio.charset.Charset
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.*

@OptIn(InternalAPI::class)
class HttpUtil(
    url: String,
    headers: MutableMap<String, String> = mutableMapOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/102.0.0.0 Safari/537.36",
        "Connection" to "keep-alive"
    ),
    useProxy: Boolean = true
) {
    private var http: HttpURLConnection? = null
    private var inp: InputStream? = null
    val NOT_VERIFY: HostnameVerifier = HostnameVerifier { _, _ -> true }

    init {
        //OKHttp一直连接泄漏关不掉，还是JDK方法稳定
        val proxy = if (Config.proxyEnabled && useProxy) Proxy(
            Proxy.Type.valueOf(
                Config.proxyType.toUpperCasePreservingASCIIRules()
            ),
            InetSocketAddress(Config.proxyAddress, Config.proxyPort)
        )
        else Proxy.NO_PROXY

        URL(url).let { url ->
            trustAllHosts()
            url.openConnection(proxy).let { conn ->
                http = when (url.protocol.lowercase()) {
                    "https" -> (conn as HttpsURLConnection).apply { hostnameVerifier = NOT_VERIFY }
                    else -> conn as HttpURLConnection
                }.apply {
                    headers.forEach { (k, v) ->
                        addRequestProperty(k, v)
                    }
                }
            }
        }


        inp = try {
            when (http?.inputStream != null) {
                true -> http!!.inputStream
                false -> http!!.errorStream
            }
        } catch (e: Exception) {
            throw IOException("获取流失败")
        }
    }

    fun getInputStream(): InputStream = inp!!

    fun getConnection(): HttpURLConnection = http!!

    fun getBytes(autoClose: Boolean = true): ByteArray {
        val bytes = inp!!.readAllBytes()
        if (autoClose) close()
        return bytes
    }

    fun getString(charset: String = Config.webCharset, autoClose: Boolean = true) =
        String(getBytes(autoClose), Charset.forName(charset))

    fun close() {
        inp?.close()
        http?.disconnect()
    }

    private fun trustAllHosts() {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()

            override fun checkClientTrusted(chain: Array<X509Certificate?>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<X509Certificate?>?, authType: String?) {}
        })
        try {
            val sc: SSLContext = SSLContext.getInstance("TLS")
            sc.init(null, trustAllCerts, SecureRandom())
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.socketFactory)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}