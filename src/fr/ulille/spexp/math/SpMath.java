package fr.ulille.spexp.math;

import org.apache.commons.math3.analysis.ParametricUnivariateFunction;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.optimization.fitting.CurveFitter;
import org.apache.commons.math3.optimization.general.GaussNewtonOptimizer;

/**
 *
 * @author roman
 */

public abstract class SpMath {
    
    public static final double sln2 = Math.sqrt(Math.PI/2.);

    public static double f(double x, RealVector p){
        double x0 = p.getEntry(0);
        double a0 = p.getEntry(1);
        double a2 = p.getEntry(2);
        double a4 = p.getEntry(3);
        return a0+a2*sqr(x-x0)+a4*sqr(x-x0)*sqr(x-x0);
    }

    public static double dfa2(double x, RealVector p){
        double x0 = p.getEntry(0);
        return (x-x0)*(x-x0);
    }

    public static double dfa4(double x, RealVector p){
        double x0 = p.getEntry(0);
        return sqr(x-x0)*sqr(x-x0);
    }

    public static double dfx0(double x, RealVector p){
        double x0 = p.getEntry(0);
        double a2 = p.getEntry(2);
        double a4 = p.getEntry(3);
        return -2*a2*(x-x0)-4*a4*(x-x0)*sqr(x-x0);
    }

    public static double sqr(double x){
        return x*x;
    }
    
    public double gauss(double x, double x0, double y0, double w, double a){
        return y0+a/(w*sln2)*Math.exp(-2.*sqr((x-x0)/w));
    }
    
    public double gaussdy0(double x, double x0, double y0, double w, double a){
        return 1.0;
    }
    
    public double gaussdx0(double x, double x0, double y0, double w, double a){
        return 4.*a*(x-x0)/(Math.pow(w, 3.)*sln2)*Math.exp(-2.*sqr((x-x0)/w));
    }
    
    public double gaussdw(double x, double x0, double y0, double w, double a){
        double t1 = 4.*a*sqr(x-x0)/(Math.pow(w, 4.)*sln2);
        double t2 = a/(sqr(w)*sln2);
        return (t1-t2)*Math.exp(-2.*sqr((x-x0)/w));
    }
    
    public double gaussda(double x, double x0, double y0, double w, double a){
        return 1./(w*sln2)*Math.exp(-2.*sqr((x-x0)/w));
    }
    
}
