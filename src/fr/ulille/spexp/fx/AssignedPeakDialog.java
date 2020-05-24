package fr.ulille.spexp.fx;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.MenuButton;
import javafx.scene.layout.Pane;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;

import java.net.URL;
import java.util.ResourceBundle;

public class AssignedPeakDialog implements Initializable {

    @FXML Pane appane;
    @FXML MenuButton menuButton;
    private TextFlow textFlow;
    private SpectrumPane spectrumPane;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        textFlow = new TextFlow();
        textFlow.setLineSpacing(2);
        textFlow.setPadding(new Insets(2,0,2,0));
        textFlow.setTextAlignment(TextAlignment.LEFT);
        textFlow.setId("textf");
        appane.getChildren().add(textFlow);
    }

    public void addAssignment(ActionEvent e){
        spectrumPane.addAssignment();
        spectrumPane.updatePeaks();
    }

    public void deleteAssignment(ActionEvent e){

        spectrumPane.deleteAssignment();
        spectrumPane.updatePeaks();
        spectrumPane.showPeaks();
        spectrumPane.assignedPeakDlgStage.hide();
    }

    public void setSpectrumPane(SpectrumPane pane){
        spectrumPane = pane;
    }
}
