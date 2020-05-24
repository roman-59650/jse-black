package fr.ulille.spexp.fftprofile;

import fr.ulille.spexp.fx.Main;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;

public class CommonParameters {

    private Property<String> name;
    private Property<Double> value;

    private boolean isEditing;

    public CommonParameters(String name, double value){
        this.name = new SimpleObjectProperty<>(name);
        this.value = new SimpleObjectProperty<>(value);

        this.value.addListener((observable, oldValue, newValue)->{
            if (isEditing) {
                return;
            }
            isEditing = true;
            if (newValue!=null) this.value.setValue(newValue);
            switch (this.name.getValue()){
                case "Lorentz width" : Main.mainfrm.getFitSpectrumPane().updateLorentzWidth(newValue); break;
                case "Doppler width" : Main.mainfrm.getFitSpectrumPane().updateDopplerWidth(newValue);
            }
            isEditing = false;
        });
    }

    public void setName(String name) {
        this.name.setValue(name);
    }

    public void setValue(Double value) {
        this.value.setValue(value);
    }

    public Property<Double> valueProperty() {
        return value;
    }

    public Property<String> nameProperty() {
        return name;
    }

    public String getName() {
        return name.getValue();
    }

    public Double getValue() {
        return value.getValue();
    }
}
