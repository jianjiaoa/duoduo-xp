package com.jianjiao.duoduo;

import android.app.Activity;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.DisplayMetrics;
import android.view.ViewConfiguration;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.net.HttpURLConnection;
import java.net.URL;

public class ImageViewManager {

    private static ImageViewManager INSTANCE;

    private static final int TITLE_HEIGHT_DP = 30;
    private static final int INFO_HEIGHT_DP = 30;
    private static final int MINI_SIZE_DP = 40;
    private static final int CONTENT_PADDING_DP = 12;
    private static final int IMAGE_GAP_DP = 8;
    private static final int IMAGE_CORNER_DP = 10;

    private View floatView;
    private View mainContent;
    private View miniIconView;

    private float saveX = 100;
    private float saveY = 300;
    private float touchX;
    private float touchY;
    private float downRawX;
    private float downRawY;
    private boolean isMoved = false;
    private int touchSlopPx = 0;

    private ListView listView;
    private EditText userIdValueView;
    private boolean isSyncingUserId = false;
    private ArrayList<String> imageItems;
    private ImageGridAdapter adapter;
    public static Context mContext;
    private final Map<String, Bitmap> imageBitmapCache = new HashMap<>();
    private final Set<String> loadingUrls = new HashSet<>();

    public static ImageViewManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ImageViewManager();
        }
        return INSTANCE;
    }

    private View createFloatView(Context context) {
        mContext = context;
        touchSlopPx = ViewConfiguration.get(context).getScaledTouchSlop();

        FrameLayout root = new FrameLayout(context);
        root.setLayoutParams(new FrameLayout.LayoutParams(
                getPanelSizePx(context),
                getPanelTotalHeightPx(context)
        ));

        mainContent = createMainContent(context);
        root.addView(mainContent);

        miniIconView = createMiniIcon(context);
        miniIconView.setVisibility(View.GONE);
        root.addView(miniIconView);

        return root;
    }

    private View createMainContent(Context context) {
        FrameLayout layout = new FrameLayout(context);
        layout.setLayoutParams(new FrameLayout.LayoutParams(
                getPanelSizePx(context),
                getPanelTotalHeightPx(context)
        ));

        LinearLayout bodyLayout = new LinearLayout(context);
        bodyLayout.setOrientation(LinearLayout.VERTICAL);
        bodyLayout.setPadding(
                dp2px(context, CONTENT_PADDING_DP),
                dp2px(context, CONTENT_PADDING_DP),
                dp2px(context, CONTENT_PADDING_DP),
                dp2px(context, CONTENT_PADDING_DP)
        );

        GradientDrawable bodyBg = new GradientDrawable();
        bodyBg.setColor(0xFFF7F1E8);
        bodyBg.setStroke(dp2px(context, 1), 0xFFD8B98A);
        bodyBg.setCornerRadii(new float[]{
                0, 0,
                0, 0,
                dp2px(context, 18), dp2px(context, 18),
                dp2px(context, 18), dp2px(context, 18)
        });
        bodyLayout.setBackground(bodyBg);

        FrameLayout.LayoutParams bodyLp = new FrameLayout.LayoutParams(
                getPanelSizePx(context),
                getPanelSizePx(context)
        );
        bodyLp.topMargin = dp2px(context, TITLE_HEIGHT_DP + INFO_HEIGHT_DP);
        layout.addView(bodyLayout, bodyLp);

        LinearLayout titleBar = new LinearLayout(context);
        titleBar.setOrientation(LinearLayout.HORIZONTAL);
        titleBar.setGravity(Gravity.CENTER_VERTICAL);
        titleBar.setPadding(dp2px(context, 14), 0, dp2px(context, 4), 0);

        GradientDrawable titleBg = new GradientDrawable();
        titleBg.setColor(0xFFC97B63);
        titleBg.setStroke(dp2px(context, 1), 0xFFB56750);
        titleBg.setCornerRadii(new float[]{
                dp2px(context, 18), dp2px(context, 18),
                dp2px(context, 18), dp2px(context, 18),
                0, 0,
                0, 0
        });
        titleBar.setBackground(titleBg);

        FrameLayout.LayoutParams titleLp = new FrameLayout.LayoutParams(
                getPanelSizePx(context),
                dp2px(context, TITLE_HEIGHT_DP)
        );
        layout.addView(titleBar, titleLp);

        LinearLayout infoBar = new LinearLayout(context);
        infoBar.setOrientation(LinearLayout.HORIZONTAL);
        infoBar.setGravity(Gravity.CENTER_VERTICAL);
        infoBar.setPadding(dp2px(context, 12), 0, dp2px(context, 12), 0);

        GradientDrawable infoBg = new GradientDrawable();
        infoBg.setColor(0xFFF3E4CF);
        infoBg.setStroke(dp2px(context, 1), 0xFFD8B98A);
        infoBar.setBackground(infoBg);

        FrameLayout.LayoutParams infoLp = new FrameLayout.LayoutParams(
                getPanelSizePx(context),
                dp2px(context, INFO_HEIGHT_DP)
        );
        infoLp.topMargin = dp2px(context, TITLE_HEIGHT_DP);
        layout.addView(infoBar, infoLp);

        TextView titleText = new TextView(context);
        titleText.setText("任务");
        titleText.setTextColor(0xFFFFF6EE);
        titleText.setTextSize(14);
        LinearLayout.LayoutParams titleTextLp = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        );
        titleBar.addView(titleText, titleTextLp);

        TextView minimizeButton = new TextView(context);
        minimizeButton.setText("-");
        minimizeButton.setTextColor(0xFFFFF6EE);
        minimizeButton.setTextSize(20);
        minimizeButton.setGravity(Gravity.CENTER);
        minimizeButton.setOnClickListener(v -> minimize());
        LinearLayout.LayoutParams minimizeLp = new LinearLayout.LayoutParams(
                dp2px(context, TITLE_HEIGHT_DP),
                dp2px(context, TITLE_HEIGHT_DP)
        );
        titleBar.addView(minimizeButton, minimizeLp);

        TextView userIdLabel = new TextView(context);
        userIdLabel.setText("用户ID");
        userIdLabel.setTextColor(0xFF8B5E3C);
        userIdLabel.setTextSize(12);
        infoBar.addView(userIdLabel);

        GradientDrawable userIdBoxBg = new GradientDrawable();
        userIdBoxBg.setColor(0xFFFFFBF7);
        userIdBoxBg.setCornerRadius(dp2px(context, 10));
        userIdBoxBg.setStroke(dp2px(context, 1), 0xFFE1C7A2);

        userIdValueView = new EditText(context);
        userIdValueView.setTextColor(0xFF5C3A21);
        userIdValueView.setTextSize(12);
        userIdValueView.setSingleLine(true);
        userIdValueView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        userIdValueView.setPadding(dp2px(context, 10), dp2px(context, 4), dp2px(context, 10), dp2px(context, 4));
        userIdValueView.setHint("请输入用户ID");
        userIdValueView.setHintTextColor(0xFFB69373);
        userIdValueView.setInputType(InputType.TYPE_CLASS_TEXT);
        userIdValueView.setBackground(userIdBoxBg);
        userIdValueView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (isSyncingUserId) {
                    return;
                }
                jj.userId = s == null ? "" : s.toString().trim();
            }
        });

        LinearLayout.LayoutParams userIdValueLp = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        );
        userIdValueLp.leftMargin = dp2px(context, 10);
        infoBar.addView(userIdValueView, userIdValueLp);
        updateUserIdDisplay();

        listView = new ListView(context);
        listView.setDivider(null);
        listView.setDividerHeight(0);
        listView.setVerticalScrollBarEnabled(false);
        listView.setCacheColorHint(Color.TRANSPARENT);
        listView.setPadding(0, 0, 0, 0);
        listView.setClipToPadding(false);

        imageItems = new ArrayList<>();
//        addPreviewPlaceholders();
        adapter = new ImageGridAdapter(context);
        listView.setAdapter(adapter);

        LinearLayout.LayoutParams listLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        bodyLayout.addView(listView, listLp);

        titleBar.setOnTouchListener(this::handlePanelDragTouch);
        bodyLayout.setOnTouchListener(this::handlePanelDragTouch);
        listView.setOnTouchListener(this::handlePanelDragTouch);

        return layout;
    }

    private View createMiniIcon(Context context) {
        TextView icon = new TextView(context);
        icon.setText("任");
        icon.setTextColor(0xFFFFF6EE);
        icon.setTextSize(16);
        icon.setGravity(Gravity.CENTER);

        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(0xFFC97B63);
        drawable.setStroke(dp2px(context, 1), 0xFFB56750);
        icon.setBackground(drawable);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                dp2px(context, MINI_SIZE_DP),
                dp2px(context, MINI_SIZE_DP)
        );
        lp.gravity = Gravity.CENTER;
        icon.setLayoutParams(lp);

        icon.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    downRawX = event.getRawX();
                    downRawY = event.getRawY();
                    touchX = event.getRawX() - floatView.getX();
                    touchY = event.getRawY() - floatView.getY();
                    isMoved = false;
                    return true;

                case MotionEvent.ACTION_MOVE:
                    float deltaX = event.getRawX() - downRawX;
                    float deltaY = event.getRawY() - downRawY;
                    if (!isMoved && Math.hypot(deltaX, deltaY) > touchSlopPx) {
                        isMoved = true;
                    }
                    if (isMoved) {
                        saveX = event.getRawX() - touchX;
                        saveY = event.getRawY() - touchY;
                        floatView.setX(saveX);
                        floatView.setY(saveY);
                    }
                    return true;

                case MotionEvent.ACTION_UP:
                    if (!isMoved) {
                        v.performClick();
                        restore();
                    }
                    return true;
            }
            return false;
        });

        return icon;
    }

    private void minimize() {
        if (!(mContext instanceof Activity) || floatView == null) return;
        ((Activity) mContext).runOnUiThread(() -> {
            mainContent.setVisibility(View.GONE);
            miniIconView.setVisibility(View.VISIBLE);
            floatView.setLayoutParams(new FrameLayout.LayoutParams(
                    dp2px(mContext, MINI_SIZE_DP),
                    dp2px(mContext, MINI_SIZE_DP)
            ));
        });
    }

    private void restore() {
        if (!(mContext instanceof Activity) || floatView == null) return;
        ((Activity) mContext).runOnUiThread(() -> {
            miniIconView.setVisibility(View.GONE);
            mainContent.setVisibility(View.VISIBLE);
            floatView.setLayoutParams(new FrameLayout.LayoutParams(
                    getPanelSizePx(mContext),
                    getPanelTotalHeightPx(mContext)
            ));
            updateUserIdDisplay();
        });
    }

    public void addImageItem(String imageUrl) {
        if (!(mContext instanceof Activity) || imageItems == null || adapter == null) return;
        if (imageUrl == null || imageUrl.trim().isEmpty()) return;
        ((Activity) mContext).runOnUiThread(() -> {
            imageItems.add(imageUrl);
            adapter.notifyDataSetChanged();
        });
    }

    public void addImageItems(Collection<String> items) {
        if (!(mContext instanceof Activity) || imageItems == null || adapter == null || items == null || items.isEmpty()) return;
        ((Activity) mContext).runOnUiThread(() -> {
            imageItems.addAll(items);
            adapter.notifyDataSetChanged();
        });
    }

    public void removeImageItem(int index) {
        if (!(mContext instanceof Activity) || imageItems == null || adapter == null) return;
        if (index < 0 || index >= imageItems.size()) return;
        ((Activity) mContext).runOnUiThread(() -> {
            imageItems.remove(index);
            adapter.notifyDataSetChanged();
        });
    }

    public void clearImageItems() {
        if (!(mContext instanceof Activity) || imageItems == null || adapter == null) return;
        ((Activity) mContext).runOnUiThread(() -> {
            imageItems.clear();
            adapter.notifyDataSetChanged();
        });
    }

    public void clearList() {
        clearImageItems();
    }

    public static void postImage(String imageUrl) {
        getInstance().addImageItem(imageUrl);
    }

    public static void postImage(String imageUrl, String ignoredText) {
        getInstance().addImageItem(imageUrl);
    }

    public static void removeImage(String imageUrl) {
        if (!(mContext instanceof Activity)) return;
        ImageViewManager manager = getInstance();
        if (manager.imageItems == null || manager.adapter == null || imageUrl == null) return;
        ((Activity) mContext).runOnUiThread(() -> {
            if (manager.imageItems.remove(imageUrl)) {
                manager.adapter.notifyDataSetChanged();
            }
        });
    }

    public static void clearImages() {
        getInstance().clearImageItems();
    }

    public void attachTo(Activity activity) {
        mContext = activity;
        if (floatView == null) {
            floatView = createFloatView(activity);
        }

        ViewGroup oldParent = (ViewGroup) floatView.getParent();
        if (oldParent != null) {
            oldParent.removeView(floatView);
        }

        ViewGroup decor = (ViewGroup) activity.getWindow().getDecorView();
        floatView.setX(saveX);
        floatView.setY(saveY);
        decor.addView(floatView);
        updateUserIdDisplay();
    }

    private void notifyImageChanged() {
        if (!(mContext instanceof Activity) || adapter == null) return;
        ((Activity) mContext).runOnUiThread(() -> adapter.notifyDataSetChanged());
    }

    private void bindImage(ImageView imageView, String imageUrl) {
        imageView.setImageDrawable(null);
        imageView.setBackgroundColor(0xFFEFDCC6);

        Bitmap bitmap = imageBitmapCache.get(imageUrl);
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        } else {
            imageView.setImageResource(android.R.color.transparent);
            loadImageFromUrl(imageUrl);
        }
    }

    private void addPreviewPlaceholders() {
        if (imageItems == null || !imageItems.isEmpty()) {
            return;
        }
        imageItems.add("preview://warm");
        imageItems.add("preview://green");
        imageItems.add("preview://blue");
        imageItems.add("preview://pink");
        imageBitmapCache.put("preview://warm", createPlaceholderBitmap(0xFFE7A977));
        imageBitmapCache.put("preview://green", createPlaceholderBitmap(0xFF94B49F));
        imageBitmapCache.put("preview://blue", createPlaceholderBitmap(0xFF7FA7C9));
        imageBitmapCache.put("preview://pink", createPlaceholderBitmap(0xFFD9A5B3));
    }

    private Bitmap createPlaceholderBitmap(int color) {
        int size = dp2px(mContext, 96);
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(color);
        return bitmap;
    }

    private void loadImageFromUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return;
        }
        synchronized (loadingUrls) {
            if (loadingUrls.contains(url)) {
                return;
            }
            loadingUrls.add(url);
        }
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                URL imageUrl = new URL(url);
                connection = (HttpURLConnection) imageUrl.openConnection();
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(8000);
                connection.setDoInput(true);
                connection.connect();
                Bitmap bitmap = BitmapFactory.decodeStream(connection.getInputStream());
                if (bitmap != null) {
                    synchronized (imageBitmapCache) {
                        imageBitmapCache.put(url, bitmap);
                    }
                    notifyImageChanged();
                }
            } catch (Exception ignored) {
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                synchronized (loadingUrls) {
                    loadingUrls.remove(url);
                }
            }
        }).start();
    }

    private int dp2px(Context context, float dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
    }

    private int getPanelSizePx(Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int targetWidth = (int) (displayMetrics.widthPixels * 0.7f);
        int minSize = dp2px(context, 180);
        return Math.max(targetWidth, minSize);
    }

    private int getPanelTotalHeightPx(Context context) {
        return getPanelSizePx(context) + dp2px(context, TITLE_HEIGHT_DP + INFO_HEIGHT_DP);
    }

    private boolean handlePanelDragTouch(View v, MotionEvent event) {
        if (floatView == null) {
            return false;
        }
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            downRawX = event.getRawX();
            downRawY = event.getRawY();
            touchX = event.getRawX() - floatView.getX();
            touchY = event.getRawY() - floatView.getY();
            isMoved = false;
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            float deltaX = event.getRawX() - downRawX;
            float deltaY = event.getRawY() - downRawY;
            if (!isMoved && Math.hypot(deltaX, deltaY) > touchSlopPx) {
                isMoved = true;
            }
            if (isMoved) {
                saveX = event.getRawX() - touchX;
                saveY = event.getRawY() - touchY;
                floatView.setX(saveX);
                floatView.setY(saveY);
            }
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_UP) {
            if (isMoved) {
                v.performClick();
            }
            return true;
        }
        return true;
    }

    private void updateUserIdDisplay() {
        if (userIdValueView == null) {
            return;
        }
        String currentUserId = jj.userId;
        if (currentUserId == null) {
            currentUserId = "";
        }
        String inputValue = userIdValueView.getText() == null ? "" : userIdValueView.getText().toString();
        if (inputValue.equals(currentUserId)) {
            return;
        }
        isSyncingUserId = true;
        userIdValueView.setText(currentUserId);
        userIdValueView.setSelection(currentUserId.length());
        isSyncingUserId = false;
    }

    private class ImageGridAdapter extends BaseAdapter {
        private final Context context;
        private final int rowHeight;
        private final int rowGap;

        ImageGridAdapter(Context context) {
            this.context = context;
            this.rowGap = dp2px(context, IMAGE_GAP_DP);
            int horizontalPadding = dp2px(context, CONTENT_PADDING_DP * 2);
            int totalGap = rowGap * 2;
            this.rowHeight = (getPanelSizePx(context) - horizontalPadding - totalGap) / 3;
        }

        @Override
        public int getCount() {
            if (imageItems == null || imageItems.isEmpty()) {
                return 0;
            }
            return (imageItems.size() + 2) / 3;
        }

        @Override
        public Object getItem(int position) {
            return position;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LinearLayout row;
            ViewHolder holder;

            if (convertView == null) {
                row = new LinearLayout(context);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                holder = new ViewHolder();
                holder.imageViews = new ImageView[3];

                for (int i = 0; i < 3; i++) {
                    FrameLayout cell = new FrameLayout(context);
                    LinearLayout.LayoutParams cellLp = new LinearLayout.LayoutParams(
                            0,
                            rowHeight,
                            1f
                    );
                    if (i < 2) {
                        cellLp.rightMargin = rowGap;
                    }
                    cell.setLayoutParams(cellLp);

                    GradientDrawable cellBg = new GradientDrawable();
                    cellBg.setColor(0xFFFFFBF7);
                    cellBg.setCornerRadius(dp2px(context, IMAGE_CORNER_DP));
                    cellBg.setStroke(dp2px(context, 1), 0xFFE3C7A0);
                    cell.setBackground(cellBg);

                    ImageView imageView = new ImageView(context);
                    FrameLayout.LayoutParams imageLp = new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                    );
                    imageView.setLayoutParams(imageLp);
                    imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    cell.addView(imageView);

                    row.addView(cell);
                    holder.imageViews[i] = imageView;
                }

                row.setTag(holder);
            } else {
                row = (LinearLayout) convertView;
                holder = (ViewHolder) row.getTag();
            }

            AbsListView.LayoutParams rowLp = new AbsListView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    rowHeight + (position == 0 ? 0 : rowGap)
            );
            row.setLayoutParams(rowLp);
            row.setPadding(0, position == 0 ? 0 : rowGap, 0, 0);

            for (int i = 0; i < 3; i++) {
                int itemIndex = position * 3 + i;
                ImageView imageView = holder.imageViews[i];
                View cell = (View) imageView.getParent();
                if (itemIndex < imageItems.size()) {
                    cell.setVisibility(View.VISIBLE);
                    imageView.setVisibility(View.VISIBLE);
                    bindImage(imageView, imageItems.get(itemIndex));
                    final int currentIndex = itemIndex;
                    cell.setOnClickListener(v -> removeImageItem(currentIndex));
                } else {
                    imageView.setImageDrawable(null);
                    imageView.setVisibility(View.INVISIBLE);
                    cell.setVisibility(View.INVISIBLE);
                    cell.setOnClickListener(null);
                }
            }

            return row;
        }
    }

    private static class ViewHolder {
        ImageView[] imageViews;
    }
}
