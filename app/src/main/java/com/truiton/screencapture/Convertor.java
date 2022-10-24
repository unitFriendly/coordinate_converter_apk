package com.truiton.screencapture;
import static java.lang.Math.cos;
import static java.lang.Math.pow;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;

public class Convertor {

    public Convertor() {

    };

    double radian =  Math.PI / 180;

    double deltaX = -0.013; //m
    double deltaY = 0.106; //m
    double deltaZ = 0.022; //m

    double omegaX = -0.00230; // radian
    double omegaY = 0.00354;// radian
    double omegaZ = 0.00421;// radian

    double scaleM = -0.000000008; // масштабный коэффициент

    double alpha = 1 / 298.257223563;

    double bigHalfOS_A = 6378137;

    double koefficient = 0.999999992;



    double [][]  secondMatrix= {
        {-0.013},
        {0.106},
        {0.022}
    };



    public PZ90 WGS84toPZ90(WGS84 wgs84)
    {
        double [][] firstMatrix = {//+
                {1 * koefficient, -2.041066 * pow(10, -8) * koefficient, -1.716240 * pow(10, -8) * koefficient},
                {2.041066 * pow(10, -8) * koefficient, 1 * koefficient, -1.115071 * pow(10, -8) * koefficient},
                {1.716240 * pow(10, -8) * koefficient, 1.115071 * pow(10, -8) * koefficient, 1 * koefficient}
        };

        double [] secondM = { -0.013 , 0.106, 0.022 };

        PZ90 pz90 = new PZ90();

        double [][] resultMatrix = { {0}, {0}, {0} };

        double[] mainMatrix = { wgs84.X_WGS84, wgs84.Y_WGS84, wgs84.Z_WGS84 };

        double[][] mainMatrixXYZ = {
        {wgs84.X_WGS84},
        {wgs84.Y_WGS84},
        {wgs84.Z_WGS84}
    };



        // работает и похер
        int m = firstMatrix.length;
        int n =mainMatrixXYZ[0].length;
        int o = mainMatrixXYZ.length;

        for (int row = 0; row < m; row++) {
            for (int col = 0; col < n; col++) {
                // Multiply the row of A by the column of B to get the row, column of product.
                for (int inner = 0; inner < o; inner++) {
                    resultMatrix[row][col] += firstMatrix[row][inner] * mainMatrixXYZ[inner][col];
                }
            }
        }

        //for (int row = 0; row < 3; row++) {
        //
        //    for (int col = 0; col < 3; col++) {
        //            resultMatrix[row][col] += firstMatrix[row][col] * mainMatrix[col];

        //        //std::cout << product[row][col] << "  ";
        //    }
        //    //std::cout << "\n";
        //}

        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 1; j++)
                resultMatrix[i][j] = resultMatrix[i][j] + secondM[j];

        pz90.X_PZ90 = resultMatrix[0][0];
        pz90.Y_PZ90 = resultMatrix[1][0];
        pz90.Z_PZ90 = resultMatrix[2][0];

        return pz90;
    }

    public WGS84 WGS84ToWGS84_XYZ(WGS84 wgs84)
    {
        double e = (2 * alpha) - (alpha * alpha); //+

        double N = bigHalfOS_A / (sqrt((1 - (e) * pow(sin(wgs84.longt * radian), 2))));

        double sinw = sqrt((1 - (e)*pow(sin(wgs84.longt * radian), 2)));

        double tempCosLat = cos(wgs84.longt * radian);
        double tempCoslong = cos(wgs84.latt * radian) ;
        double tempSinLat = sin(wgs84.longt * radian) ;
        double tempSinLongt = sin(wgs84.latt* radian) ;


        wgs84.X_WGS84 = (N + wgs84.h) * tempCosLat * tempCoslong;
        wgs84.Y_WGS84 = (N + wgs84.h) * tempCosLat * tempSinLongt;
        wgs84.Z_WGS84 = ((1 - e) * N + wgs84.h) * tempSinLat;

        return wgs84;
    }
}
