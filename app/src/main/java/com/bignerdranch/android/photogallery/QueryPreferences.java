package com.bignerdranch.android.photogallery;

import android.content.Context;
import android.preference.PreferenceManager;

/**
 * Created by Leo on 2017/7/21.
 */

public class QueryPreferences {

    private static final String PREF_SEARCH_QUERY = "searchQuery"; //用来存放KEy

    public static String getStoredQuery(Context context){ //查询
        return PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_SEARCH_QUERY,null);
    }

    public static void setStoredQuery(Context context, String query){
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putString(PREF_SEARCH_QUERY,query)
                .apply();//异步写入数据 apply()方法首先在内存中执行数据变更，然后在后台线程上真正的把数据写入文件。
    }
}
