package fr.ulille.spexp.fftprofile;

import fr.ulille.spexp.fx.Main;
import javafx.beans.property.*;

public class LineParameters {

    private Property<Integer> index;
    private Property<Double> frequency;
    private Property<Double> amplitude;

    private boolean isEditing;

    public LineParameters(int index, double freq, double amp){
        this.index = new SimpleObjectProperty<>(index);
        this.frequency = new SimpleObjectProperty<>(freq);
        this.frequency.addListener((observable, oldValue, newValue)->{
            if (isEditing) {
                return;
            }
            isEditing = true;
            if (newValue!=null) this.frequency.setValue(newValue);
            Main.mainfrm.getFitSpectrumPane().updateFrequency(this.index.getValue(),newValue);
            isEditing = false;
        });
        this.amplitude = new SimpleObjectProperty<>(amp);
        this.amplitude.addListener((observable, oldValue, newValue)->{
            if (isEditing) {
                return;
            }
            isEditing = true;
            if (newValue!=null) this.amplitude.setValue(newValue);
            Main.mainfrm.getFitSpectrumPane().updateAmplitude(this.index.getValue(),newValue);
            isEditing = false;
        });
    }

    public void setAmplitude(double amplitude) {
        this.amplitude.setValue(amplitude);
    }

    public void setIndex(int index) {
        this.index.setValue(index);
    }

    public void setFrequency(double frequency) {
        this.frequency.setValue(frequency);
    }

    public int getIndex() {
        return index.getValue();
    }

    public double getFrequency() {
        return frequency.getValue();
    }

    public double getAmplitude() {
        return amplitude.getValue();
    }

    public Property<Double> amplitudeProperty() {
        return amplitude;
    }

    public Property<Integer> indexProperty() {
        return index;
    }

    public Property<Double> frequencyProperty() {
        return frequency;
    }
}
