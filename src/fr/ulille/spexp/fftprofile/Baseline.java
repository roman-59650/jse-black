package fr.ulille.spexp.fftprofile;

import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;

import java.util.Arrays;

public class Baseline {

    private double[] coeff;
    private double rms;
    private double[] left;
    private double[] right;
    private int b;
    private double[] x;

    public Baseline(double[] y, int[] bounds){
        Arrays.sort(bounds);
        left = Arrays.copyOfRange(y,bounds[0],bounds[1]);
        right = Arrays.copyOfRange(y,bounds[2],bounds[3]);
        int points = bounds[3]-bounds[0]+1;
        double ct = points/2.;
        x = new double[points];
        for (int i=0;i<points;i++){
            x[i] = -ct+i;
        }

        final WeightedObservedPoints obs = new WeightedObservedPoints();
        for (int i=0;i<left.length;i++){
            obs.add(x[i],left[i]);
        }
        b = bounds[2]-bounds[0];
        for (int i=0;i<right.length;i++){
            obs.add(x[i+b],right[i]);
        }
        final PolynomialCurveFitter fitter = PolynomialCurveFitter.create(2);
        coeff = fitter.fit(obs.toList());
        System.out.println(coeff[0]);
        System.out.println(coeff[1]);
        System.out.println(coeff[2]);

        calculateRMS();
    }

    private void calculateRMS(){
        double[] leftres = new double[left.length];
        double[] rightres = new double[right.length];
        for (int i=0;i<left.length;i++){
            leftres[i] = left[i] - coeff[0]+coeff[1]*x[i]+coeff[2]*x[i]*x[i];
        }
        for (int i=0;i<right.length;i++){
            rightres[i] = right[i] - coeff[0]+coeff[1]*x[i+b]+coeff[2]*x[i+b]*x[i+b];
        }

        double leftrms = Math.sqrt(Arrays.stream(leftres).map((v)->v*v).sum()/left.length);
        double rightrms = Math.sqrt(Arrays.stream(rightres).map((v)->v*v).sum()/right.length);
        rms = 0.5*(leftrms+rightrms);
    }

    public double[] getCoeff(){
        return coeff;
    }

    public double valueAt(double x){
        return coeff[0]+coeff[1]*x+coeff[2]*x*x;
    }

    public void setCoeff(double[] coeff) {
        this.coeff = coeff;
        calculateRMS();
    }

    public double getRms() {
        return rms;
    }
}
