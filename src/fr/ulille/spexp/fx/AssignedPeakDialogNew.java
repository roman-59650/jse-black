package fr.ulille.spexp.fx;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.function.Predicate;

public class AssignedPeakDialogNew implements Initializable {

    @FXML ListView<Text> listView;
    @FXML VBox vBox;
    @FXML ToolBar tool;
    private SpectrumPane spectrumPane;
    private double cellHeight;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        /*Text text = new Text();
        text.setFont(Font.font("monospaced",12));
        text.setText("A");
        listView.getItems().add(text);
        double height = text.getBoundsInLocal().getHeight();
        cellHeight = height;
        listView.getItems().clear();*/

        //listView.prefHeightProperty().bind(Bindings.size(listView.getItems()).multiply(height).add(height));
        //vBox.prefHeightProperty().bind(listView.prefHeightProperty().add(tool.prefHeightProperty()));
    }

    public double getCellHeight() {
        return cellHeight;
    }

    public void fillList(ArrayList<Text> list){
        listView.getItems().clear();
        for (Text t:list){
            listView.getItems().add(t);
        }
        //listView.getSelectionModel().selectFirst();
        listView.getSelectionModel().selectAll();
    }

    public void addAssignment(ActionEvent e){
        spectrumPane.addAssignment();
        spectrumPane.updatePeaks();
        spectrumPane.assignedPeakDlgStage.hide();
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
