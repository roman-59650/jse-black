/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.ulille.spexp.data;

/**
 *
 * @author Roman
 */
public class PredictRowData extends Transition {

    protected String species;
    protected double freq;
    protected double intsy;
    protected int color;
    protected int id;

    public PredictRowData(DbFormat format){
        super(format);
    };

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setSpecies(String species){
        this.species = species;
    }

    public String getSpecies(){
        return this.species;
    }

    public void setFrequency(double value){
        this.freq = value;
    }

    public double getFrequency(){
        return this.freq;
    }

    public void setIntensity(double value){
        this.intsy = value;
    }

    public double getIntensity(){
        return this.intsy;
    }

    public void setColor(int color){
        this.color = color;
    }

    public int getColor(){
        return this.color;
    }

    public int getQnsLength(){
        return qnums.size();
    }

    @Override
    public String toString(){
        String str = "";
        for (int i=0;i<format.getLength();i++){
            str = str + qnums.get(i).toString()+" ";
        }
        str = str + " - ";
        for (int i=format.getLength();i<2*format.getLength();i++){
            str = str + qnums.get(i).toString()+" ";
        }
        return str;
    }

    public Transition getTransition(){
        return this;
    }
}
