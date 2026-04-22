package com.jianjiao.duoduo;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONObject;

import java.util.ArrayList;

public class FloatViewManager {
    public static JSONObject skuInfo = null;
    public static JSONObject skuids = new JSONObject();

    private static FloatViewManager INSTANCE;
    public static String currentInputId = "";

    private View floatView;
    private View mainContent;
    private View miniIconView;
    private boolean isMinimized = false;

    private float saveX = 100;
    private float saveY = 300;
    private float touchX, touchY;
    private boolean isMoved = false; // 🔥 关键：判断是拖动还是点击

    private ListView listView;
    private ArrayList<String> listData;
    private ArrayAdapter<String> adapter;
    public static Context mContext;
    public static EditText et;

    public static void setEt(String text) {
        ((Activity) mContext).runOnUiThread(() -> {
            et.setText(text);
        });
    }

    public static FloatViewManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new FloatViewManager();
        }
        return INSTANCE;
    }

    private View createFloatView(Context context) {
        mContext = context;

        FrameLayout root = new FrameLayout(context);
        root.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        mainContent = createMainContent(context);
        root.addView(mainContent);

        miniIconView = createMiniIcon(context);
        miniIconView.setVisibility(View.GONE);
        root.addView(miniIconView);

        return root;
    }

    private View createMainContent(Context context) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(8, 6, 24, 6);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF222222);
        bg.setStroke(2, Color.CYAN);
        bg.setCornerRadius(4);
        layout.setBackground(bg);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                dp2px(context, 150),
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        layout.setLayoutParams(lp);

        LinearLayout titleBar = new LinearLayout(context);
        titleBar.setOrientation(LinearLayout.HORIZONTAL);
        titleBar.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvTitle = new TextView(context);
        tvTitle.setText("替换id：");
        tvTitle.setTextColor(Color.WHITE);
        tvTitle.setTextSize(14);
        titleBar.addView(tvTitle);

        TextView btnMin = new TextView(context);
        btnMin.setText("—");
        btnMin.setTextColor(Color.WHITE);
        btnMin.setTextSize(18);
        btnMin.setPadding(8, 0, 0, 0);
        btnMin.setOnClickListener(v -> minimize());

        FrameLayout titleContainer = new FrameLayout(context);
        titleContainer.addView(titleBar);
        FrameLayout.LayoutParams btnLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.RIGHT | Gravity.CENTER_VERTICAL
        );
        titleContainer.addView(btnMin, btnLp);
        layout.addView(titleContainer);

        et = new EditText(context);
        et.setHint("id");
        et.setHintTextColor(0xBBFFFFFF);
        et.setTextColor(Color.WHITE);
        et.setBackgroundColor(0x22FFFFFF);
        et.setTextSize(14);
        et.setSingleLine(true);
        et.setPadding(6, 4, 6, 4);
        et.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentInputId = s.toString().trim();
                clearList();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        layout.addView(et);

        TextView tvSku = new TextView(context);
        tvSku.setText("skuids：");
        tvSku.setTextColor(Color.WHITE);
        tvSku.setTextSize(14);
        layout.addView(tvSku);

        listView = new ListView(context);
        GradientDrawable listBg = new GradientDrawable();
        listBg.setColor(0xFF333333);
        listBg.setCornerRadius(2);
        listView.setBackground(listBg);
        listView.setDivider(null);
        listView.setDividerHeight(0);
        listView.setPadding(0, 0, 0, 0);
        listView.setVerticalScrollBarEnabled(false);

        listData = new ArrayList<>();
        adapter = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, listData) {
            @Override
            public View getView(int pos, View cv, ViewGroup parent) {
                View v = super.getView(pos, cv, parent);
                TextView t = v.findViewById(android.R.id.text1);
                t.setTextSize(14);
                t.setTextColor(Color.WHITE);
                t.setPadding(0, 0, 0, 0);
                v.setLayoutParams(new AbsListView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, 50));
                return v;
            }
        };
        listView.setAdapter(adapter);

        LinearLayout.LayoutParams lvLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp2px(context, 150));
        lvLp.topMargin = 4;
        listView.setLayoutParams(lvLp);
        layout.addView(listView);

        // 主窗口拖动
        layout.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                touchX = event.getRawX() - floatView.getX();
                touchY = event.getRawY() - floatView.getY();
                isMoved = false;
                return true;
            }
            if (event.getAction() == MotionEvent.ACTION_MOVE) {
                isMoved = true;
                saveX = event.getRawX() - touchX;
                saveY = event.getRawY() - touchY;
                floatView.setX(saveX);
                floatView.setY(saveY);
                return true;
            }
            return false;
        });

        return layout;
    }

    // 🔥 核心修复：拖动 + 点击 同时生效
    private View createMiniIcon(Context context) {
        TextView icon = new TextView(context);
        icon.setText("多");
        icon.setTextColor(Color.WHITE);
        icon.setTextSize(16);
        icon.setGravity(Gravity.CENTER);

        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(0xFF222222);
        drawable.setStroke(2, Color.CYAN);
        icon.setBackground(drawable);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                dp2px(context, 40), dp2px(context, 40));
        lp.gravity = Gravity.CENTER;
        icon.setLayoutParams(lp);

        // 🔥 完美触摸逻辑：拖动时拖动，没拖动时点击
        icon.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    touchX = event.getRawX() - floatView.getX();
                    touchY = event.getRawY() - floatView.getY();
                    isMoved = false;
                    return true;

                case MotionEvent.ACTION_MOVE:
                    isMoved = true;
                    saveX = event.getRawX() - touchX;
                    saveY = event.getRawY() - touchY;
                    floatView.setX(saveX);
                    floatView.setY(saveY);
                    return true;

                case MotionEvent.ACTION_UP:
                    // 🔥 关键：如果没有拖动，才触发点击恢复
                    if (!isMoved) {
                        restore();
                    }
                    return true;
            }
            return false;
        });

        return icon;
    }

    private void minimize() {
        if (mContext == null || floatView == null) return;
        isMinimized = true;
        ((Activity) mContext).runOnUiThread(() -> {
            mainContent.setVisibility(View.GONE);
            miniIconView.setVisibility(View.VISIBLE);
            floatView.setLayoutParams(new FrameLayout.LayoutParams(
                    dp2px(mContext, 40), dp2px(mContext, 40)));
        });
    }

    private void restore() {
        if (mContext == null || floatView == null) return;
        isMinimized = false;
        ((Activity) mContext).runOnUiThread(() -> {
            miniIconView.setVisibility(View.GONE);
            mainContent.setVisibility(View.VISIBLE);
            floatView.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
        });
    }

    public void addItem(String text) {
        if (mContext == null || listData == null || adapter == null) return;
        ((Activity) mContext).runOnUiThread(() -> {
            listData.add(text);
            adapter.notifyDataSetChanged();
        });
    }

    public void clearList() {
        if (mContext == null || listData == null || adapter == null) return;
        ((Activity) mContext).runOnUiThread(() -> {
            listData.clear();
            adapter.notifyDataSetChanged();
        });
    }

    public void attachTo(Activity activity) {
        mContext = activity;
        if (floatView == null) {
            floatView = createFloatView(activity);
        }
        ViewGroup oldParent = (ViewGroup) floatView.getParent();
        if (oldParent != null) oldParent.removeView(floatView);

        ViewGroup decor = (ViewGroup) activity.getWindow().getDecorView();
        floatView.setX(saveX);
        floatView.setY(saveY);
        decor.addView(floatView);
    }

    private int dp2px(Context context, float dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
    }
}