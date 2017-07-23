package com.bignerdranch.android.photogallery;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Leo on 2017/7/19.
 */

public class PhotoGalleryFragment extends VisibleFragment {

    private static final String TAG = "PhotoGalleryFragment";

    private RecyclerView mPhotoRecyclerView;

    private List<GalleryItem> mItems = new ArrayList<>();
    
    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;
    
    public static PhotoGalleryFragment newInstance(){
        return new PhotoGalleryFragment();
    }

    private InputMethodManager mInputMethodManager;

    private SearchView mSearchView;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setRetainInstance(true);//设置为保留fragment
        setHasOptionsMenu(true); //让fragment接受菜单回调方法。
        updateItems();


        Handler responseHandler = new Handler(); //这个Handler会默认与主线程的Looper相关联（在onCreate()方法创建）
        mThumbnailDownloader = new ThumbnailDownloader<>(responseHandler);
        mThumbnailDownloader.setThumbnailDownloadListener(new ThumbnailDownloader.ThumbnailDownloadListener<PhotoHolder>() {
            @Override
            public void onThumnailDownloaded(PhotoHolder photoHolder, Bitmap bitmap) {
                Drawable drawable = new BitmapDrawable(getResources(),bitmap);
                photoHolder.bindDrawable(drawable);
            }
        });
        mThumbnailDownloader.start();
        mThumbnailDownloader.getLooper(); //在start()方法之后调用getLooper()方法是一种保证线程就绪的处理方式。可以避免潜在竞争。
        Log.i(TAG, "Background thread started");

        mInputMethodManager = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);

        mPhotoRecyclerView = (RecyclerView)v.findViewById(R.id.fragment_photo_gallery_recycler_view);
        mPhotoRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 3));

        setupAdapter();

        return v;
    }


    @Override
    public void onDestroyView(){
        super.onDestroyView();
        mThumbnailDownloader.clearQueue();
    }
    @Override
    public void onDestroy(){
        super.onDestroy();
        mThumbnailDownloader.quit(); //如不终止HandlerThread 它会一直运行下去。
        Log.i(TAG, "Background  destoyed");
    }
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater){
        super.onCreateOptionsMenu(menu,menuInflater);
        menuInflater.inflate(R.menu.fragment_photo_gallery,menu);

        MenuItem searchItem = menu.findItem(R.id.menu_item_search); //将MenuItem取出并保存在searchItem变量中。
        mSearchView = (SearchView)searchItem.getActionView(); //取出SearchView对象。


        //设置监听器
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {//提交搜索查询时。这个方法执行。
                Log.d(TAG, "onQueryTextSubmit: "+ s);
                QueryPreferences.setStoredQuery(getActivity(),s); //存储用户提交的查询信息。
                updateItems();
                hintSoftInputAndSearchField(); //隐藏键盘的方法
                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) { //搜索框的文字有变化时，这个方法执行。
                Log.d(TAG, "onQueryTextChange: " + s);
                return false;
            }
         });

        MenuItem toggleItem = menu.findItem(R.id.menu_item_toggle_polling);
        if (PollService.isServiceAlarmOn(getActivity())) {
            toggleItem.setTitle(R.string.stop_polling);
        } else {
            toggleItem.setTitle(R.string.start_polling);
        }
        mSearchView.setOnSearchClickListener(new View.OnClickListener() { //设置点击按钮时，显示之前搜索过的值
            @Override
            public void onClick(View v) {
                String query = QueryPreferences.getStoredQuery(getActivity());
                mSearchView.setQuery(query,false);
            }
        });


    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case R.id.menu_item_clear:
                QueryPreferences.setStoredQuery(getActivity(),null);
                updateItems();
                return true;
            case R.id.menu_item_toggle_polling:
                boolean shouldStartAlarm  = !PollService.isServiceAlarmOn(getActivity());
                PollService.setServiceAlarm(getActivity(),shouldStartAlarm);
                getActivity().invalidateOptionsMenu(); //刷新菜单项
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    private void updateItems(){//调用FetchItemsTask的封装方法。
        String query  = QueryPreferences.getStoredQuery(getActivity());
        new FetchItemsTask(query).execute();
    }
    private void setupAdapter(){
        if(isAdded()){ //isAdded()确认fragment已与目标activity相关联。
            mPhotoRecyclerView.setAdapter(new PhotoAdapter(mItems));
        }
    }
    private class PhotoHolder extends RecyclerView.ViewHolder{

        private ImageView mItemImageView;

        private GalleryItem mGalleryItem;

        public PhotoHolder(View itemView){
            super(itemView);

            mItemImageView = (ImageView)itemView.findViewById(R.id.fragment_photo_gallery_image_view);

//            itemView.setOnClickListener(this);
        }

        public void bindDrawable(Drawable drawable){
            mItemImageView.setImageDrawable(drawable);
        }

//        public void bindGalleryItem(GalleryItem galleryItem) {
//            mGalleryItem = galleryItem;
//        }

//        @Override
//        public void onClick(View v) {
//            Intent i = new Intent(Intent.ACTION_VIEW, mGalleryItem.getPhotoPageUri());
//            startActivity(i);
//        }
    }

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder>{

        private List<GalleryItem> mGalleryItems;

        public PhotoAdapter(List<GalleryItem> galleryItems){
            mGalleryItems = galleryItems;
        }

        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View view = inflater.inflate(R.layout.gallery_item, viewGroup,false);
            return new PhotoHolder(view);
        }

        @Override
        public void onBindViewHolder(PhotoHolder photoHolder, int position) {
            GalleryItem galleryItem = mGalleryItems.get(position);
//            photoHolder.bindGalleryItem(galleryItem);
            Drawable placeholder = getResources().getDrawable(R.drawable.bill_up_close);
            photoHolder.bindDrawable(placeholder);
            mThumbnailDownloader.queueThumbnail(photoHolder,galleryItem.getUrl());
        }

        @Override
        public int getItemCount() {
            return mGalleryItems.size();
        }
    }
    private class FetchItemsTask extends AsyncTask<Void,Void,List<GalleryItem>>{
        private String mQuery;

        public FetchItemsTask(String query){
            mQuery = query;
        }

        @Override
        protected List<GalleryItem> doInBackground(Void... parms){


            if(mQuery == null){ //
                return new FlickrFetcher().fetchRecentPhotos();
            }else {
                return new FlickrFetcher().searchPhotos(mQuery);
            }
        }

        @Override
        protected void onPostExecute(List<GalleryItem> items){
            mItems = items;
            setupAdapter();
        }
    }

    private void hintSoftInputAndSearchField(){
        View v = getActivity().getCurrentFocus();
        if(v == null){
            return;
        }
        mInputMethodManager.hideSoftInputFromWindow(v.getWindowToken(),InputMethodManager.HIDE_NOT_ALWAYS);
        mSearchView.setIconifiedByDefault(false);
        mSearchView.setIconifiedByDefault(true);
    }

}
