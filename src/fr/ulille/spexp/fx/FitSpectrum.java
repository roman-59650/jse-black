package fr.ulille.spexp.fx;

import fr.ulille.spexp.fftprofile.*;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import org.controlsfx.control.StatusBar;

import java.net.URL;
import java.util.Locale;
import java.util.ResourceBundle;

public class FitSpectrum implements Initializable {

    FitSpectrumPane fitSpectrumPane;

    @FXML VBox box1;
    @FXML VBox box2;
    @FXML ToggleButton selectBaselineButton;
    @FXML ToggleButton insertLineButton;
    @FXML Button fitButton;
    @FXML Button deleteLineButton;
    @FXML Button insertFromDbButton;
    @FXML TableColumn<LineParameters, Integer> indexColumn;
    @FXML TableColumn<LineParameters, Double> frequencyColumn;
    @FXML TableColumn<LineParameters, Double> amplitudeColumn;
    @FXML TableView<LineParameters> paramsTable;
    @FXML TableView<CommonParameters> commonsTable;
    @FXML TableColumn<CommonParameters, String> nameColumn;
    @FXML TableColumn<CommonParameters, Double> valueColumn;
    @FXML ComboBox<Function> functionCombo;
    @FXML ComboBox<Derivative> derivativeCombo;
    @FXML StatusBar statusBar;

    private ObservableList<LineParameters> list = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        list.clear();

        //pane.widthProperty().addListener((obs, old, newValue)->fitSpectrumPane.showSpectrum());

        //VBox.setVgrow(box1, Priority.ALWAYS);

        frequencyColumn.setCellValueFactory(cell->cell.getValue().frequencyProperty());
        frequencyColumn.setCellFactory(FrequencySpinnerCell.forTableColumn(0.01));

        amplitudeColumn.setCellValueFactory(cell->cell.getValue().amplitudeProperty());
        amplitudeColumn.setCellFactory(AmplitudeSpinnerCell.forTableColumn());

        indexColumn.setCellValueFactory(cd->new SimpleIntegerProperty(cd.getValue().getIndex()).asObject());

        nameColumn.setCellValueFactory(cd->new SimpleStringProperty(cd.getValue().getName()));

        valueColumn.setCellValueFactory(cell->cell.getValue().valueProperty());
        valueColumn.setCellFactory(FrequencySpinnerCell.forTableColumn(0.01));

        for (Function func : Function.values()){
            functionCombo.getItems().add(func);
        }
        functionCombo.getSelectionModel().selectFirst();
        functionCombo.valueProperty().addListener((observable, oldValue, newValue)->{
            System.out.println("function changed to "+newValue);
            fitSpectrumPane.setModel(newValue,derivativeCombo.getSelectionModel().getSelectedItem());
        });

        for (Derivative drv : Derivative.values()){
            derivativeCombo.getItems().add(drv);
        }
        derivativeCombo.getSelectionModel().selectLast();
        derivativeCombo.valueProperty().addListener((observable, oldValue, newValue)->{
            fitSpectrumPane.setModel(functionCombo.getSelectionModel().getSelectedItem(), newValue);
        });
    }

    public void onInsertBoundsClick(ActionEvent e){
        fitSpectrumPane.setBaselineBoundsInsert(selectBaselineButton.isSelected());
    }

    public void onInsertLineClick(ActionEvent e){
        selectBaselineButton.setSelected(false);
        fitSpectrumPane.setLineInsert(insertLineButton.isSelected());
    }

    public void onFitClick(ActionEvent e){
        selectBaselineButton.setSelected(false);
        insertLineButton.setSelected(false);
        fitSpectrumPane.fitProfile();
        if (fitSpectrumPane.getProfileFitter().isConverged()){
            statusBar.setGraphic(new Circle(6, Color.LIME));
            statusBar.setText(String.format(Locale.US,"Converged in %d iterations | Cost: %.3E | RMS: %.3E | Noise RMS: %.3E",
                    fitSpectrumPane.getProfileFitter().getOptimum().getIterations(),
                    fitSpectrumPane.getProfileFitter().getOptimum().getCost(),
                    fitSpectrumPane.getProfileFitter().getOptimum().getRMS(),
                    fitSpectrumPane.getBaseline().getRms()));
        } else {
            statusBar.setGraphic(new Rectangle(10,10, Color.RED));
            statusBar.setText("Fit diverging :(");
        }

    }

    public void onResetClick(ActionEvent e){
        selectBaselineButton.setSelected(false);
        insertLineButton.setSelected(false);
        fitSpectrumPane.resetProfile();
        statusBar.setGraphic(null);
        statusBar.setText("");
    }

    public void initModel(){
        fitSpectrumPane.setModel(functionCombo.getSelectionModel().getSelectedItem(),
                derivativeCombo.getSelectionModel().getSelectedItem());
    }

    public void onDeleteLineClick(ActionEvent e){
        if (paramsTable.getSelectionModel().getSelectedIndex()>-1)
            fitSpectrumPane.removeLine(paramsTable.getSelectionModel().getSelectedIndex());
    }

    public void onInsertFromDBClick(ActionEvent e){
        fitSpectrumPane.insertFromDatabase();
    }

    public void onUpdateDatabaseClick(ActionEvent e){
        fitSpectrumPane.updatePeaks();
    }
}
