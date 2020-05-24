package fr.ulille.spexp.fx;

import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class LoomisWoodPlot implements Initializable {

    private static final int MAXPLOTS = 50;

    @FXML VBox lwVBox;
    @FXML Label flabel;
    @FXML Spinner spanSpinner;
    @FXML Spinner zoomSpinner;
    @FXML Button plotButton;
    @FXML Button saveButton;
    private int nplots;
    private double width;
    private double height;
    private int index;
    private List<LoomisWoodSpectrumPane> lwPanesList;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        nplots = 0;
        SpinnerValueFactory.DoubleSpinnerValueFactory spinnerValueFactory =
                new SpinnerValueFactory.DoubleSpinnerValueFactory(10,1000,1);
        spanSpinner.setValueFactory(spinnerValueFactory);
        spanSpinner.getValueFactory().setValue(50.);

        SpinnerValueFactory.DoubleSpinnerValueFactory spinnerValueFactory1 =
                new SpinnerValueFactory.DoubleSpinnerValueFactory(1,100,0.1);
        zoomSpinner.setValueFactory(spinnerValueFactory1);
        zoomSpinner.getValueFactory().setValue(1.);
        plotButton.setOnAction(event -> {
            Task task = new Task() {
                @Override protected Void call() throws Exception {
                    for (LoomisWoodSpectrumPane pane : lwPanesList) {
                        pane.verticalZoom((Double) zoomSpinner.getValue());
                        pane.setFrequencyBounds(pane.getFrequency(),(Double) spanSpinner.getValue());
                        pane.readFileList();
                    }
                    return null;
                }
            };

            Thread th = new Thread(task);
            th.setDaemon(true);
            th.start();
        });

        saveButton.setOnAction(event -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            File selectedDirectory = directoryChooser.showDialog(Main.mainfrm.lwplotStage);

            for (LoomisWoodSpectrumPane pane : lwPanesList) {
                try {
                    pane.saveFile(selectedDirectory.getPath(),String.valueOf(pane.getIndex())+".dat");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        spanSpinner.setOnScroll(scrollEvent -> {
            if (scrollEvent.getDeltaY()<0){
                spanSpinner.decrement();
            } else {
                spanSpinner.increment();
            }
        });

        zoomSpinner.setOnScroll(scrollEvent -> {
            if (scrollEvent.getDeltaY()<0){
                double v = (Double) zoomSpinner.getValue();
                zoomSpinner.getValueFactory().setValue(v-0.1);
                for (LoomisWoodSpectrumPane pane : lwPanesList) {
                    pane.verticalZoom((Double) zoomSpinner.getValue());
                    pane.showSpectrum();
                }
            } else {
                double v = (Double) zoomSpinner.getValue();
                zoomSpinner.getValueFactory().setValue(v+0.1);
                for (LoomisWoodSpectrumPane pane : lwPanesList) {
                    pane.verticalZoom((Double) zoomSpinner.getValue());
                    pane.showSpectrum();
                }
            }
        });
    }

    public void plotAll(){
        for (LoomisWoodSpectrumPane pane : lwPanesList) {
            Platform.runLater(()->pane.showSpectrum());
        }
    }

    public void setPlots(List<Double> f){
        nplots = f.size();

        if (nplots>MAXPLOTS){
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Loomis-Wood Plot");
            alert.setHeaderText("Too many plots!");
            alert.setContentText(null);
            alert.initOwner(Main.mainfrm.lwplotStage);
            alert.showAndWait();
            Main.mainfrm.lwplotStage.close();
            return;
        }

        height = Main.mainfrm.lwplotStage.getHeight() / nplots;
        width = Main.mainfrm.lwplotStage.getWidth();
        lwVBox.getChildren().clear();

        lwPanesList = new ArrayList<>();
        for (int i=0;i<nplots;i++){
            LoomisWoodSpectrumPane lwpane = new LoomisWoodSpectrumPane(width,height,flabel, i+1);
            List<String> fileList = new ArrayList<>();
            lwpane.setPrefWidth(width);
            lwpane.setPrefHeight(height);
            lwpane.setFrequencyBounds(f.get(i),50.);
            lwpane.showMessage("LW plot n°"+i);
            lwPanesList.add(lwpane);
        }
        lwVBox.getChildren().addAll(lwPanesList);

        final Service<Void> calculateService = new Service<Void>() {
            @Override
            protected Task<Void> createTask() {
                return new Task<Void>() {

                    @Override
                    protected Void call() throws Exception {
                        for (LoomisWoodSpectrumPane pane : lwPanesList) {
                            pane.readFileList();
                        }
                        return null;
                    }
                };
            }
        };
        calculateService.stateProperty().addListener((ObservableValue<? extends Worker.State> observableValue, Worker.State oldValue, Worker.State newValue) -> {
            switch (newValue) {
                case FAILED:
                case CANCELLED:
                case SUCCEEDED: plotAll();
            }
        });
        calculateService.start();
    }

}
