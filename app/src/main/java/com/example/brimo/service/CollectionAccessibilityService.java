package com.example.brimo.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Path;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.example.brimo.bean.LogBean;
import com.example.brimo.http.OkhttpUtil;
import com.example.brimo.utils.MD5Util;
import com.example.brimo.helper.MyDBOpenHelper;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.Data;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * 收款监听
 */
public class CollectionAccessibilityService extends AccessibilityService {
    private static final String TAG = "CollectionAccessibility";
    private boolean isPost = false, isRun = true;//判断是否已经提交订单信息
    private MyDBOpenHelper myDBOpenHelper;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private void simulateSwipeUp() {
        // 创建手势路径
        Path path = new Path();
        path.moveTo(500, 1000); // 起始点（x, y）
        path.lineTo(500, 1500);  // 结束点（x, y）
        // 创建手势描述
        GestureDescription.StrokeDescription strokeDescription = new GestureDescription.StrokeDescription(path, 0, 1000); // 持续时间为500毫秒
        // 使用 GestureDescription.Builder 创建 GestureDescription 实例
        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(strokeDescription);
        // 调用 dispatchGesture 执行手势
        dispatchGesture(builder.build(), null, null);
        if (isRun) {
            print("启动滑动");
            handler.postDelayed(this::simulateSwipeUp, 10_000);
        }
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        print("服务开启");
        myDBOpenHelper = new MyDBOpenHelper(this);
        handler.postDelayed(this::simulateSwipeUp, 5_000);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        try {
            if (isPost) return;
            AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
            if (nodeInfo == null) return;
            handleNode(nodeInfo);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void handleNode(AccessibilityNodeInfo nodeInfo) {
        //获取列表集合
        List<LogBean> beans = new ArrayList<>();
        List<AccessibilityNodeInfo> recyclers = nodeInfo.findAccessibilityNodeInfosByViewId("id.co.bri.brimo:id/ll_item");

        for (AccessibilityNodeInfo recycler : recyclers) {
            //金额(+ Rp50.000,00) 去掉小数点
            AccessibilityNodeInfo balance = First(recycler.findAccessibilityNodeInfosByViewId("id.co.bri.brimo:id/tv_nominal_mutasi"));
            //时间(18:41:46 WIB)
            AccessibilityNodeInfo time = First(recycler.findAccessibilityNodeInfosByViewId("id.co.bri.brimo:id/tv_time_mutasi"));
            //付款人信息(BFST708101012001508ANDHIKA RYAN:BNINIDJA)
            AccessibilityNodeInfo notes = First(recycler.findAccessibilityNodeInfosByViewId("id.co.bri.brimo:id/tv_transaksi"));
            LogBean logBean = getBean(balance, notes, time);
            if (logBean == null) continue;
            if (isEmpty(logBean)) continue;
            if (logBean.getMoney() == 0) continue;
            boolean isLog = myDBOpenHelper.isEmpty("select * from log where md5=? ", new String[]{logBean.getMd5()});
            if (!isLog) continue;
            beans.add(logBean);
        }

        post(beans);
    }


    /**
     * 提交信息
     *
     * @param beans
     */
    private void post(List<LogBean> beans) {
        if (beans.isEmpty()) return;
        isPost = true;
        new Thread(new PostData(beans)).start();
    }

    @Data
    private class PostData implements Runnable {
        private final List<LogBean> beans;

        @Override
        public void run() {
            List<LogBean> oks = new ArrayList<>();
            for (LogBean logBean : beans) {
                OkHttpClient client = new OkHttpClient();
                ZonedDateTime beijingTime = ZonedDateTime.now().withZoneSameInstant(java.time.ZoneId.of("Asia/Shanghai"));
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                String formattedBeijingTime = beijingTime.format(formatter);
                FormBody.Builder form = new FormBody.Builder();
                form.add("amount", String.valueOf(logBean.getMoney()));
                form.add("payerName", logBean.getTransaksi());
                form.add("time", formattedBeijingTime);
                print(String.format("金额："+logBean.getMoney()+"|名字:"+logBean.getTransaksi()+"|时间:"+formattedBeijingTime));
                Request.Builder builder = new Request.Builder()
                        .url("https://admin.tynpay.site/app/confirmReceiptSuccess");
                try (Response response = client.newCall(builder.build()).execute()) {
                    oks.add(logBean);
                    ResponseBody responseBody = response.body();
                    if (responseBody != null) {
                        String text = responseBody.string();
                        print("上传成功：" + text);
                    }
                } catch (IOException e) {
                    print("上传失败");
                    e.printStackTrace();
                }
            }
            instData(oks);
        }

        private void instData(List<LogBean> beans) {
            if (beans.isEmpty()) return;
            SQLiteDatabase writ = myDBOpenHelper.getWritableDatabase();
            writ.beginTransaction();
            for (LogBean transactionEntity : beans) {
                ContentValues contentValues = new ContentValues();
                contentValues.put("transaksi", transactionEntity.getTransaksi());
                contentValues.put("time", transactionEntity.getTime());
                contentValues.put("money", transactionEntity.getMoney());
                contentValues.put("md5", transactionEntity.getMd5());
                writ.insert("log", null, contentValues);
            }
            writ.setTransactionSuccessful();
            writ.endTransaction();
            writ.close();
            isPost = false;
        }
    }

    /**
     * 获取实体类
     *
     * @param nodeInfo
     * @return
     */
    private LogBean getBean(AccessibilityNodeInfo... nodeInfo) {
        LogBean logBean = new LogBean();
        for (int i = 0; i < nodeInfo.length; i++) {
            AccessibilityNodeInfo info = nodeInfo[i];
            if (info == null) {
                print("序列为空:" + i);
            }
            if (info == null) return null;
            instData(i, logBean, info);
        }
        logBean.setMd5(getMd5(logBean));
        print("实体类结果:" + JSON.toJSONString(logBean));
        return logBean;
    }

    /**
     * 注入信息
     *
     * @param i
     * @param logBean
     * @param info
     */
    private void instData(int i, LogBean logBean, AccessibilityNodeInfo info) {
        if (i == 0) {//金额
            CharSequence balances = info.getText();
            if (balances == null) return;
            String balance = balances.toString();
            print("金额:" + balance);
            if (!balance.startsWith("+")) return;//不是入账
            String money = getBalance(balance);
            if (money != null) {
                money = money.replace(".", "");
                logBean.setMoney(Long.parseLong(money));
            }
        } else if (i == 1) {//付款信息
            CharSequence balances = info.getText();
            if (balances == null) return;
            print("收款信息:" + balances);
            logBean.setTransaksi(balances.toString());
        } else if (i == 2) {//时间
            CharSequence balances = info.getText();
            if (balances == null) return;
            print("时间:" + balances);
            logBean.setTime(balances.toString());
        }
    }


    private void print(String msg) {
        Log.d(TAG, msg);
    }

    /**
     * 提取金额
     *
     * @param input
     * @return
     */
    private String getBalance(String input) {
        String regex = "\\+ Rp([0-9.]+),([0-9]{2})";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            // 提取捕获组中的数字部分
            return matcher.group(1);
        }
        return null;
    }


    /**
     * 获取第一个
     *
     * @param nodeInfos
     * @return
     */
    private AccessibilityNodeInfo First(List<AccessibilityNodeInfo> nodeInfos) {
        if (nodeInfos.isEmpty()) {
            return null;
        }
        return nodeInfos.get(0);
    }

    /**
     * 获取为空
     *
     * @return
     */
    private boolean isEmpty(LogBean logBean) {
        JSONObject jsonObject = JSON.parseObject(JSON.toJSONString(logBean));
        for (String key : jsonObject.keySet()) {
            Object value = jsonObject.get(key);
            if (value == null) return true;
        }
        return false;
    }


    /**
     * 获取md5
     *
     * @param logBean
     * @return
     */
    public String getMd5(LogBean logBean) {
        return MD5Util.get(JSON.toJSONString(logBean));
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        isRun = false;
        myDBOpenHelper.close();
    }

    @Override
    public void onInterrupt() {

    }
}
