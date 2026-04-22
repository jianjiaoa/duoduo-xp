package com.jianjiao.duoduo;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class jj implements IXposedHookLoadPackage {
    public static ClassLoader classLoader;
    public static Context context;
    public static Activity activity;
    public static Handler mainHandler;
    public static String TAG = "jianjiao";

    public void showToast(final String message) {
        if (this.mainHandler == null) {
            try {
                this.mainHandler = new Handler(Looper.getMainLooper());
            } catch (Exception e) {
                Log.d(TAG, "创建Handler失败: " + e.getMessage());
                Log.d(TAG, "创建Handler失败: " + e.getMessage());
                return;
            }
        }
        this.mainHandler.post(new Runnable() {
            @Override
            public final void run() {
                try {
                    if (context != null) {
                        Toast.makeText(context, message, 0).show();
                    } else {
                        Log.d(TAG, "Context为空，无法显示Toast: " + message);
                    }
                } catch (Exception e) {
                    Log.d(TAG, "显示Toast失败: " + e.getMessage());
                }

            }
        });
    }

    @Override // de.robv.android.xposed.IXposedHookLoadPackage
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

        // 判断：如果不是主进程，直接return
        if (!"com.xunmeng.pinduoduo".equals(lpparam.processName)) {
            Log.d(TAG, "子进程p，不执行:" + lpparam.processName);
            return;
        }
        if (!"com.xunmeng.pinduoduo".equals(lpparam.packageName)) {
            Log.d(TAG, "子进程b，不执行:" + lpparam.processName);
            return;
        }
        Log.d(TAG, "主进程启动成功：" + lpparam.packageName + "|" + lpparam.processName);
        classLoader = lpparam.classLoader;

        //Application
        /*XposedHelpers.findAndHookMethod("android.app.PddActivityThread", classLoader, "currentApplication", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                if (context == null) {
                    Application app = (Application) param.getResult();
                    context = app.getApplicationContext();
                    if (context != null && app != null) {
                        Log.d(TAG, "afterHookedMethod222: " + context);
                    }
                }
            }
        });*/
        XposedHelpers.findAndHookMethod(StackTraceElement.class, "getClassName", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                String className = (String) param.getResult();
                if (className != null && className.contains("xposed")) {
                    Log.d(TAG, "afterHookedMethod: " + className);
                    param.setResult("android.os.Handler");
                }
                super.afterHookedMethod(param);
            }
        });
        XposedHelpers.findAndHookMethod("org.json.JSONObject", classLoader, "toString", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                try {
                    String res = (String) param.getResult();
                    JSONObject jsonObject = new JSONObject(res);
                    if (jsonObject.has("publish_data_track_info_map") && jsonObject.has("content_list")) {
                        // 获取你悬浮窗输入的 ID
                        String inputId = FloatViewManager.currentInputId;
                        // 如果用户没输入，就不替换（避免出错）
                        if (inputId == null || inputId.trim().isEmpty()) {
                            return;
                        }
                        // ==============================================
                        // 核心：替换所有 goods_id 为你输入的 ID
                        // ==============================================
                        String newJson = res.replaceAll("\"goods_id\":\\d+", "\"goods_id\":" + inputId);
                        Log.d(TAG, "分享请求！新ID: " + inputId + "|" + res);
                        Log.d(TAG, "分享请求替换后JSON: " + newJson);
                        FloatViewManager.getInstance().clearList();
                        // 把替换后的 JSON 丢回去
                        param.setResult(newJson);
                    } else if (jsonObject.has("coupon_status") && jsonObject.has("goods_id")) {
                        // 获取你悬浮窗输入的 ID
                        String inputId = FloatViewManager.currentInputId;
                        // 如果用户没输入，就不替换（避免出错）
                        if (inputId == null || inputId.trim().isEmpty()) {
                            return;
                        }
                        // ==============================================
                        // 核心：替换所有 goods_id 为你输入的 ID
                        // ==============================================
                        String newJson = res.replaceAll("\"goods_id\":\\d+", "\"goods_id\":" + inputId);
                        Log.d(TAG, "带货收藏！新ID: " + inputId + "|" + res);
                        Log.d(TAG, "带货收藏_替换后JSON: " + newJson);
                        FloatViewManager.getInstance().clearList();
                        // 把替换后的 JSON 丢回去
                        param.setResult(newJson);
                    }
                } catch (Exception e) {
                }
                super.afterHookedMethod(param);
            }
        });
        // ======================
        // 悬浮窗（依赖Activity）
        // ======================
        XposedHelpers.findAndHookMethod("android.app.Activity", lpparam.classLoader, "onResume", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                Activity activity = (Activity) param.thisObject;
                activity.runOnUiThread(() -> {
                    context = activity.getApplication();
//                    NativeSocketServer.getInstance().start();
//                    FloatViewManager.getInstance().attachTo(activity);
                    ImageViewManager.getInstance().attachTo(activity);
                });
            }
        });

        XposedHelpers.findAndHookMethod("com.xunmeng.basiccomponent.titan.api.TitanApiResponse$Builder", classLoader, "bodyBytes", byte[].class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                try {
                    byte[] bArr = (byte[]) param.args[0];
                    if (bArr == null) return;
                    new Thread(() -> {
                        try {
                            String arg0 = new String(bArr, StandardCharsets.UTF_8);
                            Log.d(TAG, "请求: " + arg0);
                            if (arg0.contains("sku_ids")) {
                                Log.d(TAG, "有skuids: " + arg0);
                                JSONObject jsonObject = new JSONObject(arg0);
                                JSONArray goods_list = jsonObject.getJSONArray("goods_list");
                                for (int i = 0; i < goods_list.length(); i++) {
                                    JSONObject item = goods_list.getJSONObject(i);
                                    JSONObject base_goods_info = item.getJSONObject("base_goods_info");
                                    String goodsID = base_goods_info.getString("goods_id");
                                    Log.d(TAG, "beforeHookedMethod: " + goodsID + "|" + FloatViewManager.currentInputId);
                                    FloatViewManager.skuids = base_goods_info;
                                    if (goodsID.equals(FloatViewManager.currentInputId)) {
                                        JSONArray sku_ids = base_goods_info.getJSONArray("sku_ids");
                                        FloatViewManager.getInstance().clearList();
                                        for (int j = 0; j < sku_ids.length(); j++) {
                                            Log.d(TAG, "skuid: " + sku_ids.getString(j));
                                            // 添加一条记录
                                            FloatViewManager.getInstance().addItem(sku_ids.getString(j));
                                        }
                                        break;
                                    }
                                }
                            } else if (arg0.contains("sku_lego_template_info") && arg0.contains("show_sku_selector")) {
                                FloatViewManager.skuInfo = new JSONObject(arg0);
                            }
                        } catch (Exception e) {
                        }
                    }).start();
                } catch (Exception e) {
                }
            }
        });
    }

    /**
     * 主动发送拼小圈（兼容Xposed类对象校验，修复NoSuchMethodError）
     */
    public static void myReq1(String goodsId) {
        // 1. 防御判断：类加载器未初始化直接返回
        if (classLoader == null) {
            Log.d(TAG, "[-] 类加载器未初始化，调用失败");
            return;
        }

        try {
            // 1. 加载需要的类（不变）
            Class<?> Tm1A = XposedHelpers.findClass("tm1.a", classLoader);
            Class<?> Qm1B = XposedHelpers.findClass("qm1.b", classLoader);
            Class<?> JSONObjectClass = XposedHelpers.findClass("org.json.JSONObject", classLoader);
            Class<?> JSONArrayClass = XposedHelpers.findClass("org.json.JSONArray", classLoader);

            // 2. 回调代理（不变）
            Object callback = java.lang.reflect.Proxy.newProxyInstance(classLoader, new Class[]{Qm1B}, (proxy, method, args) -> {
                String methodName = method.getName();
                switch (methodName) {
                    case "K":
                        Log.d(TAG, "[SimpleCallback.K] 成功回调:");
                        Log.d(TAG, "    状态码: " + args[0]);
                        Log.d(TAG, "    响应: " + args[1]);
                        break;
                    case "b":
                        Log.d(TAG, "[SimpleCallback.b] 成功回调(完整):");
                        Log.d(TAG, "    状态码: " + args[0]);
                        Log.d(TAG, "    响应: " + args[1]);
                        break;
                    case "a":
                        Log.d(TAG, "[SimpleCallback.a] 错误回调:");
                        break;
                    case "A":
                        Log.d(TAG, "[SimpleCallback.A] 异常回调:");
                        break;
                }
                return null;
            });

            // 3. 构造请求头、请求体（不变）
            Object headers = XposedHelpers.newInstance(JSONObjectClass);
            callMethod(headers, "put", "Content-Type", "application/json");

            Object body = XposedHelpers.newInstance(JSONObjectClass);
            callMethod(body, "put", "content_type", 12);
            Object contentList = XposedHelpers.newInstance(JSONArrayClass);
            Object item = XposedHelpers.newInstance(JSONObjectClass);
            callMethod(item, "put", "goods_id", goodsId);
            callMethod(item, "put", "tag", 2);
            callMethod(contentList, "put", item);
            callMethod(body, "put", "content_list", contentList);
            callMethod(body, "put", "social_request_id", "test_request_id_" + System.currentTimeMillis());

            Object publishDataTrackInfoMap = XposedHelpers.newInstance(JSONObjectClass);
            callMethod(publishDataTrackInfoMap, "put", "app_version", "8.0.0");
            callMethod(publishDataTrackInfoMap, "put", "click_trace_id", "test_trace_" + System.currentTimeMillis());
            callMethod(publishDataTrackInfoMap, "put", "source", "101");
            callMethod(publishDataTrackInfoMap, "put", "platform", "android");
            callMethod(publishDataTrackInfoMap, "put", "phone_model", "MI 8 Lite");
            callMethod(publishDataTrackInfoMap, "put", "scene", "self_selected_list");
            callMethod(body, "put", "publish_data_track_info_map", publishDataTrackInfoMap);
            callMethod(body, "put", "source", 32);

            // 日志
            Log.d(TAG, "[*] 主动调用 tm1.a.e:");
            Log.d(TAG, "    URL: /api/social/timeline/share");
            Log.d(TAG, "    Body: " + callMethod(body, "toString"));

            // ==============================================
            // 🔥 核心修复：遍历方法匹配（替代findMethodExact，和Frida逻辑一致）
            // ==============================================
            Method targetMethod = null;
            // 遍历tm1.a所有方法，找到 方法名=e + 参数个数=11 的方法
            for (Method method : Tm1A.getDeclaredMethods()) {
                if (method.getName().equals("e") && method.getParameterTypes().length == 11) {
                    targetMethod = method;
                    targetMethod.setAccessible(true); // 突破私有/包私有限制
                    Log.d(TAG, "[+] 成功匹配到 tm1.a.e 方法");
                    break;
                }
            }

            if (targetMethod == null) {
                Log.d(TAG, "[-] 未找到目标方法");
                return;
            }

            // ==============================================
            // 执行调用（兼容静态/非静态，Frida同款逻辑）
            // ==============================================
            Object invokeTarget = Modifier.isStatic(targetMethod.getModifiers()) ? null : XposedHelpers.newInstance(Tm1A);
            targetMethod.invoke(
                    invokeTarget,
                    "/api/social/timeline/share",
                    "POST",
                    false,
                    headers,
                    callMethod(body, "toString"),
                    true,
                    false,
                    10000,
                    "",
                    "",
                    callback
            );

            Log.d(TAG, "[+] 调用已发送，等待回调...");

        } catch (Throwable e) {
            Log.d(TAG, "[-] 主动调用失败: " + e.getMessage());
            Log.d(TAG, "    堆栈: " + android.util.Log.getStackTraceString(e));
        }
    }

    public static void getSku(String goodsId) {
        if (classLoader == null) {
            Log.d(TAG, "[-] 类加载器未初始化，调用失败");
            return;
        }
        try {
            Log.d(TAG, "[*] Requesting SKU render for goods: " + goodsId);

            // 1. 获取 Router
            Class<?> Router = XposedHelpers.findClass("com.xunmeng.router.Router", classLoader);
            Object iRouter = XposedHelpers.callStaticMethod(Router, "build", "AMNetWORK_INTERFACE");

            // 2. 获取 IAMNetwork
            Class<?> IAMNetwork = XposedHelpers.findClass("com.aimi.android.hybrid.action.IAMNetwork", classLoader);
            Object iAMNetwork = XposedHelpers.callMethod(iRouter, "getModuleService", IAMNetwork);

            if (iAMNetwork == null) {
                Log.d(TAG, "[-] Failed to get IAMNetwork instance");
                return;
            }
            Log.d(TAG, "[+] Got IAMNetwork instance");
            Log.d(TAG, "[+] Instance class: " + iAMNetwork.getClass().getName());

            // 3. 强转 AMNetworkImpl
            Class<?> AMNetworkImpl = XposedHelpers.findClass(
                    "com.xunmeng.pinduoduo.network_bridge.impl.AMNetworkImpl",
                    classLoader
            );

            // ====================== 关键修复：匹配 ICommonCallBack 签名 ======================
            Class<?> ICommonCallBack = XposedHelpers.findClass(
                    "com.aimi.android.common.callback.ICommonCallBack",
                    classLoader
            );

            // ====================== 核心修复：100%对齐Frida的JSON格式 ======================
            long clientTime = System.currentTimeMillis();
            String model = android.os.Build.MODEL;

            // 完全复刻Frida的请求体
            JSONObject data = new JSONObject();
            data.put("address_list", new JSONArray()); // ✅ 空JSON数组 (Frida: [])
            data.put("page_sn", "10034");
            data.put("goods_id", goodsId);
            data.put("phone_model", model);
            data.put("page_from", "0");
            data.put("page_version", "7");
            data.put("client_time", String.valueOf(clientTime));
            data.put("pic_w", 0);
            data.put("pic_h", 0);
            data.put("has_pic_url", 0);
            data.put("extend_map", new JSONObject()); // ✅ 空对象
            data.put("refer_page_sn", "10001");
            data.put("_oak_stage", "fav");
            data.put("union_pay_installed", false);

            JSONObject clientLab = new JSONObject();
            clientLab.put("mall_h5_url_preload_enable", "1");
            data.put("client_lab", clientLab);

            data.put("is_sys_minor", 0);
            data.put("system_language", "zh");
            data.put("cached_templates", new JSONArray()); // ✅ 空JSON数组 (Frida: [])


            JSONObject requestBody = new JSONObject();
            requestBody.put("url", "https://api.pinduoduo.com/api/oak/integration/render/sku");
            requestBody.put("method", "POST");
            requestBody.put("timeout", 10000);
            requestBody.put("force_anti_token", false);
            requestBody.put("encode_resp", true);
            requestBody.put("isApiRequest", true);
            requestBody.put("data", data);

            Log.d(TAG, "[+] Request body: " + requestBody.toString());

            // 5. 调用 request 方法
            Method requestMethod = AMNetworkImpl.getMethod("request", JSONObject.class, ICommonCallBack);
            requestMethod.invoke(iAMNetwork, requestBody, null);

            Log.d(TAG, "[+] Request sent successfully");
            Log.d(TAG, "getSku: ");
        } catch (Exception e) {
            Log.d(TAG, "[-] requestSkuRender error: " + e);
            e.printStackTrace();
        }
    }

    // 收藏商品核心方法（对应 Frida 的 putFavorite）
    public static void putFavorite(String goodsId) {
        try {
            Log.d(TAG, "[*] 开始收藏商品: " + goodsId);
            // 1. 获取收藏服务类
            Class<?> FavoriteServiceImpl = XposedHelpers.findClass(
                    "com.xunmeng.pinduoduo.impl.FavoriteServiceImpl",
                    classLoader
            );

            // 2. 创建服务实例
            Object favService = XposedHelpers.newInstance(FavoriteServiceImpl);

            // 3. 创建回调接口
            Class<?> ICommonCallBack = XposedHelpers.findClass(
                    "com.aimi.android.common.callback.ICommonCallBack",
                    classLoader
            );

            // 动态创建回调实现类（对应 Frida 的 registerClass）
            Object myCallback = Proxy.newProxyInstance(
                    classLoader,
                    new Class[]{ICommonCallBack},
                    (proxy, method, args) -> {
                        if ("invoke".equals(method.getName())) {
                            int code = (int) args[0];
                            Object data = args[1];
                            Log.d(TAG, "=== 收藏回调 ===");
                            Log.d(TAG, "putFavorite: ");
                            Log.d(TAG, "code: " + code);
                            Log.d(TAG, "data: " + data);
                            Log.d(TAG, "================");
                        }
                        return null;
                    }
            );

            // 4. 调用 put 方法（先尝试 4 参数，失败再尝试 5 参数）
            try {
                XposedHelpers.callMethod(favService, "put", context, 0, goodsId, myCallback);
                Log.d(TAG, "[+] 4参数收藏请求已发送");
            } catch (Throwable e) {
                // 失败则使用 5 参数（带 HashMap 参数）
                HashMap<String, Object> params = new HashMap<>();
                XposedHelpers.callMethod(favService, "put", context, 0, goodsId, myCallback, params);
                Log.d(TAG, "[+] 5参数收藏请求已发送");
            }

        } catch (Throwable e) {
            Log.d(TAG, "[-] 收藏失败: " + e.getMessage());
        }
    }

    // 简化调用方法
    public static Object callMethod(Object obj, String methodName, Object... args) {
        return XposedHelpers.callMethod(obj, methodName, args);
    }
}