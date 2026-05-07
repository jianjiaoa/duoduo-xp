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
import de.robv.android.xposed.XposedBridge;
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

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        // 1. 只 Hook 目标应用，避免影响其他应用
        if (!lpparam.packageName.equals("com.base.app")) {
            return;
        }

        XposedBridge.log("[许昌定位] 开始 Hook 应用: " + lpparam.packageName);

        // 2. Hook 高德定位类 AMapLocation 的所有 getter 方法
        final Class<?> aMapLocationClass = XposedHelpers.findClass("com.amap.api.location.AMapLocation", lpparam.classLoader);

        // --- 核心经纬度 ---
        XposedHelpers.findAndHookMethod(aMapLocationClass, "getLatitude", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                param.setResult(34.03);
            }
        });

        XposedHelpers.findAndHookMethod(aMapLocationClass, "getLongitude", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                param.setResult(113.85);
            }
        });

        // --- 许昌市地址信息 ---
        XposedHelpers.findAndHookMethod(aMapLocationClass, "getCity", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                param.setResult("许昌市");
            }
        });

        XposedHelpers.findAndHookMethod(aMapLocationClass, "getProvince", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                param.setResult("河南省");
            }
        });

        XposedHelpers.findAndHookMethod(aMapLocationClass, "getDistrict", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                param.setResult("魏都区");
            }
        });

        XposedHelpers.findAndHookMethod(aMapLocationClass, "getAddress", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                param.setResult("河南省许昌市魏都区建安大道东段");
            }
        });

        XposedHelpers.findAndHookMethod(aMapLocationClass, "getCityCode", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                param.setResult("0374");
            }
        });

        XposedHelpers.findAndHookMethod(aMapLocationClass, "getAdCode", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                param.setResult("411002");
            }
        });

        // --- 其他辅助字段 ---
        XposedHelpers.findAndHookMethod(aMapLocationClass, "getAccuracy", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                param.setResult(5.0f); // 注意：精度是 float 类型
            }
        });

        XposedHelpers.findAndHookMethod(aMapLocationClass, "getErrorCode", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                param.setResult(0);
            }
        });

        XposedHelpers.findAndHookMethod(aMapLocationClass, "getErrorInfo", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                param.setResult("success");
            }
        });

        XposedHelpers.findAndHookMethod(aMapLocationClass, "getCoordType", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                param.setResult("GCJ02");
            }
        });

        XposedHelpers.findAndHookMethod(aMapLocationClass, "getTime", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                param.setResult(System.currentTimeMillis());
            }
        });

        XposedBridge.log("[许昌定位] Hook 完成，已锁定为许昌市魏都区");
    }
}