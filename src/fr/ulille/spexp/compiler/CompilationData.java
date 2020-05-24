package fr.ulille.spexp.compiler;

import javafx.beans.property.*;
import javafx.scene.paint.Color;

/**
 *
 * @author Roman Motiyenko
 */
public class CompilationData{
    private Property<String> filepath;
    private Property<Color> color;
    private Property<String> alias;
    private Property<Double> intensity;
    private BooleanProperty status;

    private boolean isEditing = false;

    public CompilationData(String filepath, String alias, Color color, double intensity, boolean status){
        this.color = new SimpleObjectProperty<Color>(color);
        this.filepath = new SimpleStringProperty(filepath);
        this.filepath.addListener((observable, oldValue, newValue)->{
            if (isEditing) {
                return;
            }
            isEditing = true;
            if (newValue!=null) this.filepath.setValue(newValue);
            isEditing = false;
        });
        this.alias = new SimpleStringProperty(alias);
        this.alias.addListener((observable, oldValue, newValue)->{
            if (isEditing) {
                return;
            }
            isEditing = true;
            if (newValue!=null) this.alias.setValue(newValue);
            isEditing = false;
        });

        this.intensity = new SimpleObjectProperty<Double>(intensity);
        this.intensity.addListener((observable, oldValue, newValue)->{
            if (isEditing) {
                return;
            }
            isEditing = true;
            if (newValue!=null) this.intensity.setValue(newValue);
            isEditing = false;
        });
        this.status = new SimpleBooleanProperty(status);
    }

    public void setColor(int rgb){
        final int red = (rgb >> 16) & 0xFF;
        final int green = (rgb >> 8) & 0xFF;
        final int blue = rgb & 0xFF;
        Color col = Color.rgb(red, green, blue);
        this.color.setValue(col);
    }

    public void setFilepath(String filepath){
        this.filepath.setValue(filepath);
    }

    public void setAlias(String alias){ this.alias.setValue(alias); }

    public void setIntensity(double intensity){this.intensity.setValue(intensity); }

    public int getColor(){
        final int red = Math.round((float) this.color.getValue().getRed()* 255) << 16;
        final int green = Math.round((float) this.color.getValue().getGreen()* 255) << 8;
        final int blue = Math.round((float) this.color.getValue().getBlue()* 255) ;
        return red+green+blue;
    }

    public String getPath(){ return this.filepath.getValue(); }

    public String getAlias(){ return this.alias.getValue(); }

    public double getIntensity() {return this.intensity.getValue(); }

    public boolean getStatus() {return this.status.get(); }

    public Property<String> getPathProperty() {return this.filepath; }

    public Property<String> getAliasProperty() {return this.alias; }

    public Property<Color> getColorProperty() { return this.color; }

    public Property<Double> getIntensityProperty() {return this.intensity; }

    public BooleanProperty getStatusProperty() {return this.status; }

}