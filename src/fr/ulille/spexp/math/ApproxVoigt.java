package fr.ulille.spexp.math;;

import org.apache.commons.math3.util.FastMath;

/**
 * @author Roman Motiyenko
 * Approximation of the Voigt profile according to the equation taken from:
 * McLean, Mitchell, Swanston J. Electron. Spectr. Rel. Phenom. 69 (1994) 125-132
 */

public class ApproxVoigt {

    private static final double sln2 = Math.sqrt(Math.log(2.));
    private static final double spi = Math.sqrt(Math.PI);

    // Tables of coefficients taken from the paper
    public static final double[] A = {-1.2150, -1.3509, -1.2150, -1.3509};
    public static final double[] B = {1.2359, 0.3786, -1.2359, -0.3786};
    public static final double[] C = {-0.3085, 0.5906, -0.3085, 0.5906};
    public static final double[] D = {0.0210, -1.1858, -0.0210, 1.1858};

    private double x0;
    private double w;
    private double l;
    private double a;
    private double xx;
    private double yy;
    private double dxx;
    private short drv;

    /**
     * Class constructor
     * @param x0 - line central frequency
     * @param w - Doppler HWHM
     * @param l - Lorentz HWHM
     * @param a - amplitude (intensity) of the line
     * @param drv - derivative (0, 1 or 2)
     */
    public ApproxVoigt(double x0, double w, double l, double a, short drv){
        this.x0 = x0;
        this.w = w;
        this.l = l;
        this.a = a;
        this.drv = drv;
        if (drv>2||drv<0) this.drv = 0;
        dxx = sln2/w;
        yy = sln2*l/w;
    }

    /**
     * Calculation of the approximate Voigt function. Checked against exact Voigt integral
     * @param x - coordinate at which the function has to be calculated
     * @return Voigt(x)
     */
    private double d0(double x){
        xx = sln2*(x-x0)/w;
        double s = 0.;
        for (int i=0;i<A.length;i++){
            s+= (C[i]*(yy-A[i])+D[i]*(xx-B[i]))/((yy-A[i])*(yy-A[i])+(xx-B[i])*(xx-B[i]));
        }
        return s;
    }

    /**
     * Calculation of the 1st derivative approximate Voigt function. Checked against exact 1st
     * derivative Voigt integral
     * @param x - coordinate at which the function has to be calculated
     * @return dV(x)/dx
     */
    private double d1(double x){
        xx = sln2*(x-x0)/w;
        double s = 0.;
        for (int i=0;i<A.length;i++){
            double aa = yy-A[i];
            double bb = xx-B[i];
            s += (D[i]*(aa*aa-bb*bb)-2*aa*bb*C[i])/FastMath.pow(aa*aa+bb*bb,2);
        }
        return s*dxx;
    }

    /**
     * Calculation of the 2nd derivative approximate Voigt function. Checked against exact 2nd
     * derivative Voigt integral
     * @param x - coordinate at which the function has to be calculated
     * @return d^2V(x)/dx^2
     */
    private double d2(double x){
        xx = sln2*(x-x0)/w;
        double s = 0.;
        for (int i=0;i<A.length;i++){
            double aa = yy-A[i];
            double bb = xx-B[i];
            s -= 2*(D[i]*bb*(bb*bb-3*aa*aa)-C[i]*aa*(aa*aa-3*bb*bb))/FastMath.pow(aa*aa+bb*bb,3);
        }
        return s*dxx*dxx;
    }

    public double value(double x){
        switch (drv){
            case 0: return d0(x)*a*sln2/(spi*w);
            case 1: return d1(x)*a*sln2/(spi*w);
            default: return d2(x)*a*sln2/(spi*w);
        }
        //return s*a*sln2/(spi*w);
        //return d2(x)*a*sln2/(spi*w);
    }
}
