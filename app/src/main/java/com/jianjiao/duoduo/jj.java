package com.jianjiao.duoduo;

import static com.jianjiao.duoduo.ImageViewManager.postImage;
import static com.jianjiao.duoduo.ImageViewManager.removeImage;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class jj implements IXposedHookLoadPackage {
    public static Toast sToast;
    public static int count = 0;
    private static final MediaType reqhead = MediaType.parse("application/json; charset=utf-8");
    public static HashMap taskMap = new FixedSizeLinkedHashMap<String, String>(100, true);
    public static HashMap imageMap = new FixedSizeLinkedHashMap<String, String>(20, true);
    public static String userId = "jianjiao";
    public static ClassLoader classLoader;
    public static Context context;
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
    public static Activity activity;
    public static Handler mainHandler;
    public static String TAG = "jianjiao";
    public static Map<String, JSONObject> listdata = new FixedSizeLinkedHashMap<>(100, true);

    public static void showToast(final String message) {
        if (mainHandler == null) {
            try {
                mainHandler = new Handler(Looper.getMainLooper());
            } catch (Exception e) {
                Log.d(TAG, "创建Handler失败: " + e.getMessage());
                Log.d(TAG, "创建Handler失败: " + e.getMessage());
                return;
            }
        }
        mainHandler.post(new Runnable() {
            @Override
            public final void run() {
                try {
                    // 1. 如果上一个 Toast 还在显示，先关闭它
                    if (sToast != null) {
                        sToast.cancel();
                    }
                    if (context != null) {
                        sToast = Toast.makeText(context, message, 0);
                        sToast.show();
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
//                            Log.d(TAG, "请求: " + arg0);
                            JSONObject data = new JSONObject(arg0);
                            if (data.has("goods_list")) {
                                //店铺 商品列表
                                JSONArray goodsList = data.getJSONArray("goods_list");
                                if (goodsList == null) {
                                    JSONObject datares = data.getJSONObject("data");
                                    goodsList = datares.getJSONArray("goods_list");
                                }
                                Log.d(TAG, "获取到商品列表1: " + goodsList.length() + "|" + goodsList);
                                showToast("获取到: " + goodsList.length() + " 个商品");
                                ArrayList<String> ids = new ArrayList<>();
                                for (int i = 0; i < goodsList.length(); i++) {
                                    JSONObject item = goodsList.getJSONObject(i);
                                    if (item.has("goods_id") && item.has("goods_name")) {
                                        String goodsId = item.getString("goods_id");
                                        String goods_name = item.getString("goods_name");
                                        Log.d(TAG, "获取到商品: " + goodsId + "|" + goods_name);
                                        //保存此条数据信息
                                        listdata.put(goodsId, item);
                                        ids.add(goodsId);
                                    }
                                }
                                Log.d(TAG, "ids: " + listdata.toString());
                                if (ids.size() > 0) {
                                    zhuangku(ids);
                                }
                            } else if (data.has("data")) {
                                JSONObject datares = data.getJSONObject("data");
                                JSONArray goodsList = datares.getJSONArray("goods_list");
                                Log.d(TAG, "获取到商品列表1: " + goodsList.length() + "|" + goodsList);
                                showToast("获取到: " + goodsList.length() + " 个商品");
                                ArrayList<String> ids = new ArrayList<>();
                                for (int i = 0; i < goodsList.length(); i++) {
                                    JSONObject item = goodsList.getJSONObject(i);
                                    if (item.has("goods_id") && item.has("goods_name")) {
                                        //搜索页面下拉
                                        String goodsId = item.getString("goods_id");
                                        String goods_name = item.getString("goods_name");
                                        Log.d(TAG, "获取到商品: " + goodsId + "|" + goods_name);
                                        //保存此条数据信息
                                        listdata.put(goodsId, item);
                                        ids.add(goodsId);
                                    } else if (item.has("data")) {
                                        //适配首页
                                        JSONObject goods_item = item.getJSONObject("data");
                                        String goodsId = goods_item.getString("goods_id");
                                        String goods_name = goods_item.getString("goods_name");
                                        Log.d(TAG, "获取到商品: " + goodsId + "|" + goods_name);
                                        //保存此条数据信息
                                        listdata.put(goodsId, goods_item);
                                        ids.add(goodsId);
                                    }
                                }
                                Log.d(TAG, "ids: " + listdata.toString());
                                if (ids.size() > 0) {
                                    zhuangku(ids);
                                }
                            } else if (data.has("list")) {
                                JSONArray goodsList = data.getJSONArray("list");
                                if (goodsList.length() == 0) {
                                    return;
                                }
                                Log.d(TAG, "获取到商品列表_fl1: " + goodsList.length() + "|" + goodsList);
                                showToast("获取到: " + goodsList.length() + " 个商品");
                                ArrayList<String> ids = new ArrayList<>();
                                for (int i = 0; i < goodsList.length(); i++) {
                                    JSONObject item = goodsList.getJSONObject(i);
                                    if (item.has("goods_id") && item.has("goods_name")) {
                                        String goodsId = item.getString("goods_id");
                                        String goods_name = item.getString("goods_name");
                                        Log.d(TAG, "获取到商品: " + goodsId + "|" + goods_name);
                                        //保存此条数据信息
                                        listdata.put(goodsId, item);
                                        ids.add(goodsId);
                                    }
                                }
                                if (ids.size() > 0) {
                                    zhuangku(ids);
                                }
                            } else if (data.has("items")) {
                                JSONArray goodsList = data.getJSONArray("items");
                                if (goodsList.length() == 0) {
                                    return;
                                }
                                ArrayList<String> ids = new ArrayList<>();
                                for (int i = 0; i < goodsList.length(); i++) {
                                    try { // 循环内部再加一层防护
                                        JSONObject item = goodsList.getJSONObject(i);
                                        if (!item.has("item_data")) {
                                            return;
                                        }
                                        JSONObject item_data = item.getJSONObject("item_data");
                                        JSONObject goods_model = item_data.getJSONObject("goods_model");
                                        if (goods_model.has("goods_id") && goods_model.has("goods_name")) {
                                            String goodsId = goods_model.getString("goods_id");
                                            String goods_name = goods_model.getString("goods_name");
                                            Log.d(TAG, "获取到商品: " + goodsId + "|" + goods_name);
                                            //保存此条数据信息
                                            listdata.put(goodsId, goods_model);
                                            ids.add(goodsId);
                                        }
                                    } catch (Exception e) {
                                        // 单条解析失败，不影响整体
                                    }
                                }
                                if (ids.size() > 0) {
                                    Log.d(TAG, "获取到商品列表_sousuo: " + ids.size() + "|" + goodsList);
                                    showToast("获取到: " + ids.size() + " 个商品");
                                    zhuangku(ids);
                                }
                            } else if (data.has("server_time") && data.has("goods") && data.has("price")) {
                                Log.d(TAG, "获取渲染数据: " + arg0.length() + " 字符," + "|" + arg0);
                                JSONObject goods = data.getJSONObject("goods");
                                String goodsId = goods.getString("goods_id");
                                int status = goods.getInt("status");
                                if (status != 5) {
                                    tijiao(goodsId, data);
                                } else {
                                    showToast("账号不符合浏览需求，请更换账号");
                                }
                            } else {
                                Log.d("jianjiao_其他数据", "_: " + arg0.length() + " 字符," + "|" + arg0);
                            }
                        } catch (Exception e) {
                        }
                    }).start();
                } catch (Exception e) {
                }
            }
        });
    }

    public static void zhuangku(ArrayList<String> ids) {
        showToast(ids.size() + "--商品已加载," + "搜索中...");
        String responseStr = "";
        try {
            JSONObject requestData = new JSONObject("{}");
            JSONArray jsonArray = new JSONArray(ids);
            requestData.put("goodsIds", jsonArray);
            requestData.put("userId", userId);
            // 构建请求体
            String requestBody = requestData.toString();
            Log.d(TAG, " 请求数据: " + requestBody);

            // 发送请求
            RequestBody body = RequestBody.create(reqhead, requestBody);
            Request request = new Request.Builder().url("http://154.222.16.193:8080/zkget").post(body).build();
            Response response = client.newCall(request).execute();
            responseStr = response.body().string();
            Log.d(TAG, " 搜索结果: " + responseStr);
            response.close();

            // 处理响应
            JSONObject result = new JSONObject(responseStr);
            JSONArray tasks = result.getJSONArray("data");
            if (tasks.length() > 0) {
                for (int i = 0; i < tasks.length(); i++) {
                    JSONObject task = tasks.getJSONObject(i);
                    String goodsId = task.getString("goodsId");
                    String taskId = task.getString("taskId");
                    taskMap.put(goodsId, taskId);
                    Log.d(TAG, " 撞到的id: " + goodsId + ",任务id为：" + taskId);
                    // 获取商品信息
                    String goodsName = "";
                    String salesTip = "";
                    String hd_thumb_url = "";
                    JSONObject goodsInfo = listdata.get(goodsId);
                    if (goodsInfo != null) {
                        goodsName = goodsInfo.getString("goods_name");
                        salesTip = goodsInfo.getString("sales_tip");
                        hd_thumb_url = goodsInfo.getString("hd_thumb_url");
                    }
                    Log.d(TAG, " 撞到的id: " + goodsId + ",商品名称为:" + goodsName);
                    showToast("请点击名称为:" + goodsName + "|" + salesTip + "  ");
                    imageMap.put(goodsId, hd_thumb_url);
                    //添加图片
                    postImage(hd_thumb_url);
                }
            } else {
                showToast("无匹配商品");
            }
        } catch (Exception e) {
            Log.e(TAG, " 撞库失败: " + e.getMessage());
        }
    }

    public static void tijiao(String goodsId, JSONObject goodsData) {
        try {
            JSONObject goods = goodsData.getJSONObject("goods");
            int status = goods.getInt("status");
            if (status == 5) {
                showToast("账号不符合浏览需求，请更换账号");
                return;
            }
            if (taskMap.containsKey(goodsId)) {
                // 向后兼容：使用旧的任务系统
                String taskId = taskMap.get(goodsId).toString();
                showToast("开始提交数据: goodsId:" + goodsId + ",taskId:" + taskId);
                String imageUrl = (String) imageMap.get(goodsId);
                Log.d(TAG, "准备删除图片，goodsId: " + goodsId + ", imageUrl: " + imageUrl);
                removeImage(imageUrl);
                imageMap.remove(goodsId);
                taskMap.remove(goodsId);
                submitLegacyTask(goodsId, taskId, goodsData);
            } else {
                Log.d(TAG, "tijiao: 任务列表里面没有此Id");
                showToast("任务不存在");
            }
        } catch (Exception e) {
            Log.d(TAG, "tijiao: 提交数据报错" + e);
        }
    }

    public static void submitLegacyTask(String goodsId, String taskId, JSONObject goodsdata) {
        new Thread(() -> {
            try {
                JSONObject data = new JSONObject();
                data.put("taskId", taskId);
                data.put("userId", userId);
                data.put("data", goodsdata);
                Log.d(TAG, "构建提交数据: " + data);
                RequestBody body = RequestBody.create(reqhead, data.toString());
                Request request = new Request.Builder()
                        .url("http://154.222.16.193:8080/zkupload")
                        .post(body).build();
                Response response = client.newCall(request).execute();
                String str = response.body().string();
                showToast("浏览结果: " + str);
                response.close();
                JSONObject res = new JSONObject(str);
                String code = res.getString("code");
                String msg = res.getString("msg");
                if (code.equals("0")) {
                    count = count + 1;
                    showToast("浏览成功,已浏览:" + count);
                } else {
                    showToast("失败:" + msg);
                }
            } catch (Exception e) {
                Log.e(TAG, "兼容模式提交失败: " + e.getMessage());
                showToast("兼容模式提交失败: " + e.getMessage());
            }
        }).start();
    }
}