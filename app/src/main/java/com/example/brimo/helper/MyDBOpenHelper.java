package com.example.brimo.helper;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

public class MyDBOpenHelper extends SQLiteOpenHelper {
    private static final String DBNAME = "bill.db";
    private static final int VERSION = 1;

    public MyDBOpenHelper(Context context) {
        super(context, DBNAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table log\n" +
                "(\n" +
                "    id    integer\n" +
                "        constraint log_pk\n" +
                "            primary key autoincrement,\n" +
                "    transaksi  text,\n" +
                "    time  text,\n" +
                "    money text,\n" +
                "    md5   text\n" +
                ");");
    }

    //select * from log where content=?

    /**
     * 判断不存在数据
     *
     * @param sql SQL命令
     * @param str 字符串
     * @return
     */
    public boolean isEmpty(String sql, String[] str) {
        SQLiteDatabase read = getReadableDatabase();
        Cursor cursor = read.rawQuery(sql, str);
        int count = cursor.getCount();
        cursor.close();
        read.close();
        return count == 0;
    }

    public JSONArray getResults(String searchQuery, String[] userTable) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor cursor = db.rawQuery(searchQuery, userTable);
        JSONArray resultSet = new JSONArray();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            int totalColumn = cursor.getColumnCount();
            JSONObject rowObject = new JSONObject();
            for (int i = 0; i < totalColumn; i++) {
                if (cursor.getColumnName(i) != null) {
                    try {
                        if (cursor.getString(i) != null) {
                            //Log.d("userTable", cursor.getString(i) );
                            rowObject.put(cursor.getColumnName(i), cursor.getString(i));
                        } else {
                            rowObject.put(cursor.getColumnName(i), "");
                        }
                    } catch (Exception e) {
                        // Log.d("userTable", e.getMessage()  );
                    }
                }
            }
            resultSet.add(rowObject);
            cursor.moveToNext();
        }
        cursor.close();
        return resultSet;
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
