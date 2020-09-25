package com.example.radmapv2;

import java.util.Objects;

public class Antena {
    private String radio;
    private Double mcc;
    private Double net;
    private Double area;
    private Double cell;
    private Double unit;
    private Double lon;
    private Double lat;
    private Double range;
    private Double samples;

    public Antena() {
    }

    public Antena(String radio, Double mcc, Double net, Double area, Double cell, Double unit, Double lon, Double lat, Double range, Double samples) {
        this.radio = radio;
        this.mcc = mcc;
        this.net = net;
        this.area = area;
        this.cell = cell;
        this.unit = unit;
        this.lon = lon;
        this.lat = lat;
        this.range = range;
        this.samples = samples;
    }

    public String getRadio() {
        return radio;
    }

    public void setRadio(String radio) {
        this.radio = radio;
    }

    public Double getMcc() {
        return mcc;
    }

    public void setMcc(Double mcc) {
        this.mcc = mcc;
    }

    public Double getNet() {
        return net;
    }

    public void setNet(Double net) {
        this.net = net;
    }

    public Double getArea() {
        return area;
    }

    public void setArea(Double area) {
        this.area = area;
    }

    public Double getCell() {
        return cell;
    }

    public void setCell(Double cell) {
        this.cell = cell;
    }

    public Double getUnit() {
        return unit;
    }

    public void setUnit(Double unit) {
        this.unit = unit;
    }

    public Double getLon() {
        return lon;
    }

    public void setLon(Double lon) {
        this.lon = lon;
    }

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getRange() {
        return range;
    }

    public void setRange(Double range) {
        this.range = range;
    }

    public Double getSamples() {
        return samples;
    }

    public void setSamples(Double samples) {
        this.samples = samples;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Antena antena = (Antena) o;
        return Objects.equals(lon, antena.lon) &&
                Objects.equals(lat, antena.lat);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lon, lat);
    }
}
