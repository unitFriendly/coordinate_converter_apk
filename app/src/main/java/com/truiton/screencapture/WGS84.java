package com.truiton.screencapture;

public class WGS84 {

    public WGS84() {

    };

    public WGS84(double _longt, double _latt, double _h)
    {
        longt = _longt;
        latt = _latt;
        h = _h;
    }

    public double X_WGS84 = 0;
    public double Y_WGS84 = 0;
    public double Z_WGS84 = 0;

    public double longt, latt, h;
}
