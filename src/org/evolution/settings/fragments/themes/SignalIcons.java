/*
 * Copyright (C) 2022-2025 crDroid Android Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.evolution.settings.fragments.themes;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.core.InstrumentedFragment;
import com.android.settings.R;
import com.android.internal.util.evolution.ThemeUtils;

import java.lang.ref.WeakReference;
import java.util.List;

public class SignalIcons extends InstrumentedFragment {

    private static final String TAG = "SignalIcons";

    private RecyclerView mRecyclerView;
    private ThemeUtils mThemeUtils;
    private final String mCategory = "android.theme.customization.signal_icon";
    private List<String> mPkgs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().setTitle(R.string.theme_customization_signal_icon_title);
        mThemeUtils = new ThemeUtils(requireContext());
        mPkgs = mThemeUtils.getOverlayPackagesForCategory(mCategory, "android");
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.item_view, container, false);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        new Handler().post(() -> {
            mRecyclerView = view.findViewById(R.id.recycler_view);
            if (mRecyclerView != null) {
                mRecyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 3));
                mRecyclerView.setAdapter(new Adapter(requireContext(), mPkgs, mThemeUtils, mCategory));
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mRecyclerView != null) {
            mRecyclerView.setAdapter(null);
            mRecyclerView = null;
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.EVOLVER;
    }

    public static class Adapter extends RecyclerView.Adapter<Adapter.CustomViewHolder> {
        private final WeakReference<Context> contextRef;
        private final List<String> mPkgs;
        private final ThemeUtils mThemeUtils;
        private final String mCategory;
        private final String mAppliedPkg;
        private String mSelectedPkg;

        public Adapter(Context context, List<String> pkgs, ThemeUtils themeUtils, String category) {
            this.contextRef = new WeakReference<>(context);
            this.mPkgs = pkgs;
            this.mThemeUtils = themeUtils;
            this.mCategory = category;

            mAppliedPkg = mThemeUtils.getOverlayInfos(mCategory).stream()
                    .filter(info -> info.isEnabled())
                    .map(info -> info.packageName)
                    .findFirst()
                    .orElse("android");
            mSelectedPkg = mAppliedPkg;
        }

        @NonNull
        @Override
        public CustomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.icon_option, parent, false);
            return new CustomViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CustomViewHolder holder, int position) {
            Context context = contextRef.get();
            if (context == null) return;

            String pkg = mPkgs.get(position);

            if ("android".equals(pkg)) {
                holder.image1.setBackgroundDrawable(new QPR1SignalDrawable(context, 0));
                holder.image2.setBackgroundDrawable(new QPR1SignalDrawable(context, 1));
                holder.image3.setBackgroundDrawable(new QPR1SignalDrawable(context, 2));
                holder.image4.setBackgroundDrawable(new QPR1SignalDrawable(context, 4));
            } else {
                holder.image1.setBackgroundDrawable(getDrawable(context, pkg, "ic_signal_cellular_0_5_bar"));
                holder.image2.setBackgroundDrawable(getDrawable(context, pkg, "ic_signal_cellular_1_5_bar"));
                holder.image3.setBackgroundDrawable(getDrawable(context, pkg, "ic_signal_cellular_3_5_bar"));
                holder.image4.setBackgroundDrawable(getDrawable(context, pkg, "ic_signal_cellular_5_5_bar"));
            }

            String label = getLabel(context, pkg);
            holder.name.setText("android".equals(pkg) ? "Default" : label);
            holder.itemView.setActivated(pkg.equals(mSelectedPkg));

            holder.itemView.setOnClickListener(view -> {
                if (!pkg.equals(mSelectedPkg)) {
                    String oldPkg = mSelectedPkg;
                    mSelectedPkg = pkg;

                    if ("android".equals(pkg)) {
                        Settings.System.putInt(context.getContentResolver(), "signal_icon_use_overlays", 0);
                    } else {
                        Settings.System.putInt(context.getContentResolver(), "signal_icon_use_overlays", 1);
                    }

                    mThemeUtils.setOverlayEnabled(mCategory, pkg, "android");
                    updateActivatedStatus(oldPkg);
                    updateActivatedStatus(mSelectedPkg);
                }
            });
        }

        @Override
        public int getItemCount() {
            return mPkgs.size();
        }

        private void updateActivatedStatus(String pkg) {
            int index = mPkgs.indexOf(pkg);
            if (index >= 0) {
                notifyItemChanged(index);
            }
        }

        public static class CustomViewHolder extends RecyclerView.ViewHolder {
            TextView name;
            ImageView image1, image2, image3, image4;

            public CustomViewHolder(View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.option_label);
                image1 = itemView.findViewById(R.id.image1);
                image2 = itemView.findViewById(R.id.image2);
                image3 = itemView.findViewById(R.id.image3);
                image4 = itemView.findViewById(R.id.image4);
            }
        }

        private Drawable getDrawable(Context context, String pkg, String drawableName) {
            try {
                PackageManager pm = context.getPackageManager();
                Resources res = pkg.equals("android") ? Resources.getSystem() : pm.getResourcesForApplication(pkg);
                int resId = res.getIdentifier(drawableName, "drawable", pkg);
                if (resId != 0) {
                    return res.getDrawable(resId, context.getTheme());
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Drawable load failed for pkg: " + pkg + ", name: " + drawableName, e);
            }
            return null;
        }

        private String getLabel(Context context, String pkg) {
            PackageManager pm = context.getPackageManager();
            try {
                return pm.getApplicationInfo(pkg, 0).loadLabel(pm).toString();
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Label load failed for pkg: " + pkg, e);
            }
            return pkg;
        }
    }

    private static class QPR1SignalDrawable extends Drawable {
        private final Paint activePaint;
        private final Paint inactivePaint;
        private final int level;
        private final float density;

        public QPR1SignalDrawable(Context context, int level) {
            this.level = level;
            this.density = context.getResources().getDisplayMetrics().density;

            int nightMode = context.getResources().getConfiguration().uiMode 
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
            boolean isDarkMode = nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES;

            int activeColor = isDarkMode ? 0xFFFFFFFF : 0xFF000000;
            int inactiveColor = isDarkMode ? 0xFF555555 : 0xFFAAAAAA;

            activePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            activePaint.setStyle(Paint.Style.FILL);
            activePaint.setColor(activeColor);

            inactivePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            inactivePaint.setStyle(Paint.Style.FILL);
            inactivePaint.setColor(inactiveColor);
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            int width = getBounds().width();
            int height = getBounds().height();

            float barWidth = 2.5f * density;
            float barSpacing = 1.5f * density;
            float dotRadius = 1.5f * density;

            float totalWidth = (barWidth * 4) + (barSpacing * 3);
            float startX = (width - totalWidth) / 2f;
            float baseY = height * 0.7f;
            float minBarHeight = 3f * density;

            for (int i = 0; i < 4; i++) {
                float x = startX + (i * (barWidth + barSpacing));
                float barHeight = Math.max(minBarHeight, ((i + 1) / 4f) * (height * 0.45f));
                Paint barPaint = (i < level) ? activePaint : inactivePaint;
                canvas.drawRoundRect(
                    x,
                    baseY - barHeight,
                    x + barWidth,
                    baseY,
                    barWidth / 2f,
                    barWidth / 2f,
                    barPaint
                );

                float dotY = baseY + (dotRadius * 2.5f);
                Paint dotPaint = (i < level) ? activePaint : inactivePaint;
                canvas.drawCircle(x + barWidth / 2f, dotY, dotRadius, dotPaint);
            }
        }

        @Override
        public void setAlpha(int alpha) {
            activePaint.setAlpha(alpha);
            inactivePaint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(@Nullable android.graphics.ColorFilter colorFilter) {
            activePaint.setColorFilter(colorFilter);
            inactivePaint.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity() {
            return android.graphics.PixelFormat.TRANSLUCENT;
        }

        @Override
        public int getIntrinsicWidth() {
            return (int)(20 * density);
        }

        @Override
        public int getIntrinsicHeight() {
            return (int)(20 * density);
        }
    }
}
