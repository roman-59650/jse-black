package fr.ulille.spexp.fftprofile;

import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.special.BesselJ;

import java.util.ArrayList;

public class LineProfile {

    //public enum Function {Doppler, Lorentz, Voigt};
    //public enum Derivative {Zero, First, Second};

    private Function function;
    private Derivative derivative;
    private double lorentzWidth;
    private double dopplerWidth;
    private ArrayList<Double> frequency;
    private ArrayList<Double> amplitude;
    private int profileSize;
    private double data[];
    private double cfreq;
    private double odr;
    private ProfileFunction profileFunction;
    private double x[];
    private Baseline baseline;
    private double bfreq;
    private double sfreq;

    public LineProfile(Function func, Derivative drv, Baseline baseline, double dw, double lw, int size, double bf, double sf){
        frequency = new ArrayList<>();
        amplitude = new ArrayList<>();
        this.function = func;
        this.derivative = drv;
        /*switch (drv){
            case Zero: odr = 0; break;
            case First: odr = 1; break;
            case Second: odr = 2; break;
        }*/
        odr = derivative.getValue();
        this.dopplerWidth = dw;
        this.lorentzWidth = lw;
        this.profileSize = size;
        data = new double[4*size];
        x = new double[size];
        cfreq = size/2.;
        for (int i=0;i<size;i++)
            x[i] = -size/2.+i;
        profileFunction = null;
        switch (function){
            case Doppler: profileFunction = new DopplerProfile(); break;
            case Voigt: profileFunction = new VoigtProfile(); break;
            case Lorentz: profileFunction = new LorentzProfile();
        }
        this.baseline = baseline;
        this.bfreq = bf;
        this.sfreq = sf;
    }

    public void addLine(double freq, double ampl){
        frequency.add(freq);
        amplitude.add(ampl);
        updateProfile();
    }

    public void removeLine(int index){
        if (index<frequency.size()){
            frequency.remove(index);
            amplitude.remove(index);
            updateProfile();
        }
    }

    private void updateProfile(){
        DoubleFFT_1D fft = new DoubleFFT_1D(2*profileSize);
        for (int i=0;i<data.length;i++)
            data[i] = 0.;
        double t = 0;
        double dt = 1./profileSize;
        for (int i=0;i<profileSize;i++){
            double sum = 0;
            for (int k=0;k<frequency.size();k++)
                sum+= -amplitude.get(k)*Math.sin(Math.PI*t*(cfreq+frequency.get(k)));
            data[2*i] = sum*BesselJ.value(odr,t)*profileFunction.value(dopplerWidth,lorentzWidth,t);
            t+=dt;
        }
        fft.complexForward(data);
    }

    public double[] getProfile() {
        double out[] = new double[profileSize];
        for (int i=0;i<profileSize;i++){
            if (odr==0||odr==2) out[i] = baseline.valueAt(x[i])+data[2*i+1];
            else out[i] = baseline.valueAt(x[i])+data[2*i];
        }
        return out;
    }

    public double[] getX() {
        return x;
    }

    public double[] getLinewidthDerivative(){
        if (function==Function.Doppler)
            return getDopplerDerivative();
        else
            return getLorentzDerivative();
    }

    private double[] getDopplerDerivative(){
        double[] drv = new double[4*profileSize];
        double out[] = new double[profileSize];
        DoubleFFT_1D fft = new DoubleFFT_1D(2*profileSize);
        for (int i=0;i<drv.length;i++)
            drv[i] = 0.;
        double t = 0;
        double dt = 1./profileSize;
        for (int i=0;i<profileSize;i++){
            double sum = 0.;
            for (int k=0;k<frequency.size();k++)
                sum+= -amplitude.get(k)*Math.sin(Math.PI*t*(cfreq+frequency.get(k)));
            drv[2*i] = sum*BesselJ.value(odr,t)*profileFunction.dDvalue(dopplerWidth,lorentzWidth,t);
            t+=dt;
        }
        fft.complexForward(drv);
        for (int i=0;i<profileSize;i++){
            if (odr==0||odr==2) out[i] = drv[2*i+1];
            else out[i] = drv[2*i];
        }
        return out;
    }

    private double[] getLorentzDerivative(){
        double[] drv = new double[4*profileSize];
        double out[] = new double[profileSize];
        DoubleFFT_1D fft = new DoubleFFT_1D(2*profileSize);
        for (int i=0;i<drv.length;i++)
            drv[i] = 0.;
        double t = 0;
        double dt = 1./profileSize;
        for (int i=0;i<profileSize;i++){
            double sum = 0.;
            for (int k=0;k<frequency.size();k++)
                sum+= -amplitude.get(k)*Math.sin(Math.PI*t*(cfreq+frequency.get(k)));
            drv[2*i] = sum*BesselJ.value(odr,t)*profileFunction.dGvalue(dopplerWidth,lorentzWidth,t);
            t+=dt;
        }
        fft.complexForward(drv);
        for (int i=0;i<profileSize;i++){
            if (odr==0||odr==2) out[i] = drv[2*i+1];
            else out[i] = drv[2*i];
        }
        return out;
    }

    public double[] getAmplitudeDerivative(int index){
        double[] drv = new double[4*profileSize];
        double out[] = new double[profileSize];
        DoubleFFT_1D fft = new DoubleFFT_1D(2*profileSize);
        for (int i=0;i<drv.length;i++)
            drv[i] = 0.;
        double t = 0;
        double dt = 1./profileSize;
        for (int i=0;i<profileSize;i++){
            double sum = -Math.sin(Math.PI*t*(cfreq+frequency.get(index)));
            drv[2*i] = sum*BesselJ.value(odr,t)*profileFunction.value(dopplerWidth,lorentzWidth,t);
            t+=dt;
        }
        fft.complexForward(drv);
        for (int i=0;i<profileSize;i++){
            if (odr==0||odr==2) out[i] = drv[2*i+1];
            else out[i] = drv[2*i];
        }
        return out;
    }

    public double[] getFrequencyDerivative(int index){
        double[] drv = new double[4*profileSize];
        double out[] = new double[profileSize];
        DoubleFFT_1D fft = new DoubleFFT_1D(2*profileSize);
        for (int i=0;i<drv.length;i++)
            drv[i] = 0.;
        double t = 0;
        double dt = 1./profileSize;
        for (int i=0;i<profileSize;i++){
            double sum = -amplitude.get(index)*Math.cos(Math.PI*t*(cfreq+frequency.get(index)))*Math.PI*t;
            drv[2*i] = sum*BesselJ.value(odr,t)*profileFunction.value(dopplerWidth,lorentzWidth,t);
            t+=dt;
        }
        fft.complexForward(drv);
        for (int i=0;i<profileSize;i++){
            if (odr==0||odr==2) out[i] = drv[2*i+1];
            else out[i] = drv[2*i];
        }
        return out;
    }

    public void setFrequency(int i, double value){
        double rfreq = (value-bfreq)/sfreq-cfreq;
        frequency.set(i, rfreq);
        updateProfile();
    }

    public double getFrequency(int i){
        return (frequency.get(i)+cfreq)*sfreq+bfreq;
    }

    public void setAmplitude(int i, double value){
        amplitude.set(i, value);
        updateProfile();
    }

    public void setDopplerWidth(double width){
        dopplerWidth = width/sfreq;
        updateProfile();
    }

    public void setLorentzWidth(double width){
        lorentzWidth = width/sfreq;
        updateProfile();
    }

    public void setProfileParameters(RealVector params){
        if (function==Function.Doppler)
            dopplerWidth = params.getEntry(0);
        else
            lorentzWidth = params.getEntry(0);
        baseline.setCoeff(params.getSubVector(1,3).toArray());
        int i = 4;
        int idx = 0;
        while (i<params.getDimension()){
           frequency.set(idx,params.getEntry(i));
           amplitude.set(idx,params.getEntry(i+1));
           i+=2;
           idx++;
        }
        updateProfile();
    }

    public double[] getProfileParameters(){
        double[] pars = new double[2*frequency.size()+4];
        if (function==Function.Doppler)
            pars[0] = dopplerWidth;
        else
            pars[0] = lorentzWidth;
        for (int i=1;i<4;i++)
            pars[i] = baseline.getCoeff()[i-1];
        int i=4;
        int idx=0;
        while(idx<frequency.size()){
            pars[i] = frequency.get(idx);
            pars[i+1] = amplitude.get(idx);
            i+=2;
            idx++;
        }
        return pars;
    }

    public int getLines(){
        return frequency.size();
    }

    public LineParameters getLineParameters(int index){
        LineParameters pars = new LineParameters(index,0,0);
        pars.setFrequency((frequency.get(index-1)+cfreq)*sfreq+bfreq);
        pars.setAmplitude(amplitude.get(index-1));
        return pars;
    }

    public CommonParameters getCommonParameters(int index){
        CommonParameters coms = new CommonParameters("",0);
        switch (index){
            case 0: { coms.setName("Doppler width"); coms.setValue(dopplerWidth*sfreq);} break;
            case 1: { coms.setName("Lorentz width"); coms.setValue(lorentzWidth*sfreq);}
        }
        return coms;
    }

}
