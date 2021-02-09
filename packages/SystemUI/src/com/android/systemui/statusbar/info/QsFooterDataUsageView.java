package com.android.systemui.statusbar.info;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.os.AsyncTask;
import android.os.Handler;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.text.BidiFormatter;
import android.text.format.Formatter;
import android.text.format.Formatter.BytesResult;
import android.widget.TextView;
import android.provider.Settings;
import android.view.View;

import com.android.internal.util.zenx.ZenxUtils;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.NetworkController.SignalCallback;
import com.android.settingslib.net.DataUsageController;

public class QsFooterDataUsageView extends TextView {

    private Context mContext;
    private NetworkController mNetworkController;
    private static boolean shouldUpdateData;
    private String formatedinfo;
    private Handler mHandler;
    private static long mTime;

    public QsFooterDataUsageView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mContext = context;
        mNetworkController = Dependency.get(NetworkController.class);
        mHandler = new Handler();
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (isDataUsageEnabled()) {
            if(shouldUpdateData) {
                shouldUpdateData = false;
                updateDataUsage();
            } else {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        updateDataUsage();
                    }
                }, 10000);
            }
        }
    }

    private void updateDataUsage() {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                updateUsageData();
            }
        });
        setText(formatedinfo);
    }

    private void updateUsageData() {
        DataUsageController mobileDataController = new DataUsageController(mContext);
        mobileDataController.setSubscriptionId(
            SubscriptionManager.getDefaultDataSubscriptionId());
        final DataUsageController.DataUsageInfo info = isDataUsageEnabled() ?
                (ZenxUtils.isWiFiConnected(mContext) ?
                        mobileDataController.getDailyWifiDataUsageInfo()
                        : mobileDataController.getDailyDataUsageInfo())
                : (ZenxUtils.isWiFiConnected(mContext) ?
                        mobileDataController.getWifiDataUsageInfo()
                        : mobileDataController.getDataUsageInfo());
        if(formatedinfo == "" || formatedinfo == " ") {
            formatedinfo = "Loading..";
        } else {
            formatedinfo = formatDataUsage(info.usageLevel) + " ";
        }
    }

    public boolean isDataUsageEnabled() {
        return Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.QS_FOOTER_INFO, 3) == 1 || Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.QS_FOOTER_INFO_RIGHT, 2) == 2;
    }

    public static void updateUsage() {
        // limit to one update per second
        long time = System.currentTimeMillis();
        if (time - mTime > 1000) {
            shouldUpdateData = true;
        }
        mTime = time;
    }

    private CharSequence formatDataUsage(long byteValue) {
        final BytesResult res = Formatter.formatBytes(mContext.getResources(), byteValue,
                Formatter.FLAG_IEC_UNITS);
        return BidiFormatter.getInstance().unicodeWrap(mContext.getString(
                com.android.internal.R.string.fileSizeSuffix, res.value, res.units));
    }
}
