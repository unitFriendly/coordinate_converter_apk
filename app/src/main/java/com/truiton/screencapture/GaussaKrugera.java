package com.truiton.screencapture;

import static java.lang.Math.cos;
import static java.lang.Math.pow;
import static java.lang.Math.sin;
import static java.lang.Math.tan;

public class GaussaKrugera {

    private final double Pi = 3.14159265358979; // Число Пи

    private final double compression = 298.257223563; // Сжатие эллипса
    private final double f = 1 / compression;

    private final double a = 6378137; // Большая полуось
    private final double b = a * (1 - f); // Малая полуось

    private final double e2 = 1 - ((b * b) / (a * a)); // Квадрат эксонтрицитета
    private final double e_ = ((a * a) / (b * b)) - 1; // Второй эксонтрицитет

    private double nu;

    private double X; // X по гауссу-крюгеру пз90.11
    private double Y; // Y по гауссу-крюгеру пз90.11

    private double B_degree; //широта в градусах
    private double L_degree; //долгота в градусах

    private double B_radian; //широта в радианах
    private double L_radian; //долгота в радианах

    private double E;

    private int n; // Номер зоны

    private double L0; // Середина текущей зоны

    private double l; // Разность текушей долготы от средней
    private final double k = (a - b) / (a + b);

    private final double C0_WGS84 = 6367449.1458;
    private final double C2_WGS84 = 16038.5086;
    private final double C4_WGS84 = 16.8326;
    private final double C6_WGS84 = 0.0220;

    private double S; // Длина дуги мереданы

    public GaussaKrugera()
    {

    };

    public void calcCoordinate(double _B, double _L)
    {
        B_degree = _B;
        L_degree = _L;

        B_radian = B_degree * Pi / 180;
        L_radian = L_degree * Pi / 180;

        nu = e_ * cos(B_radian);

        E = (6 + L_degree) / 6;

        n = (int)E;

        L0 = (6 * n - 3) * Pi / 180;
        l = L_radian - L0;

        double N = a * pow((1 - e2 * pow((sin(B_radian)), 2)), -0.5);

        double a2 = 0.5 * N * sin(B_radian) * cos(B_radian);

        double a4 = 0.04166 * N * sin(B_radian) * pow(cos(B_radian), 3) * (5 - pow(tan(B_radian), 2) + 9 * pow(nu, 2) + 4 * pow(nu, 4));

        double a6 = 1.0/720.0 * N * sin(B_radian) * pow(cos(B_radian), 5) * (61.0 - 58.0 * pow(tan(B_radian), 2) + pow(tan(B_radian), 4) + 270.0 * pow(nu, 2) - 330.0 * pow(nu, 2) * pow(tan(B_radian), 2));

        double a8 = 1.0/40320.0 * N * sin(B_radian) * pow(cos(B_radian), 7) * (1385.0 - 3111.0 * pow(tan(B_radian), 2) + 543.0 * pow(tan(B_radian), 4) - pow(tan(B_radian), 6));

        double b1 = N * cos(B_radian);

        double b3 = 1.0/6.0 * N * pow(cos(B_radian), 3) * (1.0 - pow(tan(B_radian), 2) + pow(nu, 2));

        double b5 = 1.0 / 120.0 * N * pow(cos(B_radian), 5) * (5.0 - 18.0 * pow(tan(B_radian), 2) + pow(tan(B_radian), 4) + 14.0 * pow(nu, 2) - 58.0 * pow(nu, 2) * pow(tan(B_radian), 2));

        double b7 = 1.0 / 5040.0 * N * pow(cos(B_radian), 7) * (61 - 479 * pow(tan(B_radian), 2) + 179 * pow(tan(B_radian), 4) - pow(tan(B_radian), 6));

        S = C0_WGS84 * B_radian - C2_WGS84 * sin(2 * B_radian) + C4_WGS84 * sin(4 * B_radian) - C6_WGS84 * sin(6 * B_radian);

        X = S + a2 * pow(l, 2) + a4 * pow(l, 4) + a6 * pow(l, 6) + a8 * pow(l, 8);

        double yTmp = b1 * l + b3 * pow(l, 3) + b5 * pow(l, 5) + b7 * pow(l, 7);

        Y = yTmp + (5 + 10 * n) * pow(10, 5);
    };

    public double getX() {
        return X;
    }

    public double getY(){
        return Y;
    }
}
