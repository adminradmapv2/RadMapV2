package com.example.radmapv2;

public class PositionToIntensity {
    private double lat;
    private double lon;
    private double intens;

    public PositionToIntensity(double lat, double lon, double intens) {
        this.lat = lat;
        this.lon = lon;
        this.intens = intens;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public double getIntens() {
        return intens;
    }

    public void setIntens(double intens) {
        this.intens = intens;
    }
}
