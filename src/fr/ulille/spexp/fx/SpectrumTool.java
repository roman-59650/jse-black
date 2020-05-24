package fr.ulille.spexp.fx;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;

import java.net.URL;
import java.sql.SQLException;
import java.util.Optional;
import java.util.ResourceBundle;

public class SpectrumTool implements Initializable {
    @FXML TextField smpField;
    @FXML TextField zlevField;
    @FXML TextField ylimField;
    @FXML TextField blptField;
    @FXML Spinner sigmaSpinner;
    @FXML ToggleButton insertPeakButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        SpinnerValueFactory.IntegerSpinnerValueFactory spf = new SpinnerValueFactory.IntegerSpinnerValueFactory(1,1000,1);
        spf.setValue(8);
        sigmaSpinner.setValueFactory(spf);
        sigmaSpinner.setOnScroll(event -> {
            if (event.getDeltaY()<0) sigmaSpinner.decrement();
            else sigmaSpinner.increment();
        });
    }

    public void onInvertClick(ActionEvent e){
        Tab tab = Main.mainfrm.tabpane.getSelectionModel().getSelectedItem();
        WorkPane workPane = (WorkPane) tab.getContent().lookup("#workpane");
        workPane.getSpectrum().invertData();
        workPane.plotAll();
    }

    public void onSmoothClick(ActionEvent e){
        Tab tab = Main.mainfrm.tabpane.getSelectionModel().getSelectedItem();
        WorkPane workPane = (WorkPane) tab.getContent().lookup("#workpane");
        workPane.getSpectrum().smoothData(0, Integer.parseInt(smpField.getText()));
        workPane.plotAll();
    }

    public void onDiffSmoothClick(ActionEvent e){
        Tab tab = Main.mainfrm.tabpane.getSelectionModel().getSelectedItem();
        WorkPane workPane = (WorkPane) tab.getContent().lookup("#workpane");
        workPane.getSpectrum().smoothData(1, Integer.parseInt(smpField.getText()));
        workPane.plotAll();
    }

    public void onCreateBaselineClick(ActionEvent e){
        int npt = Integer.parseInt(blptField.getText());
        double nyl = Double.parseDouble(ylimField.getText());
        double zl = Double.parseDouble(zlevField.getText());
        Tab tab = Main.mainfrm.tabpane.getSelectionModel().getSelectedItem();
        WorkPane workPane = (WorkPane) tab.getContent().lookup("#workpane");
        workPane.getSpectrum().setBaseline(npt,nyl,zl,true);
        workPane.plotAll();

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(Main.getPrimaryStage());
        alert.setTitle("Baseline");
        alert.setHeaderText(null);
        alert.setContentText("Remove baseline?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == ButtonType.OK){
            workPane.getSpectrum().suppBaseline();
            workPane.plotAll();
        }
    }

    public void onSuppressBaselineClick(ActionEvent e){
        Tab tab = Main.mainfrm.tabpane.getSelectionModel().getSelectedItem();
        WorkPane workPane = (WorkPane) tab.getContent().lookup("#workpane");
        if (workPane.getSpectrum().isBaselined()) {
            workPane.getSpectrum().suppBaseline();
            workPane.plotAll();
        }
    }

    public void onFindPeaksClick(ActionEvent e) throws SQLException {
        if (Main.mainfrm.tabpane.getTabs().size()==0) return;
        Tab tab = Main.mainfrm.tabpane.getSelectionModel().getSelectedItem();
        WorkPane workPane = (WorkPane) tab.getContent().lookup("#workpane");
        workPane.getSpectrum().clearPeaks();
        int npeaks = workPane.getSpectrum().findPeaks((Integer) sigmaSpinner.getValue(), Main.mainfrm.getDatabase());
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Peak Finder");
        alert.setHeaderText(String.format("Number of peaks found: %d",npeaks));
        alert.setContentText(null);
        alert.initOwner(Main.getPrimaryStage());
        alert.showAndWait();
        workPane.plotAll();
    }

    public void onInsertPeaksClick(ActionEvent e) throws SQLException {
        if (Main.mainfrm.tabpane.getTabs().size()==0) return;
        Tab tab = Main.mainfrm.tabpane.getSelectionModel().getSelectedItem();
        WorkPane workPane = (WorkPane) tab.getContent().lookup("#workpane");
        int n = workPane.getSpectrum().addPeaksDb(Main.mainfrm.getDatabase());
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Peak Finder");
        alert.setHeaderText(String.format("%d peaks inserted into DB",n));
        alert.setContentText(null);
        alert.initOwner(Main.getPrimaryStage());
        alert.showAndWait();
        workPane.plotAll();
    }

    public void onFitPeaksClick(ActionEvent e) throws SQLException {
        if (Main.mainfrm.tabpane.getTabs().size()==0) return;
        Tab tab = Main.mainfrm.tabpane.getSelectionModel().getSelectedItem();
        WorkPane workPane = (WorkPane) tab.getContent().lookup("#workpane");
        workPane.getSpectrum().fitPeaks(Main.mainfrm.getDatabase());
    }

    public void onFFTClick(ActionEvent e){
        if (Main.mainfrm.tabpane.getTabs().size()==0) return;
        Tab tab = Main.mainfrm.tabpane.getSelectionModel().getSelectedItem();
        WorkPane workPane = (WorkPane) tab.getContent().lookup("#workpane");
        workPane.getSpectrum().doFFT();

        Main.mainfrm.fftView.setHPFcutoff(Double.parseDouble(Main.getProperties().getProperty("hpf cutoff")));
        Canvas canvas = (Canvas) Main.mainfrm.fftviewStage.getScene().lookup("#fftcanvas");
        workPane.getSpectrum().getFFTransformer().plot(canvas);
        Main.mainfrm.fftviewStage.show();
    }

    public void onInsertSinglePeakClick(ActionEvent e){
        if (Main.mainfrm.tabpane.getTabs().size()==0) return;
        Tab tab = Main.mainfrm.tabpane.getSelectionModel().getSelectedItem();
        WorkPane workPane = (WorkPane) tab.getContent().lookup("#workpane");
        workPane.setPeakInsertEnabled(insertPeakButton.isSelected());
        if (insertPeakButton.isSelected()) Main.getPrimaryStage().requestFocus();
        else Main.mainfrm.updateWindow();
    }

    public void onClearPeaksClick(){
        if (Main.mainfrm.tabpane.getTabs().size()==0) return;
        Tab tab = Main.mainfrm.tabpane.getSelectionModel().getSelectedItem();
        WorkPane workPane = (WorkPane) tab.getContent().lookup("#workpane");
        workPane.getSpectrum().clearPeaks();
        workPane.plotAll();
    }

}
