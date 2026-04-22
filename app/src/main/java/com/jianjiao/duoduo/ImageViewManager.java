package com.jianjiao.duoduo;

import android.app.Activity;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.DisplayMetrics;
import android.view.ViewConfiguration;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

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
    private ArrayList<Object> imageItems;
    private ImageGridAdapter adapter;
    public static Context mContext;
    private final Map<String, Bitmap> imageBitmapCache = new HashMap<>();
    private final Set<String> loadingUrls = new HashSet<>();

    public interface OnBindImageViewListener {
        void onBind(ImageView imageView, Object item, int position);
    }

    private OnBindImageViewListener onBindImageViewListener;

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
                getPanelSizePx(context) + dp2px(context, TITLE_HEIGHT_DP)
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
                getPanelSizePx(context) + dp2px(context, TITLE_HEIGHT_DP)
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
        bodyLp.topMargin = dp2px(context, TITLE_HEIGHT_DP);
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

        listView = new ListView(context);
        listView.setDivider(null);
        listView.setDividerHeight(0);
        listView.setVerticalScrollBarEnabled(false);
        listView.setCacheColorHint(Color.TRANSPARENT);
        listView.setPadding(0, 0, 0, 0);
        listView.setClipToPadding(false);

        imageItems = new ArrayList<>();
        addPreviewPlaceholders();
        adapter = new ImageGridAdapter(context);
        listView.setAdapter(adapter);

        LinearLayout.LayoutParams listLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        bodyLayout.addView(listView, listLp);

        titleBar.setOnTouchListener((v, event) -> {
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
                v.performClick();
                return true;
            }
            return true;
        });

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
                    getPanelSizePx(mContext) + dp2px(mContext, TITLE_HEIGHT_DP)
            ));
        });
    }

    public void setOnBindImageViewListener(OnBindImageViewListener listener) {
        onBindImageViewListener = listener;
        notifyImageChanged();
    }

    public void addImageItem(Object item) {
        if (!(mContext instanceof Activity) || imageItems == null || adapter == null) return;
        ((Activity) mContext).runOnUiThread(() -> {
            imageItems.add(item);
            adapter.notifyDataSetChanged();
        });
    }

    public void addImageItems(Collection<?> items) {
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

    public void addItem(String text) {
        addImageItem(text);
    }

    public void clearList() {
        clearImageItems();
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
    }

    private void notifyImageChanged() {
        if (!(mContext instanceof Activity) || adapter == null) return;
        ((Activity) mContext).runOnUiThread(() -> adapter.notifyDataSetChanged());
    }

    private void bindImage(ImageView imageView, Object item, int position) {
        imageView.setImageDrawable(null);
        imageView.setBackgroundColor(0xFFEFDCC6);

        if (item instanceof Drawable) {
            imageView.setImageDrawable((Drawable) item);
        } else if (item instanceof Bitmap) {
            imageView.setImageBitmap((Bitmap) item);
        } else if (item instanceof Integer) {
            imageView.setImageResource((Integer) item);
        } else if (item instanceof String) {
            String url = (String) item;
            Bitmap bitmap = imageBitmapCache.get(url);
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            } else {
                imageView.setImageResource(android.R.color.transparent);
                loadImageFromUrl(url);
            }
        }

        if (onBindImageViewListener != null) {
            onBindImageViewListener.onBind(imageView, item, position);
        }
    }

    private void addPreviewPlaceholders() {
        if (imageItems == null || !imageItems.isEmpty()) {
            return;
        }
        imageItems.add(createPlaceholderDrawable(0xFFE7A977));
        imageItems.add(createPlaceholderDrawable(0xFF94B49F));
        imageItems.add(createPlaceholderDrawable(0xFF7FA7C9));
        imageItems.add(createPlaceholderDrawable(0xFFD9A5B3));
    }

    private Drawable createPlaceholderDrawable(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp2px(mContext, IMAGE_CORNER_DP));
        return drawable;
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
        int halfScreenWidth = displayMetrics.widthPixels / 2;
        int minSize = dp2px(context, 180);
        return Math.max(halfScreenWidth, minSize);
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
                    bindImage(imageView, imageItems.get(itemIndex), itemIndex);
                } else {
                    imageView.setImageDrawable(null);
                    imageView.setVisibility(View.INVISIBLE);
                    cell.setVisibility(View.INVISIBLE);
                }
            }

            return row;
        }
    }

    private static class ViewHolder {
        ImageView[] imageViews;
    }
}
