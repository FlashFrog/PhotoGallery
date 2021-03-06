package com.bignerdranch.android.photogallery;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.List;

/**
 * Created by Leo on 2017/7/22.
 */

public class PollService extends IntentService {

    private static final String TAG = "PollService";

    private static final long POLL_INTERVAL =  1000 * 10;

    public static final String ACTION_SHOW_NOTIFICATION = "com.bignerdranch.android.photogallery.SHOW_NOTIFICATION";

    public static final String PERM_PRIVATE = "com.bignerdranch.android.photogallery.PRIVATE";

    public static final String REQUEST_CODE = "REQUEST_CODE";

    public static final String NOTIFICATION = "NOTIFICATION";

    public static Intent newIntent(Context context){
        return new Intent(context,PollService.class);
    }

    public static void setServiceAlarm(Context context, boolean isOn){
        Intent i = PollService.newIntent(context);
        PendingIntent pi = PendingIntent.getService(context, 0, i , 0);

        AlarmManager alarmManager = (AlarmManager)context.getSystemService(ALARM_SERVICE);

        if(isOn){
            alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(),POLL_INTERVAL,pi); //启动定时器
        }else {
            alarmManager.cancel(pi);
            pi.cancel();
        }

        QueryPreferences.setAlarmOn(context,isOn); //设置定时器启停状态
    }

    //判断定时器的状态
    public static boolean isServiceAlarmOn(Context context){
        Intent i = PollService.newIntent(context);
        PendingIntent pi = PendingIntent.getService(context,0,i,PendingIntent.FLAG_NO_CREATE);
        return pi != null;
    }
    public PollService(){
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent){
        if(!isNetworkAvailableAndConnected()){
            return ;        }
        Log.d(TAG, "onHandleIntent: ");
        String query = QueryPreferences.getStoredQuery(this);  //获取当前查询结果以及上一次结果ID
        String lastResultId = QueryPreferences.getLastResultId(this);
        List<GalleryItem> items;

        //获取最新结果集
        if(query == null){
            items = new FlickrFetcher().fetchRecentPhotos();
        }else{
            items = new FlickrFetcher().searchPhotos(query);
        }

        if(items.size() == 0){
            return ;
        }
        //获取第一条结果
        String resultId = items.get(0).getId();
        //确认是否不同于上一次结果ID
        if(resultId.equals(lastResultId)){
            Log.i(TAG, "Got a old result" + resultId);
        }else{
            Log.i(TAG, "Got a new result" + resultId);

            Resources resources = getResources();
            Intent i = PhotoGalleryActivity.newIntent(this);
            PendingIntent pi = PendingIntent.getActivity(this,0,i,0);

            Notification notification = new NotificationCompat.Builder(this)
                    .setTicker(resources.getString(R.string.new_pictures_title))
                    .setSmallIcon(android.R.drawable.ic_menu_report_image)
                    .setContentTitle(resources.getString(R.string.new_pictures_title))
                    .setContentText(resources.getString(R.string.new_pictures_text))
                    .setContentIntent(pi)
                    .setAutoCancel(true)
                    .build();



            showBackgroundNotification(0,notification);
        }
        //将第一条结果存入SharedPreferences
        QueryPreferences.setLastResultId(this,resultId);
    }

    private boolean isNetworkAvailableAndConnected(){
        ConnectivityManager cm = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);

        boolean isNetworkAvailable = cm.getActiveNetworkInfo() != null; //
        boolean isNetworkConnected = isNetworkAvailable && cm.getActiveNetworkInfo().isConnected(); //网络是否是完全连接

        return isNetworkConnected;
    }

    private void showBackgroundNotification(int requestCode, Notification notification){
        Intent i = new Intent(ACTION_SHOW_NOTIFICATION);
        i.putExtra(REQUEST_CODE,requestCode);
        i.putExtra(NOTIFICATION,notification);
        /*
         * 第三个参数为一个results receiver
         * 第四：一个支持result receiver运行的Handler
         * 第五: 结果代码初始值
         * 第六: 结果数据
         * 第七: 有序broadcast的结果附加内容
         */
        sendOrderedBroadcast(i, PERM_PRIVATE, null, null, Activity.RESULT_OK, null, null);
    }
}
