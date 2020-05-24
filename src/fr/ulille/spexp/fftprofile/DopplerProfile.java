package fr.ulille.spexp.fftprofile;

public class DopplerProfile implements ProfileFunction {
    @Override
    public double value(double dwidth, double lwidth, double T) {
        double ad = dwidth*cf;
        return Math.exp(-0.25*(ad*ad*T*T));
    }

    @Override
    public double dDvalue(double dwidth, double lwidth, double T) {
        double ad = dwidth*cf;
        return value(dwidth,lwidth,T)*(-0.5*ad*T*T)*cf;
    }

    @Override
    public double dGvalue(double dwidth, double lwidth, double T) {
        return 0;
    }
}
