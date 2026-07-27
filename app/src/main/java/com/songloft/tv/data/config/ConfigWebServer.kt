package com.songloft.tv.data.config

import fi.iki.elonen.NanoHTTPD
import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * 局域网配置服务：手机扫码打开表单页，填写服务器地址/账号/密码后
 * 提交回电视端完成登录。
 */
class ConfigWebServer(
    port: Int,
    private val onSubmit: (server: String, username: String, password: String) -> Unit
) : NanoHTTPD(port) {

    override fun serve(session: IHTTPSession): Response {
        return if (session.method == Method.POST && session.uri == "/submit") {
            session.parseBody(HashMap())
            val params = session.parameters
            val server = params["server"]?.firstOrNull()?.trim().orEmpty()
            val username = params["username"]?.firstOrNull()?.trim().orEmpty()
            val password = params["password"]?.firstOrNull().orEmpty()
            if (server.isBlank() || username.isBlank() || password.isBlank()) {
                html(Response.Status.BAD_REQUEST, resultPage("配置失败", "请完整填写服务器地址、账号和密码。"))
            } else {
                onSubmit(server, username, password)
                html(Response.Status.OK, resultPage("已提交", "电视端正在登录，请查看电视屏幕。"))
            }
        } else {
            html(Response.Status.OK, FORM_PAGE)
        }
    }

    private fun html(status: Response.Status, content: String): Response =
        newFixedLengthResponse(status, "text/html; charset=utf-8", content)

    companion object {

        fun localIpAddress(): String? =
            NetworkInterface.getNetworkInterfaces().asSequence()
                .filter { it.isUp && !it.isLoopback }
                .flatMap { it.inetAddresses.asSequence() }
                .filterIsInstance<Inet4Address>()
                .firstOrNull { it.isSiteLocalAddress }
                ?.hostAddress

        private fun resultPage(title: String, message: String) = """
            <!DOCTYPE html><html lang="zh"><head><meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <title>$title</title>
            <style>body{font-family:sans-serif;background:#111827;color:#eee;
            display:flex;flex-direction:column;align-items:center;justify-content:center;
            min-height:90vh;margin:0;padding:16px}h2{color:#8fb0e8}</style></head>
            <body><h2>$title</h2><p>$message</p></body></html>
        """.trimIndent()

        private val FORM_PAGE = """
            <!DOCTYPE html><html lang="zh"><head><meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <title>Songloft TV 配置</title>
            <style>
            body{font-family:sans-serif;background:#111827;color:#eee;margin:0;padding:24px}
            h2{color:#8fb0e8;text-align:center}
            label{display:block;margin:16px 0 6px;font-size:14px;color:#bbb}
            input{width:100%;box-sizing:border-box;padding:12px;font-size:16px;
            border:1px solid #374151;border-radius:8px;background:#1f2937;color:#eee}
            button{width:100%;margin-top:24px;padding:14px;font-size:16px;font-weight:bold;
            border:none;border-radius:8px;background:#415F91;color:#fff}
            </style></head><body>
            <h2>Songloft TV 配置</h2>
            <form method="post" action="/submit">
              <label>服务器地址</label>
              <input name="server" type="url" placeholder="http://192.168.1.100:58091" required>
              <label>账号</label>
              <input name="username" type="text" placeholder="admin" required>
              <label>密码</label>
              <input name="password" type="password" placeholder="输入密码" required>
              <button type="submit">提交到电视</button>
            </form></body></html>
        """.trimIndent()
    }
}
