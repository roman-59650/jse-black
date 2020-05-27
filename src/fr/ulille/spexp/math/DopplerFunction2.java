package fr.ulille.spexp.math;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.util.FastMath;

public class DopplerFunction2 implements UnivariateFunction {

    private double x;
    private double x0;
    private double w;
    private double a;

    public DopplerFunction2(double x, double x0, double w, double a){
        this.x = x;
        this.x0 = x0;
        this.w = w/2;
        this.a = a;
    }

    @Override
    public double value(double v) {
        double xx = (x-x0)/w;
        return a*FastMath.exp(-xx*xx)*2/w*(1-2*xx*xx);
    }
}
