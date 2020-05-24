package fr.ulille.spexp.fx;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.util.StringConverter;

import java.net.URL;
import java.util.Locale;
import java.util.ResourceBundle;

public class FFTView implements Initializable {

    @FXML AnchorPane pane;
    @FXML Canvas canvas;
    @FXML Spinner<Double> spinner;

    private double hpfcutoff;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        pane.setId("fftpane");
        canvas.setId("fftcanvas");

        SpinnerValueFactory<Double> spf = new SpinnerValueFactory.DoubleSpinnerValueFactory(0,1,0,0.01);

        spf.setConverter(new StringConverter<Double>() {
            @Override
            public String toString(Double value) {
                if (value == null) {
                    return "";
                }
                return String.format(Locale.US,"%.2f",value);
            }

            @Override
            public Double fromString(String string) {
                // If the specified value is null or zero-length, return null
                if (string == null) {
                    return null;
                }
                string = string.trim();
                if (string.length() < 1) {
                    return null;
                }
                // Perform the requested parsing
                return Double.parseDouble(string);
            }
        });

        spinner.setValueFactory(spf);
    }

    public void setHPFcutoff(double hpfcutoff) {
        this.hpfcutoff = hpfcutoff;
        spinner.getValueFactory().setValue(this.hpfcutoff);
    }

    public double getHPFcutoff() {
        hpfcutoff = spinner.getValue();
        return hpfcutoff;
    }
}
