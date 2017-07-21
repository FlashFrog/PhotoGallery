package com.bignerdranch.android.photogallery;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Leo on 2017/7/20.
 */


public class ThumbnailDownloader<T> extends HandlerThread {

    private static final String TAG = "ThumbnailDownloader";
    private static final int MESSAGE_DOWNLOAD = 0; //标识下载请求

    private Boolean mHasQuit = false;
    private Handler mRequestHandler; //存储对Handler的引用，这个Handler负责在ThumbnailDownloader后台线程上管理下载请求消息队列。这个Handler也负责从消息队列里取出并处理下载请求消息。
    private ConcurrentHashMap<T,String> mRequestMap = new ConcurrentHashMap<>();//ConcurrentHashMap是一种线程安全的HashMap
    private Handler mResponseHandler; //用于存放来自于主线程的Handler
    private ThumbnailDownloadListener<T> mThumbnailDownloadListener;

    public interface ThumbnailDownloadListener<T>{
        /*
         * 图片下载完成，可以交给UI去显示时，接口中的方法就会被调用。
         * 会使用这个方法把处理已下载图片的任务代理给另一个类（PhotoGalleryFragment）,这样ThumbnailDownloader就可以把下载结果传给其他视图对象。
         */
        void onThumnailDownloaded(T target, Bitmap thumbnail);
    }

    public void setThumbnailDownloadListener(ThumbnailDownloadListener<T> listener){
        mThumbnailDownloadListener = listener;
    }
    public ThumbnailDownloader(Handler responseHandler){
        super(TAG);
        mResponseHandler = responseHandler;
    }


    //onLooperPrepared()在Looper首次检查消息队列之前调用的。
    @Override
    protected void onLooperPrepared(){
        mRequestHandler = new Handler(){
            @Override
            public void handleMessage(Message msg){  //队列中的下载消息取出并可以处理时，就会触发调用Handler.handleMessage()方法。
                if(msg.what == MESSAGE_DOWNLOAD){
                    T target = (T)msg.obj;
                    Log.i(TAG, "Got a request for URL"+ mRequestMap.get(target));
                    handleRequest(target);
                }
            }
        };
    }
    @Override
    public boolean quit(){
        mHasQuit = true;
        return super.quit();
    }

    public void queueThumbnail(T target, String url) {
        Log.i(TAG, "Got a URL" + url);

        if(url  == null){
            mRequestMap.put(target,url);
        }else {
            mRequestMap.put(target, url);

            mRequestHandler.obtainMessage(MESSAGE_DOWNLOAD,target).sendToTarget();
        }
    }

    public void clearQueue(){
        mRequestHandler.removeMessages(MESSAGE_DOWNLOAD);
    }
    private void handleRequest(final T target){
        try {
            final String url = mRequestMap.get(target);

            if(url == null){
                return ;
            }

            byte[] bitmapBytes = new FlickrFetcher().getUrlBytes(url);
            final Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapBytes,0,bitmapBytes.length);//将返回的字节数组转化为位图
            Log.i(TAG, "Bitmap Created");

            mResponseHandler.post(new Runnable() {
                @Override
                public void run() {
                    if(mRequestMap.get(target) != url || mHasQuit){  //再次检查requestMap 因为RecyclerView会循环使用其视图，
                        return ;
                    }
                    mRequestMap.remove(target); //在requesMap中删除配对的PhotoHolder-URL
                    mThumbnailDownloadListener.onThumnailDownloaded(target, bitmap);
                }
            });
        }catch (IOException ioe){
            Log.e(TAG, "Error downloading image", ioe);
        }
    }
}
