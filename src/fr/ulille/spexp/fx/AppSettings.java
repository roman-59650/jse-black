package fr.ulille.spexp.fx;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.util.StringConverter;

import java.io.*;
import java.net.URL;
import java.util.*;

public class AppSettings implements Initializable {

    private Properties properties;

    @FXML Button okButton;
    @FXML Button apButton;
    @FXML Button cnButton;
    @FXML ColorPicker spectrumColor;
    @FXML ColorPicker spBackgroundColor;
    @FXML ColorPicker predictionColor;
    @FXML ColorPicker prBackgroundColor;
    @FXML Spinner pwSpinner;
    @FXML TextField duText;
    @FXML Spinner lwSpinner;
    @FXML Spinner psSpinner;
    @FXML TextField ulwText;
    @FXML Spinner tkSpinner;
    @FXML CheckBox showXScaleBox;
    @FXML ChoiceBox derivativeSelect;
    @FXML CheckBox showProfileBox;

    boolean settingsApplied;

    private List<Color> standardColors;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        standardColors = new ArrayList<>();
        standardColors.add(Color.rgb(255,0,0));
        standardColors.add(Color.rgb(255,128,0));
        standardColors.add(Color.rgb(255,255,0));
        standardColors.add(Color.rgb(128,255,0));
        standardColors.add(Color.rgb(0,255,0));
        standardColors.add(Color.rgb(0,255,128));
        standardColors.add(Color.rgb(0,255,255));
        standardColors.add(Color.rgb(0,128,255));
        standardColors.add(Color.rgb(0,0,255));
        standardColors.add(Color.rgb(128,0,255));
        standardColors.add(Color.rgb(255,0,255));
        standardColors.add(Color.rgb(255,0,128));

        SpinnerValueFactory spinnerValueFactory1 = new SpinnerValueFactory.DoubleSpinnerValueFactory(0.5,4,1, 0.1);
        SpinnerValueFactory spinnerValueFactory2 = new SpinnerValueFactory.DoubleSpinnerValueFactory(0.1, 10, 1, 0.1);
        SpinnerValueFactory spinnerValueFactory3 = new SpinnerValueFactory.DoubleSpinnerValueFactory(4, 8, 7, 0.1);
        SpinnerValueFactory spinnerValueFactory4 = new SpinnerValueFactory.IntegerSpinnerValueFactory(5,25);

        derivativeSelect.getItems().addAll("0","1","2");
        derivativeSelect.getSelectionModel().selectLast();

        spectrumColor.getCustomColors().addAll(standardColors);
        predictionColor.getCustomColors().addAll(standardColors);

        StringConverter<Double> converter = new StringConverter<Double>() {
            @Override
            public String toString(Double value) {
                if (value == null) {
                    return "";
                }
                return String.format(Locale.US,"%.1f",value);
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
        };

        spinnerValueFactory1.setConverter(converter);
        spinnerValueFactory2.setConverter(converter);
        spinnerValueFactory3.setConverter(converter);

        pwSpinner.setValueFactory(spinnerValueFactory1);
        pwSpinner.setOnScroll(e->{
            pwSpinner.requestFocus();
            if (e.getDeltaY()<0) pwSpinner.decrement();
            else pwSpinner.increment();
        });
        lwSpinner.setValueFactory(spinnerValueFactory2);
        lwSpinner.setOnScroll(e->{
            lwSpinner.requestFocus();
            if (e.getDeltaY()<0) lwSpinner.decrement();
            else lwSpinner.increment();
        });
        psSpinner.setValueFactory(spinnerValueFactory3);
        psSpinner.setOnScroll(e->{
            psSpinner.requestFocus();
            if (e.getDeltaY()<0) psSpinner.decrement();
            else psSpinner.increment();
        });
        tkSpinner.setValueFactory(spinnerValueFactory4);

        okButton.setOnAction(actionEvent -> {
            updateSettings();
            if (!settingsApplied) Main.mainfrm.updateWindow();
            Main.mainfrm.settingsStage.hide();
        });

        apButton.setOnAction(event -> {
            updateSettings();
            Main.mainfrm.updateWindow();
            settingsApplied = true;
        });

        cnButton.setOnAction(event -> {
            Main.mainfrm.settingsStage.hide();
        });
    }

    private void updateSettings(){
        properties = Main.getProperties();
        properties.setProperty("spectrum color", spectrumColor.getValue().toString());
        properties.setProperty("background color", spBackgroundColor.getValue().toString());
        properties.setProperty("predictions color", predictionColor.getValue().toString());
        properties.setProperty("predictions background", prBackgroundColor.getValue().toString());
        properties.setProperty("pen width", pwSpinner.getValue().toString());
        properties.setProperty("lw multiplier", lwSpinner.getValue().toString());
        properties.setProperty("peak size", psSpinner.getValue().toString());
        properties.setProperty("default uncertainty", duText.getText());
        properties.setProperty("unres linewidth", ulwText.getText());
        properties.setProperty("show x-axis", String.valueOf(showXScaleBox.isSelected()));
        properties.setProperty("x-axis ticks",tkSpinner.getValue().toString());
        properties.setProperty("profile derivative", String.valueOf(derivativeSelect.getSelectionModel().getSelectedIndex()));
        properties.setProperty("profile color",predictionColor.getValue().toString());
        properties.setProperty("plot profile",String.valueOf(showProfileBox.isSelected()));
        File configFile = new File("config.xml");
        try {
            OutputStream outputStream = new FileOutputStream(configFile);
            properties.storeToXML(outputStream, "application settings");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
