package fr.ulille.spexp.fftprofile;

public class LorentzProfile implements ProfileFunction {
    @Override
    public double value(double dwidth, double lwidth, double T) {
        return Math.exp(-lwidth*Math.PI*T);
    }

    @Override
    public double dDvalue(double dwidth, double lwidth, double T) {
        return 0;
    }

    @Override
    public double dGvalue(double dwidth, double lwidth, double T) {
        return value(dwidth,lwidth,T)*(-Math.PI*T);
    }
}
