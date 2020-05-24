package fr.ulille.spexp.fftprofile;

public interface ProfileFunction {

    double ln2 = Math.log(2);
    double sqln2 = Math.sqrt(ln2);
    double cf = Math.PI/sqln2;

    double value(double a, double b, double c);
    double dDvalue(double a, double b, double c);
    double dGvalue(double a, double b, double c);
}
