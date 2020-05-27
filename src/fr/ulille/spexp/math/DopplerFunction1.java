package fr.ulille.spexp.math;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.util.FastMath;

public class DopplerFunction1 implements UnivariateFunction {

    private double x;
    private double x0;
    private double w;
    private double a;

    public DopplerFunction1(double x, double x0, double w, double a){
        this.x = x;
        this.x0 = x0;
        this.w = w/2;
        this.a = a;
    }

    @Override
    public double value(double v) {
        double xx = (x-x0)/w;
        return -2*a*xx/w*FastMath.exp(-xx*xx);
    }
}
