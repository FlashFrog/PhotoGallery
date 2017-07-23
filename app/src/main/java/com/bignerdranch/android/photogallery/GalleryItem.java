package com.bignerdranch.android.photogallery;

/**
 * Created by Leo on 2017/7/19.
 */
//模型对象类
public class GalleryItem {

    private String mCaption;

    private String mId;

    private String mUrl;

//    private String mOwner;

//    public String getOwner() {
//        return mOwner;
//    }
//
//    public void setOwner(String owner) {
//        mOwner = owner;
//    }

    @Override
    public String toString(){
        return mCaption;
    }

    public String getCaption() {
        return mCaption;
    }

    public void setCaption(String caption) {
        mCaption = caption;
    }

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        mId = id;
    }

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String url) {
        mUrl = url;
    }

//    //产生图片URL的方法
//    public Uri getPhotoPageUri() {
//        return Uri.parse("http://www.flickr.com/photos/")
//                .buildUpon()
//                .appendPath(mOwner)
//                .appendPath(mId)
//                .build();
//    }
}
