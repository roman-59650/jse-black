package fr.ulille.spexp.compiler;

public class MiscData {

    private String path;
    private double temp;
    private double mass;
    private double qfunc;
    private double icutoff;

    public MiscData(){
        path = "";
        temp = 300;
        mass = 30;
        qfunc = 1;
        icutoff = 1.0E-8;
    }

    public void setMass(double mass) {
        this.mass = mass;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setQfunc(double qfunc) {
        this.qfunc = qfunc;
    }

    public void setTemp(double temp) {
        this.temp = temp;
    }

    public void setIcutoff(double icutoff) {
        this.icutoff = icutoff;
    }

    public double getMass() {
        return mass;
    }

    public double getQfunc() {
        return qfunc;
    }

    public double getTemp() {
        return temp;
    }

    public String getPath() {
        return path;
    }

    public double getIcutoff() {
        return icutoff;
    }
}
