package fr.ulille.spexp.data;

import java.util.ArrayList;

public class PeakData {

    private double peakfrequency;
    private double fittedfrequency;
    private boolean assigned;
    private ArrayList<Integer> references;
    private int id;
    private String stringreferences;

    public PeakData(int id, double pfr, double ffr, boolean stat, ArrayList<Integer> list){
        this.id = id;
        this.fittedfrequency = ffr;
        this.peakfrequency = pfr;
        this.assigned = stat;
        this.references = list;
        this.stringreferences = "";
    }

    public void setStringReferences(String stringreferences) {
        this.stringreferences = stringreferences;
    }

    public void setFittedfrequency(double fittedfrequency) {
        this.fittedfrequency = fittedfrequency;
    }

    public void setPeakfrequency(double peakfrequency) {
        this.peakfrequency = peakfrequency;
    }

    public void setReferences(ArrayList<Integer> references) {
        this.references = references;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setAssigned(boolean assigned) {
        this.assigned = assigned;
    }

    public double getFittedfrequency() {
        return fittedfrequency;
    }

    public double getPeakfrequency() {
        return peakfrequency;
    }

    public boolean isAssigned() {
        return assigned;
    }

    public ArrayList<Integer> getReferences() {
        return references;
    }

    public int getId() {
        return id;
    }

    public String getStringReferences() {
        return stringreferences;
    }
}
