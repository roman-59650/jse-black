package fr.ulille.spexp.math;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.util.FastMath;

public class VoigtFunction1 implements UnivariateFunction {
    private double a;
    private double w;
    private double l;
    private double x0;
    private double x;

    public VoigtFunction1(double x, double x0, double w, double l, double a){
        this.x0 = x0;
        this.w = w;
        this.l = l;
        this.a = a;
        this.x = x;
    }

    public void setX(double x) {
        this.x = x;
    }

    /*
  x:=sqrt(ln(2))*(w-w0)/dowidth;
  y:=sqrt(ln(2))*lowidth/dowidth;
  BB:=-2*y*ln(2)/(sqr(dowidth)*pi*sqrt(pi));
  Result:=amp*BB*(x-t)/sqr(sqr(y)+sqr(x-t));
     */

    @Override
    public double value(double t) {
        double xx = (x-x0)/w;
        double yy = l/w;
        //double c = -2E+5*yy/(FastMath.pow(w,2)*FastMath.pow(FastMath.PI,1.5));
        double c =2E+5; // just a coefficient, simplified version to avoid yy=0 in case if l = 0
        return FastMath.exp(-t*t)*c*a*(xx-t)/FastMath.pow(yy*yy+(xx-t)*(xx-t),2);
    }

}
