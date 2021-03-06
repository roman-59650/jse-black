package fr.ulille.spexp.math;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.util.FastMath;

public class VoigtFunction2 implements UnivariateFunction {

    private double a;
    private double w;
    private double l;
    private double x0;
    private double x;

    public VoigtFunction2(double x, double x0, double w, double l, double a){
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
  CC:=2*y*ln(2)*sqrt(ln(2))/(sqr(dowidth)*dowidth*pi*sqrt(pi));
  Result:=amp*CC*(sqr(y)-3*sqr(x-t))/Power(sqr(y)+sqr(x-t),3);
     */

    @Override
    public double value(double t) {
        double xx = (x-x0)/w;
        double yy = l/w;
        double c = 2E+5*yy/(FastMath.pow(w,3)*FastMath.pow(FastMath.PI,3.));
        return FastMath.exp(-t*t)*c*a*(yy*yy-3*(xx-t)*(xx-t))/FastMath.pow(yy*yy+(xx-t)*(xx-t),3);
    }
}
