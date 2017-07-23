package com.bignerdranch.android.photogallery;

import android.content.Context;
import android.preference.PreferenceManager;

/**
 * Created by Leo on 2017/7/21.
 */

public class QueryPreferences {

    private static final String PREF_SEARCH_QUERY = "searchQuery"; //用来存放KEy
    private static final String PREF_LAST_RESULT_ID = "lastResultId";
    private static final String PREF_IS_ALARM_ON = "isAlarmOn";


    public static String getStoredQuery(Context context) { //查询
        return PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_SEARCH_QUERY, null);
    }

    public static void setStoredQuery(Context context, String query) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putString(PREF_SEARCH_QUERY, query)
                .apply();//异步写入数据 apply()方法首先在内存中执行数据变更，然后在后台线程上真正的把数据写入文件。
    }


    public static String getLastResultId(Context context){
        return PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_LAST_RESULT_ID,null);
    }
    //存储最近一次获取图片的ID
    public static void setLastResultId(Context context, String lastResultId){
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(PREF_LAST_RESULT_ID,lastResultId)
                .apply();
    }


    //获取定时器的启停状态
    public static boolean isAlarmOn(Context context){
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(PREF_IS_ALARM_ON,false);
    }

    //设置定时器的启停状态
    public static void setAlarmOn(Context context, boolean isOn){
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(PREF_IS_ALARM_ON,isOn)
                .apply();
    }

}
