package fr.ulille.spexp.math;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.integration.SimpsonIntegrator;

public class VoigtFunctionInt implements UnivariateFunction {

    private SimpsonIntegrator simpsonIntegrator;
    private UnivariateFunction voigtFunction;
    private UnivariateFunction dopplerFunction;
    private double l;

    public VoigtFunctionInt(int drv, double x, double x0, double w, double l, double a){
        simpsonIntegrator = new SimpsonIntegrator();
        this.l = l;
        switch (drv){
            case 0: voigtFunction = new VoigtFunction0(x,x0,w,l,a); dopplerFunction = new DopplerFunction0(x,x0,w,a); break;
            case 1: voigtFunction = new VoigtFunction1(x,x0,w,l,a); dopplerFunction = new DopplerFunction1(x,x0,w,a); break;
            case 2: voigtFunction = new VoigtFunction2(x,x0,w,l,a); dopplerFunction = new DopplerFunction2(x,x0,w,a);
        }

    }

    private double simpson(UnivariateFunction f, double xmin, double xmax, int n){
        double h = ((xmax-xmin)/n)/2;
        double r = f.value(xmin);
        for (int i=1;i<2*n-1;i++){
            if (i%2!=0) r+=4*f.value(xmin+i*h);
            else r+=2*f.value(xmin+i*h);
        }
        return (r+f.value(xmax))*h/3;
    }

    @Override
    public double value(double x) {
        if (l==0)
            return dopplerFunction.value(x);
        else
            return simpsonIntegrator.integrate(10000,voigtFunction,-5,5);
    }
}
