package fr.ulille.spexp.fftprofile;

import fr.ulille.spexp.math.RelativeChecker;
import org.apache.commons.math3.fitting.leastsquares.*;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.optim.ConvergenceChecker;
import org.apache.commons.math3.optim.SimplePointChecker;
import org.apache.commons.math3.util.Pair;

import java.util.Locale;

public class ProfileFitter {

    private final static double relativeThreshold = 1.0E-6;
    private final static double absoluteThreshold = 1.0E-6;
    private final static int maxIterations = 20;
    private final static int maxEvaluations = 20;

    private LineProfile fitprofile;
    private double[] y;
    private MultivariateJacobianFunction lineprofile;
    private LeastSquaresOptimizer.Optimum opt;
    private boolean isConverged;

    public ProfileFitter(LineProfile profile, double[] ydata){
        this.fitprofile = profile;
        this.y = ydata;
        int fitsize = ydata.length;
        isConverged = false;
        lineprofile = params -> {
            RealVector value = new ArrayRealVector(fitsize);
            int nlines = fitprofile.getLines();
            RealMatrix jacobian = new Array2DRowRealMatrix(fitsize,2*nlines+4);
            fitprofile.setProfileParameters(params);
            double[] val = fitprofile.getProfile();
            double[] dw = fitprofile.getLinewidthDerivative();
            double[][] ddx = new double[nlines][fitsize];
            double[][] dda = new double[nlines][fitsize];
            for (int i=0;i<nlines;i++) {
                double[] dx0 = fitprofile.getFrequencyDerivative(i);
                double[] da = fitprofile.getAmplitudeDerivative(i);
                for (int j=0;j<fitsize;j++){
                    ddx[i][j] = dx0[j];
                    dda[i][j] = da[j];
                }
            }
            double xv[] = fitprofile.getX(); // x values in profile units;
            for (int i=0;i<fitsize;i++){
                value.setEntry(i, val[i]);
                jacobian.setEntry(i,0,dw[i]);
                jacobian.setEntry(i,1,1);
                jacobian.setEntry(i,2, xv[i]);
                jacobian.setEntry(i,3, xv[i]*xv[i]);
                int k = 4;
                for (int j=0;j<nlines;j++) {
                    jacobian.setEntry(i, k, ddx[j][i]);
                    jacobian.setEntry(i, k+1, dda[j][i]);
                    k+=2;
                }
            }
            return new Pair<>(value, jacobian);
        };
    }

    public boolean fit(){
        double[] inipars = fitprofile.getProfileParameters();
        //SimplePointChecker check = new SimplePointChecker(relativeThreshold,absoluteThreshold);

        RelativeChecker check = new RelativeChecker();
        check.setMaxIterations(maxIterations);
        check.setTolerance(relativeThreshold);
        LeastSquaresProblem fit = new LeastSquaresBuilder().
                start(inipars).
                model(lineprofile).
                target(y).
                lazyEvaluation(false).
                maxEvaluations(maxEvaluations).
                maxIterations(maxIterations).
                checkerPair(check).
                build();
        GaussNewtonOptimizer gnopt = new GaussNewtonOptimizer(GaussNewtonOptimizer.Decomposition.SVD);
        opt = gnopt.optimize(fit);
        if (check.isConv()) {
            isConverged = true;
            fitprofile.setProfileParameters(opt.getPoint());

            for (int i=0;i<opt.getSigma(0.).getDimension();i++)
                System.out.println(String.format(Locale.US,"%f %f", opt.getPoint().getEntry(i),opt.getSigma(0).getEntry(i)));

        }
        return check.isConv();
    }

    public double[] getOptimizedProfile(){
        double[] yvfit;
        yvfit = lineprofile.value(opt.getPoint()).getFirst().toArray();
        return yvfit;
    }

    public RealVector getOptimizedParametersVector(){
        return opt.getPoint();
    }

    public RealVector getOptimizedParametersSigma(){
        return opt.getSigma(0.);
    }

    public double[] getOptimizedParameters(){
        double [] pars;
        pars = opt.getPoint().toArray();
        return pars;
    }

    public boolean isConverged() {
        return isConverged;
    }

    public GaussNewtonOptimizer.Optimum getOptimum(){
        return opt;
    }
}
