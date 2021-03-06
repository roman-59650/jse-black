package fr.ulille.spexp.data;

import java.util.Locale;

/**
 *
 * @author roman
 */
public class AssignRowData extends PredictRowData {

    private double wght;
    private int outputFormat;
    public static final int GENERAL_FMT = 0;
    public static final int ASFIT_FMT = 1;
    public static final int SPFIT_FMT = 2;
    
    public AssignRowData(DbFormat dbformat){
        super(dbformat);
        this.outputFormat = 0;
    }
    
    public void setWeight(double value){
        wght = value;
    }
    
    public double getWeight(){
        return this.wght;
    }
    
    public void setOutputFormat(int value){
        this.outputFormat = value;
    }

    private String getOutputFormat(int i){
        String str = "";
        if (format.getDataType(i) == DbFormat.qnDataType.IntData) str = "%3d";
        if (format.getDataType(i) == DbFormat.qnDataType.FloData) str = "%5.1f";
        if (format.getDataType(i) == DbFormat.qnDataType.StrData) str = "%s";
        return str;
    }

    @Override
    public String toString(){
        String str = "";
        if (outputFormat == 0){
            str = str + String.format("%8s :", this.species);
            for (int i=0;i<format.getLength();i++){  
                str = str + String.format(Locale.US,getOutputFormat(i), qnums.get(i));
            }
            str = str + "";
            for (int i=format.getLength();i<2*format.getLength();i++){
                str = str + String.format(Locale.US,getOutputFormat(i), qnums.get(i));
            }
            str = str + String.format(Locale.US,"%15.3f %10.3f", freq, wght);
            if (this.intsy!=0) str = str + String.format(Locale.US, " %.2E", intsy);
        }
        if (outputFormat == 1){
            for (int i=0;i<format.getLength();i++){  
                str = str + String.format("%5d", qnums.get(i));
            }
            str = str + "";
            for (int i=format.getLength();i<2*format.getLength();i++){
                str = str + String.format("%5d", qnums.get(i));
            }
            str = str + String.format(Locale.US,"%20.3f %15.6f", freq, wght);
            if (this.intsy!=1.||this.intsy!=0) str = str + String.format(Locale.US, "%5d %-10.1E", 2, intsy);
            else str = str + String.format(Locale.US, "%5d", 1);
        }
        if (outputFormat == 2){
            for (int i=0;i<format.getLength();i++){  
                str = str + String.format("%3d", qnums.get(i));
            }
            str = str + "";
            for (int i=format.getLength();i<2*format.getLength();i++){
                str = str + String.format("%3d", qnums.get(i));
            }
            int freelen = 40 - 6*format.getLength()+15;
            String freeform = "%" + Integer.toString(freelen) + ".3f ";
            str = str + String.format(Locale.US,freeform+"%12.5f", freq, wght);
            str = str + String.format(Locale.US, " %3.1E", intsy);
        }
        if (outputFormat == 3){
            str += String.format(Locale.US,"%11.3f ", freq);
            for (int i=0;i<format.getLength();i++){
                str += String.format("%5d", qnums.get(i));
            }
            str = str + "   ";
            for (int i=format.getLength();i<2*format.getLength();i++){
                str += String.format("%5d", qnums.get(i));
            }
            str += String.format(Locale.US,"%7d", 1);
            str += String.format(Locale.US,"%10.3f", wght);
        }
        if (outputFormat == 4){
            str += String.format("%2d",qnums.get(0));
            str += String.format("%4d",qnums.get(1));
            str += String.format("%4d",qnums.get(2));
            str += String.format("%4d",qnums.get(4));
            str += String.format("%4d",qnums.get(5));
            str += String.format("%4d",qnums.get(6));
            str += String.format("%9d",qnums.get(3));
            str += String.format("%4d",qnums.get(7));
            str += String.format(Locale.US,"%16.6f ", freq/1e+3);
        }
        if (outputFormat == 5){
            str += String.format(Locale.US,"%10.3f00 0", freq);
            str += String.format("%4d",qnums.get(0));
            str += String.format("%5d",qnums.get(1));
            if (qnums.get(3).equals(1)) str+="  +";
            else if (qnums.get(3).equals(-1)) str+="  -";
            else str+="   ";
            str += String.format("%3d  0",qnums.get(2));

            str += String.format("%4d",qnums.get(5));
            str += String.format("%5d",qnums.get(6));
            if (qnums.get(8).equals(1)) str+="  +";
            else if (qnums.get(8).equals(-1)) str+="  -";
            else str+="   ";
            str += String.format("%3d  1",qnums.get(7));
            str += String.format("%7d", Math.round(1000*(wght+1)));
            str += String.format("   %s",qnums.get(4));
        }

        return str;
    }

    public Transition getTransition(){
        return super.getTransition();
    }
    
}
