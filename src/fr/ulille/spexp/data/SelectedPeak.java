package fr.ulille.spexp.data;

public class SelectedPeak {

    private double frequency;
    private boolean fitted;
    private int index;
    private int adbindex; // absolute db position: "ID" field of the table APP.PEAK
    private int rdbindex; // relative position within a current ResultSet
    private double[] xvalues;
    private double[] yvalues;

    public SelectedPeak() {
        frequency = 0.0;
        fitted = false;
        index = -1;
        adbindex = 0;
        rdbindex = 0;
        xvalues = null;
        yvalues = null;
    }

    public boolean isFitted(){
        return fitted;
    }

    public void setFitted(boolean fitted){
        this.fitted = fitted;
    }

    public void setFrequency(double frequency){
        this.frequency = frequency;
    }

    public double getFrequency(){
        return frequency;
    }

    public void setXYtable(double[] xv, double[] yv){
        this.xvalues = xv;
        this.yvalues = yv;
    }

    public double[] getXtable(){
        return xvalues;
    }

    public double[] getYtable(){
        return yvalues;
    }

    public void setIndex(int index){
        this.index = index;
    }

    public int getIndex(){
        return index;
    }

    public void setAbsoluteDbIndex(int index){ adbindex = index; }

    public void setRelativeDbIndex(int index){ rdbindex = index; }

    /**
     *
     * @return absolute db position of the peak - "ID" field in the APP.PEAKS table
     */
    public int getAbsoluteDbIndex(){ return adbindex; }

    /**
     *
     * @return relative position of the peak within current ResultSet
     */
    public int getRelativeDbIndex(){ return rdbindex; }

    public void clearSelected(){
        frequency = 0.;
        fitted = false;
        index = -1;
        adbindex = 0;
        rdbindex = 0;
        xvalues = null;
        yvalues = null;
    }
}
