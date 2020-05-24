package fr.ulille.spexp.math;

import org.apache.commons.math3.optim.ConvergenceChecker;
import org.apache.commons.math3.optim.PointVectorValuePair;
import org.apache.commons.math3.util.FastMath;

public class RelativeChecker implements ConvergenceChecker<PointVectorValuePair> {

    private boolean conv;
    private double tol;
    private int maxiter;

    public boolean isConv() {
        return conv;
    }

    public void setTolerance(double tol){
        this.tol = tol;
    }

    public void setMaxIterations(int maxiter){
        this.maxiter = maxiter;
    }

    @Override
    public boolean converged(int i, PointVectorValuePair previous, PointVectorValuePair current) {
        if (i>=maxiter) {
            return true;
        } else {
            conv = false;
            double[] p = previous.getPointRef();
            double[] c = current.getPointRef();
            int np = 0;
            for (int j = 0; j < p.length; j++) {
                if (c[j] != 0)
                    if (FastMath.abs((c[j] - p[j]) / c[j]) < tol) np++;
            }
            if (np == p.length) conv = true;
            else conv = false;
            return conv;
        }
    }

}
