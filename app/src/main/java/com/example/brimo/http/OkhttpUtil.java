package com.example.brimo.http;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;

import com.alibaba.fastjson2.JSON;
import com.example.brimo.bean.PostBean;
import com.example.brimo.bean.LogBean;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OkhttpUtil {
    private static final String TAG = "OkhttpUtil";
    private final List<LogBean> transactionEntities;
    private OnError onError;
    private OnSuccess onSuccess;
    private String url, key;
    private final OkHttpClient client = new OkHttpClient();

    public OkhttpUtil(List<LogBean> transactionEntities) {
        this.transactionEntities = transactionEntities;
    }

    public OkhttpUtil setOnError(OnError onError) {
        this.onError = onError;
        return this;
    }

    public OkhttpUtil setKey(String key) {
        this.key = key;
        return this;
    }

    public OkhttpUtil setOnSuccess(OnSuccess onSuccess) {
        this.onSuccess = onSuccess;
        return this;
    }

    public OkhttpUtil setUrl(String url) {
        this.url = url;
        return this;
    }

    public void post() {
        PostBean postEntity = getData();
        String json = JSON.toJSONString(postEntity);
        RequestBody requestBody = RequestBody.create(json.getBytes());
        Request.Builder builder = new Request.Builder();
        builder.post(requestBody);
        builder.url(url);
        client.newCall(builder.build()).enqueue(new Callback());
    }

    private class Callback implements okhttp3.Callback, Handler.Callback {
        private final Handler handler = new Handler(Looper.getMainLooper(), this);

        @Override
        public void onFailure(@NonNull Call call, @NonNull IOException e) {
            handler.sendEmptyMessage(0);
        }

        @Override
        public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
            handler.sendEmptyMessage(1);
        }

        @Override
        public boolean handleMessage(@NonNull Message msg) {
            if (msg.what == 0 && onError != null) {
                onError.onError(new IOException(""));
            } else if (msg.what == 1 && onSuccess != null) {
                onSuccess.onSuccess(transactionEntities);
            }
            return false;
        }
    }

    private PostBean getData() {
        long time = System.currentTimeMillis();
        String data = JSON.toJSONString(transactionEntities);
        String signData = "data=" + data + "&timestamp=" + time + "&key=" + key;
        String sign = get(signData);

        PostBean postEntity = new PostBean();
        postEntity.setData(transactionEntities);
        postEntity.setSign(sign);
        postEntity.setTimestamp(time);

        return postEntity;
    }

    public String get(String text) {
        String result = null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(text.getBytes());
            result = toHexString(digest);
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        }
        return result;
    }

    private String toHexString(byte[] digest) {
        StringBuilder sb = new StringBuilder();
        String hexStr;
        for (byte b : digest) {
            hexStr = Integer.toHexString(b & 0xFF);//& 0xFF处理负数
            if (hexStr.length() == 1) {//长度等于1，前面进行补0，保证最后的字符串长度为32
                hexStr = "0" + hexStr;
            }
            sb.append(hexStr);
        }
        return sb.toString();
    }


    public interface OnSuccess {

        void onSuccess(List<LogBean> transactionEntities);
    }

    public interface OnError {

        void onError(IOException e);
    }

    public static OkhttpUtil with(List<LogBean> transactionEntities) {
        return new OkhttpUtil(transactionEntities);
    }
}
