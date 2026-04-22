package com.jianjiao.duoduo;

import static com.jianjiao.duoduo.jj.TAG;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import de.robv.android.xposed.XposedBridge;

public class NativeSocketServer {
    // 单例，避免重复启动
    private static NativeSocketServer instance;
    // 监听端口
    private static final int PORT = 8080;
    private ServerSocket serverSocket;
    // 服务运行状态
    private boolean isRunning = false;

    // 单例获取
    public static NativeSocketServer getInstance() {
        if (instance == null) {
            instance = new NativeSocketServer();
        }
        return instance;
    }

    // 启动服务器（子线程运行，防止阻塞）
    public void start() {
        if (isRunning) return;
        new Thread(() -> {
            try {
                // 1. 创建 ServerSocket 绑定端口
                serverSocket = new ServerSocket(PORT);
                isRunning = true;
                XposedBridge.log("✅ 原生Socket服务启动成功，端口：" + PORT);

                // 2. 循环监听客户端连接（阻塞等待）
                while (isRunning) {
                    // 接收新连接
                    Socket clientSocket = serverSocket.accept();
                    XposedBridge.log("🎯 收到客户端连接：" + clientSocket.getInetAddress());

                    // 3. 子线程处理每个客户端请求（并发支持）
                    new Thread(() -> handleClientRequest(clientSocket)).start();
                }
            } catch (IOException e) {
                XposedBridge.log("❌ Socket服务启动失败：" + e.getMessage());
                isRunning = false;
            }
        }).start();
    }

    // 处理客户端 HTTP 请求（核心：解析协议 + 响应）
    private void handleClientRequest(Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             OutputStream out = socket.getOutputStream()) {

            // ------------- 1. 解析 HTTP 请求头 -------------
            String requestLine = in.readLine(); // 读取第一行：GET /call?data=test HTTP/1.1
            if (requestLine == null || requestLine.isEmpty()) return;

            XposedBridge.log("📩 请求行：" + requestLine);
            // 解析请求方法、URI、协议
            String[] requestParts = requestLine.split(" ");
            String method = requestParts[0]; // GET/POST
            String uri = requestParts[1];    // 接口路径

            // ------------- 2. 解析 URI 参数 -------------
            Map<String, String> params = parseUrlParams(uri);

            // ------------- 3. 处理业务接口 -------------
            String responseContent = "";
            String contentType = "text/plain;charset=utf-8";

            // 测试接口
            if (uri.startsWith("/setReplaceId")) {
                String id = params.get("id");
                if (id != null) {
                    FloatViewManager.setEt(id);
                }
                responseContent = "true";
                /*responseContent = "✅ 原生Socket服务响应成功\n"
                        + "请求方法：" + method + "\n"
                        + "请求参数：" + params;*/
            } else if (uri.startsWith("/fab")) {
                String id = params.get("id");
                if (id != null) {
                    FloatViewManager.setEt("");
                }
                responseContent = "true";
                jj.myReq1(id);
            } else if (uri.startsWith("/favorite")) {
                String id = params.get("id");
                if (id != null) {
                    jj.putFavorite(id);
                }
                responseContent = "true";
            }
            // 外部调用接口（核心）
            else if (uri.startsWith("/getInfo")) {
                String data = params.getOrDefault("data", "无参数");
                //responseContent = "{\"code\":200,\"msg\":\"原生Socket接收调用成功\",\"data\":\"" + data + "\"}";
                contentType = "application/json;charset=utf-8";
                if (FloatViewManager.skuids != null) {
                    responseContent = FloatViewManager.skuids.toString();
                } else {
                    responseContent = null;
                }
                // ======================
                // 在这里写 Xposed 逻辑！
                // Hook 目标APP、修改参数、调用系统方法
                // ======================
                XposedBridge.log("📥 外部调用参数：" + data);
            } else if (uri.startsWith("/getsku")) {
                String id = params.get("id");
                if (id != null) {
                    jj.getSku(id);
                    try {
                        // 延时 2 秒（单位：毫秒）
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        // 必须捕获中断异常
                        e.printStackTrace();
                    }
                    Log.d(TAG, "返回: " + FloatViewManager.skuInfo.toString());
                    responseContent = FloatViewManager.skuInfo.toString();
                }
            }

            // 404
            else {
                responseContent = "❌ 接口不存在";
            }

            // ------------- 4. 构造标准 HTTP 响应（必须符合协议！）-------------
            String httpResponse = "HTTP/1.1 200 OK\r\n"
                    + "Content-Type: " + contentType + "\r\n"
                    + "Content-Length: " + responseContent.getBytes().length + "\r\n"
                    + "Connection: close\r\n"
                    + "\r\n" // 空行分隔响应头和响应体
                    + responseContent;

            // 发送响应
            out.write(httpResponse.getBytes());
            out.flush();

        } catch (Exception e) {
            XposedBridge.log("❌ 处理请求失败：" + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                // 忽略关闭异常
            }
        }
    }

    // 解析 URL 中的参数（?key=value&a=b）
    private Map<String, String> parseUrlParams(String uri) {
        Map<String, String> params = new HashMap<>();
        int paramIndex = uri.indexOf("?");
        if (paramIndex == -1) return params;

        String paramStr = uri.substring(paramIndex + 1);
        String[] keyValues = paramStr.split("&");
        for (String kv : keyValues) {
            String[] pair = kv.split("=");
            if (pair.length == 2) {
                params.put(pair[0], pair[1]);
            }
        }
        return params;
    }

    // 停止服务
    public void stop() {
        try {
            isRunning = false;
            if (serverSocket != null) serverSocket.close();
            XposedBridge.log("🛑 Socket服务已停止");
        } catch (IOException e) {
            XposedBridge.log("❌ 停止服务失败：" + e.getMessage());
        }
    }
}