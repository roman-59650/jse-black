/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.ulille.spexp.data;

import java.util.ArrayList;

/**
 *
 * @author roman
 */
public class PeakList {

    private ArrayList peakFrequency;
    private ArrayList peakIntensity;
    private boolean isVisible = false;
    public enum PeakViewMode {SimpleView, DbView}
    private ArrayList viewMode;
    private ArrayList peakStatus;
    private int peakSelected;
    private ArrayList peakId;


    public PeakList(){
        peakFrequency = new ArrayList();
        peakIntensity = new ArrayList();
        viewMode = new ArrayList();
        peakStatus = new ArrayList();
        peakId = new ArrayList();
        peakSelected = -1;
    }

    public void add(int freq, double ints, boolean stat, int id, PeakViewMode view){
        peakFrequency.add(freq);
        peakIntensity.add(ints);
        viewMode.add(view);
        peakStatus.add(stat);
        peakId.add(id);
    }

    public int getFrequency(int i){
        return (Integer)peakFrequency.get(i);
    }

    public double getIntensity(int i){
        return (Double)peakIntensity.get(i);
    }

    public PeakViewMode getViewMode(int i){
        return (PeakViewMode)viewMode.get(i);
    }

    public boolean getStatus(int i){
        return (Boolean)peakStatus.get(i);
    }

    public int getLength(){
        return peakFrequency.size();
    }

    public int getPeakId(int i){
        return (Integer)peakId.get(i);
    }

    public void clearAll(){
        peakFrequency.clear();
        peakIntensity.clear();
        viewMode.clear();
        peakStatus.clear();
        peakId.clear();
    }

    public void setFrequency(int i, double freq){
        peakFrequency.set(i, freq);
    }

    public void setIntensity(int i, double ints){
        peakIntensity.set(i, ints);
    }

    public void setViewMode(int i, PeakViewMode view){
        viewMode.set(i, view);
    }

    public void setStatus(int i, boolean stat){
        peakStatus.set(i, stat);
    }

    public void setPeakId(int i, int id){
        peakId.set(i, id);
    }

    public void setVisible(boolean Visible){
        isVisible = Visible;
    }

    public boolean getVisible(){
        return isVisible;
    }

    public void setSelected(int peak){
        peakSelected = peak;
    }

    public int getSelected(){
        return peakSelected;
    }

    public boolean getStatusSelected(){
        return getStatus(peakSelected);
    }

}
