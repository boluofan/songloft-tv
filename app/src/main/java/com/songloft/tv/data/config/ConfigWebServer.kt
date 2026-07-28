package com.songloft.tv.data.config

import fi.iki.elonen.NanoHTTPD
import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * 局域网 Web 服务：手机扫码打开页签页面，
 * 「登录配置」页签提交服务器地址/账号/密码回电视端登录，
 * 「搜索」页签提交关键字触发电视端搜索。
 */
class ConfigWebServer(
    port: Int,
    private val onConfig: ((server: String, username: String, password: String) -> Unit)? = null,
    private val onSearch: ((keyword: String) -> Unit)? = null
) : NanoHTTPD(port) {

    override fun serve(session: IHTTPSession): Response {
        if (session.method == Method.POST && session.uri == "/submit") {
            session.parseBody(HashMap())
            val onConfig = onConfig
                ?: return html(Response.Status.BAD_REQUEST, resultPage("配置失败", "电视端当前不在登录页，无法提交配置。"))
            val params = session.parameters
            val server = params["server"]?.firstOrNull()?.trim().orEmpty()
            val username = params["username"]?.firstOrNull()?.trim().orEmpty()
            val password = params["password"]?.firstOrNull().orEmpty()
            return if (server.isBlank() || username.isBlank() || password.isBlank()) {
                html(Response.Status.BAD_REQUEST, resultPage("配置失败", "请完整填写服务器地址、账号和密码。"))
            } else {
                onConfig(server, username, password)
                html(Response.Status.OK, resultPage("已提交", "电视端正在登录，请查看电视屏幕。"))
            }
        }
        if (session.method == Method.POST && session.uri == "/search") {
            session.parseBody(HashMap())
            val onSearch = onSearch
                ?: return text(Response.Status.BAD_REQUEST, "电视端当前不在搜索页，无法搜索。")
            val keyword = session.parameters["keyword"]?.firstOrNull()?.trim().orEmpty()
            return if (keyword.isBlank()) {
                text(Response.Status.BAD_REQUEST, "请输入搜索关键字。")
            } else {
                onSearch(keyword)
                text(Response.Status.OK, "已发送，电视端正在搜索。")
            }
        }
        return html(Response.Status.OK, PAGE)
    }

    private fun html(status: Response.Status, content: String): Response =
        newFixedLengthResponse(status, "text/html; charset=utf-8", content)

    private fun text(status: Response.Status, content: String): Response =
        newFixedLengthResponse(status, "text/plain; charset=utf-8", content)

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

        private val PAGE = """
            <!DOCTYPE html><html lang="zh"><head><meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <title>Songloft TV</title>
            <style>
            body{font-family:sans-serif;background:#111827;color:#eee;margin:0;padding:24px}
            h2{color:#8fb0e8;text-align:center}
            .tabs{display:flex;margin-bottom:8px;border-bottom:1px solid #374151}
            .tab{flex:1;padding:12px;font-size:16px;text-align:center;color:#bbb;
            background:none;border:none;border-bottom:2px solid transparent}
            .tab.active{color:#8fb0e8;border-bottom-color:#415F91;font-weight:bold}
            .panel{display:none}
            .panel.active{display:block}
            label{display:block;margin:16px 0 6px;font-size:14px;color:#bbb}
            input{width:100%;box-sizing:border-box;padding:12px;font-size:16px;
            border:1px solid #374151;border-radius:8px;background:#1f2937;color:#eee}
            button.submit{width:100%;margin-top:24px;padding:14px;font-size:16px;font-weight:bold;
            border:none;border-radius:8px;background:#415F91;color:#fff}
            #searchStatus{margin-top:16px;font-size:14px;text-align:center;color:#8fb0e8;min-height:20px}
            </style></head><body>
            <h2>Songloft TV</h2>
            <div class="tabs">
              <button class="tab" id="tabConfig" onclick="showTab('config')">登录配置</button>
              <button class="tab" id="tabSearch" onclick="showTab('search')">搜索</button>
            </div>
            <div class="panel" id="panelConfig">
              <form method="post" action="/submit">
                <label>服务器地址</label>
                <input name="server" type="url" placeholder="http://192.168.1.100:58091" required>
                <label>账号</label>
                <input name="username" type="text" placeholder="admin" required>
                <label>密码</label>
                <input name="password" type="password" placeholder="输入密码" required>
                <button class="submit" type="submit">提交到电视</button>
              </form>
            </div>
            <div class="panel" id="panelSearch">
              <form id="searchForm">
                <label>搜索关键字</label>
                <input name="keyword" id="keyword" type="text" placeholder="输入歌曲、歌手或专辑" required>
                <button class="submit" type="submit">搜索</button>
              </form>
              <div id="searchStatus"></div>
            </div>
            <script>
            function showTab(name){
              document.getElementById('tabConfig').classList.toggle('active',name==='config');
              document.getElementById('tabSearch').classList.toggle('active',name==='search');
              document.getElementById('panelConfig').classList.toggle('active',name==='config');
              document.getElementById('panelSearch').classList.toggle('active',name==='search');
            }
            showTab(location.hash==='#search'?'search':'config');
            document.getElementById('searchForm').addEventListener('submit',function(e){
              e.preventDefault();
              var status=document.getElementById('searchStatus');
              var keyword=document.getElementById('keyword').value.trim();
              if(!keyword){status.textContent='请输入搜索关键字';return;}
              status.textContent='发送中...';
              fetch('/search',{method:'POST',
                headers:{'Content-Type':'application/x-www-form-urlencoded'},
                body:'keyword='+encodeURIComponent(keyword)})
                .then(function(r){return r.text().then(function(t){
                  status.textContent=t;
                  status.style.color=r.ok?'#8fb0e8':'#f87171';
                });})
                .catch(function(){status.textContent='发送失败，请确认电视端仍在搜索页';
                  status.style.color='#f87171';});
            });
            </script>
            </body></html>
        """.trimIndent()
    }
}
