package fr.ulille.spexp.fx;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.util.StringConverter;

import java.net.URL;
import java.util.Locale;
import java.util.ResourceBundle;

public class PeakDialog implements Initializable {
    @FXML Spinner accSpinner;
    @FXML Label freqLabel;
    @FXML Button assignButton;

    private SpectrumPane spectrumPane;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        SpinnerValueFactory.DoubleSpinnerValueFactory spf =
                new SpinnerValueFactory.DoubleSpinnerValueFactory(0,10,0,0.005);
        spf.setConverter(new StringConverter<Double>() {
            @Override
            public String toString(Double value) {
                if (value == null) {
                    return "";
                }
                return String.format(Locale.US,"%.3f",value);
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

        accSpinner.setValueFactory(spf);
        freqLabel.setId("flab");
        freqLabel.setAlignment(Pos.CENTER_RIGHT);
        freqLabel.setWrapText(true);
    }

    public void onPeakFit(ActionEvent e){
        Tab tab = Main.mainfrm.tabpane.getSelectionModel().getSelectedItem();
        WorkPane workPane = (WorkPane) tab.getContent().lookup("#workpane");
        double fr = workPane.getSpectrum().singlePeakFit();
        System.out.println(fr);
        if (workPane.getSpectrum().getSelectedPeak().isFitted()) {
            workPane.showFittedProfile();
            Main.mainfrm.getDatabase().updatePeakFrequency(workPane.getSpectrum().getSelectedPeak().getAbsoluteDbIndex(),fr);
        }

    }

    public void assignPeak(ActionEvent e){
        spectrumPane.assignPeak((Double) accSpinner.getValue());
        spectrumPane.updatePeaks();
        spectrumPane.showPeaks();
        spectrumPane.peakDlgStage.hide();
    }

    public void deletePeak(ActionEvent e){
        spectrumPane.deletePeak();
        spectrumPane.updatePeaks();
        spectrumPane.showSpectrum();
        spectrumPane.peakDlgStage.hide();
    }

    public void setSpectrumPane(SpectrumPane pane){
        spectrumPane = pane;
    }

}
