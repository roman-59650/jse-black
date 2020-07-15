package fr.ulille.spexp.spectrum;

import fr.ulille.spexp.compiler.MiscData;
import fr.ulille.spexp.data.Database;
import fr.ulille.spexp.data.PeakList;
import fr.ulille.spexp.data.SelectedPeak;
import fr.ulille.spexp.math.FFTransformer;
import fr.ulille.spexp.math.RelativeChecker;
import fr.ulille.spexp.math.SpMath;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import org.jtransforms.fft.*;
import javafx.scene.canvas.GraphicsContext;;
import org.apache.commons.math3.fitting.leastsquares.*;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.random.RandomDataGenerator;
import org.apache.commons.math3.util.Pair;
import fr.ulille.spexp.fx.Main;

import java.sql.ResultSet;
import java.sql.SQLException;


public class Spectrum extends SpectrumFile {

	private static final int SMCYCLES = 5;       // number of smoothing cycles for baseline calculation
    private static final double FREQLO = 0.005;  // the fft filter cutoff frequencies; fixed values
    private static final double FREQHI = 0.165;  // make them variable in the final version
    private static final int NUMPOINTS = 7;      // minimum number of points to fit a peak frequency
    private double[] blData;
    private boolean doBaseline;
    private double pnoise = 0.0;
    private PeakList peaks;
    private SelectedPeak selectedPeak;
    private double[] xv;
    private double[] yv;
    private double[] xvfit;
    private double[] yvfit;
    private FFTransformer ffTransformer;

    public Spectrum(){
        doBaseline = false;
        peaks = new PeakList();
        selectedPeak = new SelectedPeak();
    }

    public SelectedPeak getSelectedPeak(){
        return selectedPeak;
    }

    public PeakList getPeakData(){
            return peaks;
        }

    public void setPeakData(PeakList peakData){
            peaks = peakData;
        }

    public double getPNoise(){
            return pnoise;
        }

   	public double getBaselineData(int i){
        if (i>=0&&i<dataPoints) return blData[i];
        else return 0;
	}

	public boolean isBaselined(){ return doBaseline; }

	public void suppBaseline(){
            for (int i=0;i<dataPoints;i++){
                yData[i]=yData[i]-blData[i];
            }
            scaleData();
            doBaseline=false;
   	}

   	/**
      * Calculation of the Savitzky-Golay smoothing-array coefficients.
      * Order of the smoothed derivative: 0
      * Order of the polynomial: 2 or 3
      * @param m number of smoothed points
      * @param s current index
      * @return
     */
    private double polyS03(int m, int s){
              return 3.0*(3.0*m*m+3.0*m-1.0-5.0*s*s)/((2.0*m+3.0)*(2.0*m+1.0)*(2.0*m-1.0));
    }

    /**
     * Calculation of the Savitzky-Golay smoothing-array coefficients.
     * Order of the smoothed derivative: 1
     * Order of the polynomial: 3 or 4
     * @param m number of smoothed points
     * @param s current index
     * @return
    */
    private double polyS13(int m, int s){
           double c1 = 5.0*(3.0*Math.pow(m,4.0)+6.0*Math.pow(m,2.0)*m-3.0*m+1.0)*s-
                            7.0*(3.0*m*m+3.0*m-1)*Math.pow(s,3.0);
           double c2 = (2.0*m+3.0)*(2.0*m+1.0)*(2.0*m-1.0)*(m+2.0)*(m+1.0)*m*(m-1.0);
           return 5.0*c1/c2;
    }

    /**
     * Baseline calculation using the Savitzky-Golay smoothing for a large number of points
     * The number smoothing cycles is defined by SMCYCLES constant
     * @param blPoints number of points for each smoothing cycle
     * @param ampLimit amplitude limit (+/-), allows better accounting for strong lines
     * @param zeroLevel zero level, allows centering datapoints Y values at 0 level
     * @param dobl boolean parameter to keep the calculated baseline plotted
     */
    public void setBaseline(int blPoints, 
                            double ampLimit,
                            double zeroLevel,
                            boolean dobl){
        if (!fileRead) return;
        blData = new double[dataPoints];
        double[] atmp = new double[2*dataPoints];
        int N1 = blPoints;
        double sum;
        // baseline
        System.arraycopy(yData, 0, blData, 0, dataPoints);
        System.arraycopy(blData, 0, atmp, 0, N1);
        for (int cyc=0;cyc<SMCYCLES;cyc++){
            System.arraycopy(blData, 0, atmp, 0, N1);
            for (int i=N1;i<dataPoints+N1;i++){
                atmp[i]=blData[i-N1];
            }
            for (int i=dataPoints+N1;i<dataPoints+2*N1;i++){
                atmp[i]=blData[i-2*N1];
            }
    
            for (int i=N1;i<dataPoints+N1;i++){
                sum=0.0;
                for (int j=-N1;j<=N1;j++){
                    double fact;// = atmp[i+j];
                    if ((atmp[i+j]<zeroLevel+ampLimit/2)&&(atmp[i+j]>zeroLevel-ampLimit/2))
                        fact = atmp[i+j];
                    else
                        fact = zeroLevel;
    	            sum=sum+polyS03(N1,j)*fact;//atmp[i+j];
                }
            blData[i-N1]=sum;
            }
        } // for cyc
        doBaseline = dobl;
    }
	  
    public void smoothData(int diff, int smPoints){
         if (!fileRead) return;
         double[] atmp = new double[2*dataPoints];
         int N1 = smPoints;
         double sum;
         System.arraycopy(yData, 0, atmp, 0, N1);
         for (int i=N1;i<dataPoints+N1;i++){
             atmp[i]=yData[i-N1];
         }
         for (int i=dataPoints+N1;i<dataPoints+2*N1;i++){
             atmp[i]=yData[i-2*N1];
         }
         for (int i=N1;i<dataPoints+N1;i++){
             sum=0.0;
             for (int j=-N1;j<=N1;j++){
                 if (diff==0) sum=sum+polyS03(N1,j)*atmp[i+j];
                 else
                 if (diff==1) sum=sum+polyS13(N1,j)*atmp[i+j];
          }
          yData[i-N1]=sum;
         }
         scaleData();
    }

    public void invertData(){
        if (!fileRead) return;
        for(int i=0;i<dataPoints;i++) yData[i] = -yData[i];
        scaleData();
    }

    private double noiseEstimate(double[] data, int ifup){
        DoubleFFT_1D fft = new DoubleFFT_1D(dataPoints);
        double[] atmp = new double[2*dataPoints];
        System.arraycopy(data, 0, atmp, 0, dataPoints);
        fft.realForwardFull(atmp);
        double rmean = 0.0;
        double imean = 0.0;
        for (int i=ifup;i<dataPoints/2;i++){
            rmean = rmean+atmp[2*i];
            imean = imean+atmp[2*i+1];
        }
        rmean = rmean/(dataPoints-ifup+1);
        imean = imean/(dataPoints-ifup+1);
        double stdev = 0.0;
        double re = 0.0;
        double im = 0.0;
        for (int i=ifup;i<dataPoints/2;i++){
            re = (atmp[2*i]-rmean);
            im = (atmp[2*i+1]-imean);
            stdev = stdev+Math.abs(re*re-im*im);
        }
        stdev = Math.sqrt(stdev/(dataPoints-ifup));
        // ***** generating new array with <mean> and <stdev>
        double[] btmp = new double[2*dataPoints];

        RandomDataGenerator rgen = new RandomDataGenerator();

        for(int i=1;i<dataPoints;i++){
            btmp[i] = rgen.nextGaussian(rmean, stdev);
        }
        for(int i=1;i<dataPoints/2;i++){
            btmp[2*(dataPoints-i)] = btmp[2*i];
            btmp[2*(dataPoints-i)+1] = -btmp[2*i+1];
        }
        btmp[0] = 0.0; btmp[1] = 0.0;
        fft.complexInverse(btmp, true);
        // ***** estimate of noise using new data after inverse fft
        rmean = 0.0;
        for(int i=0;i<dataPoints;i++){
            rmean = rmean+btmp[2*i];
        }
        rmean = rmean/dataPoints;
        stdev = 0.0;
        for(int i=0;i<dataPoints;i++){
            stdev = stdev+(btmp[2*i]-rmean)*(btmp[2*i]-rmean);
        }
        stdev = Math.sqrt(stdev/dataPoints);

        System.gc();

        return rmean + stdev;

    }

    private double[] filterData(double[] data, int ifdo, int ifup){
        double fup;
        double fdo;
        for(int i=1;i<dataPoints/2;i++){
            fup = (double)(i/ifup);
            fdo = (double)(ifdo/i);
            data[2*i]=data[2*i]/(1+Math.pow(fup,5.0))/(1+Math.pow(fdo,5.0));
            data[2*i+1]=data[2*i+1]/(1+Math.pow(fup,5.0))/(1+Math.pow(fdo,5.0));
            data[2*(dataPoints-i)] = data[2*i];
            data[2*(dataPoints-i)+1] = -data[2*i+1];
        }
        return data;
    }

    public void doFFT(){
        ffTransformer = new FFTransformer(yData,dataPoints);
    }

    public FFTransformer getFFTransformer(){
        return ffTransformer;
    }

    public int findPeaks(int sigma, Database database){
        int nump = 0;
        double hpffreq = Double.parseDouble(Main.getProperties().getProperty("hpf cutoff"));
        int ifup = (int)Math.round(hpffreq*dataPoints);
        pnoise = noiseEstimate(yData,ifup);
        //nsData = new double[dataPoints];
        //for (int i=0;i<dataPoints;i++) nsData[i]=pnoise*sigma;
        double noiseLevel = pnoise*sigma*2;
        double ddyp;
        double ddyn;
        int ppos = 0;
        int opos = 0;
        int p1, p2;
        ddyp = yData[1] - yData[0];
        int i = 2;
        do{
            ppos = 0;
            ddyn = yData[i]-yData[i-1];
            if ((ddyp>0.0)&(ddyn<0.0)) ppos = i-1;
            if (ddyn==0){
               p1 = i-1;
               do {
                 ddyn = yData[i]-yData[i-1];
                 i++;
               } while (ddyn==0);
               p2 = i;
               ppos = Math.round((p1+p2)/2);
            }
            ddyp = ddyn;
            i++;
            if (ppos!=0){
               if (yData[ppos]>noiseLevel){
                   if ((ppos-opos<10)&(opos!=0)) continue;
                   if (!database.getPeakInterval(xData[ppos], 0.2)){
                     peaks.add(ppos,yData[ppos],false,0, PeakList.PeakViewMode.SimpleView);
                     nump++;
                     opos = ppos;
                   }
               }
            }
        } while(i<dataPoints);
        peaks.setVisible(true);
        return nump;
    }

    public void clearPeaks(){
        peaks.clearAll();
        peaks.setVisible(false);
    }

    public double getDopplerWidth(double freq){
        double molMass = Main.mainfrm.getDatabase().getMiscData().getMass();
        double Temp = Main.mainfrm.getDatabase().getMiscData().getTemp();
        return 3.58e-7*freq*Math.sqrt(Temp/molMass);
    }

    private double peakFitParabola(int ipeakfr){
        if (ipeakfr<=0) return 0.0;
    
        double molMass = Main.mainfrm.getDatabase().getMiscData().getMass();
        double Temp = Main.mainfrm.getDatabase().getMiscData().getTemp();
        double freq = xData[ipeakfr];
        double lwmult = Double.parseDouble(Main.getProperties().getProperty("lw multiplier"));
        double sigmaDop = lwmult*3.58e-7*freq*Math.sqrt(Temp/molMass);
    
        int idf = ipeakfr-1;
        double df;
        if (idf>0) df = xData[ipeakfr]-xData[ipeakfr-1];
        else df = xData[ipeakfr+1]-xData[ipeakfr];
        long isigmaDop = Math.round(sigmaDop/df);
    
        if (isigmaDop<3) return 0.;
        int ixa = ipeakfr-(int)isigmaDop;
        int ixb = ipeakfr+(int)isigmaDop;
        if (ixa<0) ixa = 0;
        if (ixb>dataPoints-1) ixb = dataPoints-1;
        if (ixb-ixa<NUMPOINTS) return 0.;
    
        int npoints = ixb-ixa+1;
        xv = new double[npoints];
        yv = new double[npoints];
        xvfit = new double[npoints];
        yvfit = new double[npoints];
        for (int i=0;i<npoints;i++){
            xv[i] = xData[ixa+i]-xData[ipeakfr]; //ixa+i-ipeakfr;
            xvfit[i] = ixa+i; //xData[ixa+i];
            yv[i] = yData[ixa+i];
        }
    
        double dx = xv[npoints/2]-xv[0];
        double dy = yv[npoints/2]-yv[0];
        double[] inipars = {0,yData[ipeakfr],-dy/dx/dx,0};
    
        MultivariateJacobianFunction parabola = new MultivariateJacobianFunction(){
            @Override
            public Pair<RealVector, RealMatrix> value(RealVector params) {
                RealVector value = new ArrayRealVector(npoints);
                RealMatrix jacobian = new Array2DRowRealMatrix(npoints,4);
                for (int i=0;i<npoints;i++){
                    value.setEntry(i, SpMath.f(xv[i],params));
                    // the order parameter derivatives in Jacobian should follow the order of parameters in params
                    jacobian.setEntry(i,0,SpMath.dfx0(xv[i],params));
                    jacobian.setEntry(i,1,1);
                    jacobian.setEntry(i,2,SpMath.dfa2(xv[i],params));
                    jacobian.setEntry(i,3,SpMath.dfa4(xv[i],params));
                }
                return new Pair<RealVector, RealMatrix>(value, jacobian);
            }
        };
    
        RelativeChecker convergenceChecker = new RelativeChecker();
        convergenceChecker.setTolerance(1E-06);
        convergenceChecker.setMaxIterations(20);
    
        LeastSquaresProblem problem = new LeastSquaresBuilder().
                start(inipars).
                model(parabola).
                target(yv).
                lazyEvaluation(false).
                maxEvaluations(20).
                maxIterations(20).
                checkerPair(convergenceChecker).
                build();
        GaussNewtonOptimizer gnopt = new GaussNewtonOptimizer(GaussNewtonOptimizer.Decomposition.SVD);
        LeastSquaresOptimizer.Optimum opt = gnopt.optimize(problem);
    
        yvfit = parabola.value(opt.getPoint()).getFirst().toArray();
    
        if (selectedPeak.getIndex()>0){
            selectedPeak.setFitted(convergenceChecker.isConv());
            selectedPeak.setXYtable(xvfit,yvfit);
        }
    
        if (convergenceChecker.isConv())
            return xData[ipeakfr]+opt.getPoint().getEntry(0);
        else
            return 0.;
    }

    public int addPeaksDb(Database database){
        int length = peaks.getLength();
        int nump = 0;
        Main.mainfrm.statusbar.setProgress(0);
        Main.mainfrm.statusbar.setVisible(true);
        for (int i=0;i<length;i++){
            double fpos = xData[peaks.getFrequency(i)];
            if (peaks.getViewMode(i)==PeakList.PeakViewMode.SimpleView){
                  double fpeak = peakFitParabola(peaks.getFrequency(i));
                  database.insertValue(fpos, fpeak);
                  Main.mainfrm.statusbar.setProgress((double) i/length);
                  nump++;
            }
        }
        peaks.clearAll();
        Main.mainfrm.statusbar.setProgress(0);
        return nump;
    }

    public double[] getScreenXArray(GraphicsContext gc){
        double width = gc.getCanvas().getWidth();
        double [] xs = new double[xbLimit-xaLimit+1];
        double span = getScreenSpan();
        double xmin = getScreenFreqMin();
        for (int i=xaLimit;i<=xbLimit;i++){
            xs[i-xaLimit] = (xData[i]-xmin)*width/span;
        }
        return xs;
    }

    public double[] getScreenXArray(GraphicsContext gc, double[] a){
        double width = gc.getCanvas().getWidth();
        double [] xs = new double[a.length];
        double span = getScreenSpan();
        double xmin = getScreenFreqMin();
        for (int i=0;i<a.length;i++){
            xs[i] = (a[i]-xmin)*width/span;
        }
        return xs;
    }

    public double[] getScreenFrequenciesArray(){
        double [] xs = new double[xbLimit-xaLimit+1];
        for (int i=xaLimit;i<=xbLimit;i++){
            xs[i-xaLimit] = xData[i];
        }
        return xs;
    }

    public double getScreenXCoord(double f, GraphicsContext gc){
        double width = gc.getCanvas().getWidth();
        double span = xData[xbLimit]-xData[xaLimit];
        return (f-xData[xaLimit])*width/span;
    }

    public double getScreenXValue(GraphicsContext gc, int index){
        double width = gc.getCanvas().getWidth();
        double span = xData[xbLimit]-xData[xaLimit];
        return (xData[index]-xData[xaLimit])*width/span;
    }

    public double[] getScreenYArray(GraphicsContext gc){
        double height = gc.getCanvas().getHeight();
        double ymin = getYScreenMin();
        double yspan = getYScreenMax()-getYScreenMin();
        double [] ys = new double[xbLimit-xaLimit+1];
        for (int i=xaLimit;i<=xbLimit;i++){
            ys[i-xaLimit] = height - height*(yData[i]-ymin)/yspan;
            if (ys[i-xaLimit]>height) ys[i-xaLimit]=height;
            if (ys[i-xaLimit]<0) ys[i-xaLimit]=0;
        }
        return ys;
    }

    public double[] getScreenYArray(GraphicsContext gc, double[] a){
        double height = gc.getCanvas().getHeight();
        double ymin = getYScreenMin();
        double yspan = getYScreenMax()-getYScreenMin();
        double [] ys = new double[a.length];
        for (int i=0;i<a.length;i++){
            ys[i] = height - height*(a[i]-ymin)/yspan;
            if (ys[i]>height) ys[i]=height;
            if (ys[i]<0) ys[i]=0;
        }
        return ys;
    }

    public double getScreenYValue(GraphicsContext gc, int index){
        double height = gc.getCanvas().getHeight();
        double ymin = getYScreenMin();
        double yspan = getYScreenMax()-getYScreenMin();
        return height - height*(yData[index]-ymin)/yspan;
    }

    public double[] getScreenBArray(GraphicsContext gc){
        double height = gc.getCanvas().getHeight();
        double ymin = getYScreenMin();
        double yspan = getYScreenMax()-getYScreenMin();
        double [] bs = new double[xbLimit-xaLimit+1];
        for (int i=xaLimit;i<xbLimit;i++){
            bs[i-xaLimit] = height - height*(blData[i]-ymin)/yspan;
        }
        return bs;
    }

    public double getScreenYCoord(double y, GraphicsContext gc){
        double height = gc.getCanvas().getHeight();
        double ymin = getYScreenMin();
        double yspan = getYScreenMax()-getYScreenMin();
        return height - height*(y-ymin)/yspan;
    }

    public double singlePeakFit(){
        return peakFitParabola(selectedPeak.getIndex());
    }

    public void fitPeaks(Database database){
        int length = 0;
        ResultSet rs;
        database.getPeakData(getScreenFreqMin(),getScreenFreqMax());
        rs = database.getPeakResultSet();
        try {
            if (rs.last()) length = rs.getRow();
            for (int j=0;j<length;j++) {
                rs.absolute(j + 1);
                double freq = rs.getDouble("PEAK_FR");
                int id = rs.getInt("ID");
                long ipos = Math.round((getSize() - 1) * (freq - getFreqMin()) / getSpan());
                double gfr = peakFitParabola((int)ipos);
                if (gfr!=0) {
                    database.updatePeakFrequency(id,gfr);
                    //System.out.println(gfr);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public double insertPeak(double frequency, Stage stage){
        MiscData misc = Main.mainfrm.getDatabase().getMiscData();
        double lwmult = Double.parseDouble(Main.getProperties().getProperty("lw multiplier"));
        double doppler = lwmult*3.58e-7*frequency*Math.sqrt(misc.getTemp()/misc.getMass());
        int imin = (int) getDataIndex(frequency-doppler);
        int imax = (int) getDataIndex(frequency+doppler);
        double[] y = new double[imax-imin+1];
        System.arraycopy(yData,imin, y,0,imax-imin+1);
        int maxpos = 0;
        double max = y[0];
        for (int i=1;i<y.length;i++){
            if (y[i]>max){
                max = y[i];
                maxpos = i;
            }
        }
        if (maxpos==0||maxpos==y.length-1) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Peak Finder");
            alert.setHeaderText("Unable to find a peak!");
            alert.setContentText(null);
            alert.initOwner(stage);
            alert.showAndWait();
            return 0.;
        }

        double fpeak = peakFitParabola(imin+maxpos);
        double fpos = xData[imin+maxpos];
        Main.mainfrm.getDatabase().insertValue(fpos, fpeak);
        return fpos;
    }

}
