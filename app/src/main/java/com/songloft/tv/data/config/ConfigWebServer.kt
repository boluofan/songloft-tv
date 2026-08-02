package com.songloft.tv.data.config

import fi.iki.elonen.NanoHTTPD
import java.io.File
import java.io.FileInputStream
import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * 局域网 Web 服务：手机扫码打开页签页面，
 * 「登录配置」页签提交服务器地址/账号/密码回电视端登录，
 * 「搜索」页签提交关键字触发电视端搜索，
 * 「日志」页签列出电视端导出的日志文件并支持下载。
 */
class ConfigWebServer(
    port: Int,
    private val onConfig: ((server: String, username: String, password: String) -> Unit)? = null,
    private val onSearch: ((keyword: String) -> Unit)? = null,
    private val logsDir: File? = null
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
        if (session.method == Method.GET && session.uri == "/logs") {
            val files = logsDir?.listFiles { f -> f.isFile }
                ?.sortedByDescending { it.lastModified() }
                .orEmpty()
            val json = files.joinToString(",", "[", "]") {
                """{"name":"${it.name}","size":${it.length()},"mtime":${it.lastModified()}}"""
            }
            return newFixedLengthResponse(Response.Status.OK, "application/json; charset=utf-8", json)
        }
        if (session.method == Method.GET && session.uri == "/logs/download") {
            val name = session.parameters["name"]?.firstOrNull().orEmpty()
            if (logsDir == null || name.isBlank() ||
                name.contains('/') || name.contains('\\') || name.contains("..")
            ) {
                return text(Response.Status.BAD_REQUEST, "无效的文件名。")
            }
            val file = File(logsDir, name)
            if (!file.isFile) return text(Response.Status.NOT_FOUND, "文件不存在。")
            val response = newFixedLengthResponse(
                Response.Status.OK, "text/plain; charset=utf-8", FileInputStream(file), file.length()
            )
            response.addHeader("Content-Disposition", "attachment; filename=\"$name\"")
            return response
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
            .pw-wrap{position:relative}
            .pw-wrap input{padding-right:44px}
            .pw-toggle{position:absolute;right:6px;top:50%;transform:translateY(-50%);
            background:none;border:none;cursor:pointer;padding:8px;display:flex;
            align-items:center;justify-content:center;color:#9ca3af}
            .pw-toggle svg{width:20px;height:20px;display:block}
            button.submit{width:100%;margin-top:24px;padding:14px;font-size:16px;font-weight:bold;
            border:none;border-radius:8px;background:#415F91;color:#fff}
            #searchStatus{margin-top:16px;font-size:14px;text-align:center;color:#8fb0e8;min-height:20px}
            #logList{margin-top:16px}
            #logList .hint{font-size:14px;text-align:center;color:#6b7280}
            #logList a{display:block;padding:12px;margin-bottom:8px;font-size:14px;
            border:1px solid #374151;border-radius:8px;background:#1f2937;color:#8fb0e8;
            text-decoration:none;word-break:break-all}
            #logList a span{color:#6b7280;font-size:12px;margin-left:8px}
            .feedback{display:block;margin-top:32px;text-align:center;font-size:13px;color:#6b7280}
            .feedback a{color:#8fb0e8;text-decoration:none}
            </style></head><body>
            <h2>Songloft TV</h2>
            <div class="tabs">
              <button class="tab" id="tabConfig" onclick="showTab('config')">登录配置</button>
              <button class="tab" id="tabSearch" onclick="showTab('search')">搜索</button>
              <button class="tab" id="tabLogs" onclick="showTab('logs')">日志</button>
            </div>
            <div class="panel" id="panelConfig">
              <form method="post" action="/submit">
                <label>服务器地址</label>
                <input name="server" type="url" placeholder="http://192.168.1.100:58091" required>
                <label>账号</label>
                <input name="username" type="text" placeholder="admin" required>
                <label>密码</label>
                <div class="pw-wrap">
                  <input id="pw" name="password" type="password" placeholder="输入密码" required>
                  <button type="button" class="pw-toggle" id="pwToggle" onclick="togglePw()" aria-label="显示密码">
                    <svg id="pwEye" viewBox="0 0 24 24" fill="currentColor"><path d="M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z"/></svg>
                    <svg id="pwEyeOff" viewBox="0 0 24 24" fill="currentColor" style="display:none"><path d="M12 7c2.76 0 5 2.24 5 5 0 .65-.13 1.26-.36 1.83l2.92 2.92c1.51-1.26 2.7-2.89 3.43-4.75-1.73-4.39-6-7.5-11-7.5-1.4 0-2.74.25-3.98.7l2.16 2.16C10.74 7.13 11.35 7 12 7zM2 4.27l2.28 2.28.46.46C3.08 8.3 1.78 10.02 1 12c1.73 4.39 6 7.5 11 7.5 1.55 0 3.03-.3 4.38-.84l.42.42L19.73 22 21 20.73 3.27 3 2 4.27zM7.53 9.8l1.55 1.55c-.05.21-.08.43-.08.65 0 1.66 1.34 3 3 3 .22 0 .44-.03.65-.08l1.55 1.55c-.67.33-1.41.53-2.2.53-2.76 0-5-2.24-5-5 0-.79.2-1.53.53-2.2zm4.31-.78l3.15 3.15.02-.16c0-1.66-1.34-3-3-3l-.17.01z"/></svg>
                  </button>
                </div>
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
            <div class="panel" id="panelLogs">
              <div id="logList"><div class="hint">加载中…</div></div>
            </div>
            <div class="feedback">遇到问题？
              <a href="https://github.com/boluofan/songloft-tv/issues" target="_blank" rel="noopener">问题反馈</a>
            </div>
            <script>
            function showTab(name){
              ['Config','Search','Logs'].forEach(function(t){
                var k=t.toLowerCase();
                document.getElementById('tab'+t).classList.toggle('active',name===k);
                document.getElementById('panel'+t).classList.toggle('active',name===k);
              });
              if(name==='logs')loadLogs();
            }
            function togglePw(){
              var input=document.getElementById('pw');
              var show=input.type==='password';
              input.type=show?'text':'password';
              document.getElementById('pwEye').style.display=show?'none':'';
              document.getElementById('pwEyeOff').style.display=show?'':'none';
              document.getElementById('pwToggle').setAttribute('aria-label',show?'隐藏密码':'显示密码');
            }
            function fmtSize(n){
              if(n>=1048576)return (n/1048576).toFixed(1)+' MB';
              if(n>=1024)return (n/1024).toFixed(1)+' KB';
              return n+' B';
            }
            function fmtTime(t){
              var d=new Date(t),p=function(x){return x<10?'0'+x:x};
              return d.getFullYear()+'-'+p(d.getMonth()+1)+'-'+p(d.getDate())+' '+
                p(d.getHours())+':'+p(d.getMinutes());
            }
            function loadLogs(){
              var el=document.getElementById('logList');
              el.innerHTML='<div class="hint">加载中…</div>';
              fetch('/logs').then(function(r){return r.json();}).then(function(list){
                if(!list.length){
                  el.innerHTML='<div class="hint">暂无日志。请先在电视端 设置 → 日志 → 导出日志</div>';
                  return;
                }
                el.innerHTML=list.map(function(f){
                  return '<a href="/logs/download?name='+encodeURIComponent(f.name)+'" download>'+
                    f.name+'<span>'+fmtSize(f.size)+' · '+fmtTime(f.mtime)+'</span></a>';
                }).join('');
              }).catch(function(){
                el.innerHTML='<div class="hint">加载失败，请刷新重试</div>';
              });
            }
            showTab(location.hash==='#search'?'search':location.hash==='#logs'?'logs':'config');
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
