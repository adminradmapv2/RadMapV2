package com.example.radmapv2;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.ClusterItem;

public class MyItem implements ClusterItem {
    private final LatLng mPosition;
    private String mTitle;
    private String mSnippet;
    private BitmapDescriptor mIcon;

    public MyItem(double lat, double lng) { mPosition = new LatLng(lat, lng); }

    public MyItem(double lat, double lng, String title, String snippet, BitmapDescriptor icon) {
        mPosition = new LatLng(lat, lng);
        mTitle = title;
        mSnippet = snippet;
        mIcon = icon;
    }

    public MyItem(MarkerOptions markerOptions) {
        mPosition = markerOptions.getPosition();
        mTitle = markerOptions.getTitle();
        mSnippet = markerOptions.getSnippet();
        mIcon = markerOptions.getIcon();
    }

    @Override
    public LatLng getPosition() {
        return mPosition;
    }

    @Override
    public String getTitle() {
        return mTitle;
    }

    @Override
    public String getSnippet() {
        return mSnippet;
    }

    public BitmapDescriptor getIcon() {
        return mIcon;
    }

    public void setIcon(BitmapDescriptor icon) {
        this.mIcon = icon;
    }
}
