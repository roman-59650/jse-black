package fr.ulille.spexp.fx;

import fr.ulille.spexp.compiler.MiscData;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import fr.ulille.spexp.data.Database;
import javafx.scene.paint.Color;

import javax.sql.rowset.CachedRowSet;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class LoomisWoodSpectrumPane extends SpectrumPane {

    private Label flabel;
    private int index;
    private double fmin;
    private double fmax;
    private double freq;
    private double span;

    public LoomisWoodSpectrumPane(double width, double height, Label label, int index) {
        super(width, height);
        flabel = label;
        this.index = index;
        cv2.addEventHandler(MouseEvent.MOUSE_MOVED, this::onMouseMove);
        cv2.addEventHandler(MouseEvent.MOUSE_CLICKED, this::onMouseClick);

        cv2.setOnScroll(scrollEvent -> {
            onMouseScroll(scrollEvent);
        });
        fmin = 0;
        fmax = 0;
    }

    public void setFrequencyBounds(double freq, double span){
        this.freq = freq;
        this.span = span;
        this.fmin = freq-span;
        this.fmax = freq+span;
    }

    public void plotFrequency(){
        GraphicsContext gc = cv1.getGraphicsContext2D();
        double frmin = spectrum.getScreenFreqMin();
        double frmax = spectrum.getScreenFreqMax();
        double nix = (freq-frmin)*cv1.getWidth()/(frmax-frmin);
        gc.setStroke(Color.valueOf(Main.getProperties().getProperty("background color")).brighter());
        gc.setLineWidth(0.5);
        gc.setLineDashes(2);
        gc.strokeLine(nix, 2, nix, this.getHeight()-2);

    }

    public double getFrequency(){
        return freq;
    }

    @Override
    public void showSpectrum(){
        if (!spectrum.isFileRead()) return;
        super.showSpectrum();
        plotFrequency();
        showPeaks();
    }

    private void onMouseScroll(ScrollEvent e){
        if (!e.isControlDown()) return;
        if (e.getDeltaY()<0)
            verticalZoomIn(1.1);
        else
            verticalZoomOut(1.1);
    }

    private void onMouseMove(MouseEvent me){
        double xvalue;
        if (spectrum.isFileRead()){
            xvalue = spectrum.getScreenFreqMin()+me.getX()*spectrum.getScreenSpan()/cv2.getWidth();
            super.checkPeakAtPosition(me,false);
            if (isPeakSelectionCancelled) {
                isPeakSelectionCancelled = false;
            }
        }
        else {
            xvalue = 0.0;
        }
        flabel.setText(String.format(Locale.US, "x = %10.3f | ∆x = %7.3f", xvalue, xvalue - freq));
    }

    private void onMouseClick(MouseEvent me){
        if (!spectrum.isFileRead()) return;
        Database db = Main.mainfrm.getDatabase();
        if (me.isShiftDown()&&me.getButton()== MouseButton.PRIMARY){
            double freq = spectrum.getScreenFreqMin()+me.getX()*spectrum.getScreenSpan()/cv2.getWidth();
            MiscData misc = db.getMiscData();
            double lwmult = Double.parseDouble(Main.getProperties().getProperty("lw multiplier"));
            double doppler = lwmult*3.58e-7*freq*Math.sqrt(misc.getTemp()/misc.getMass());
            if (db.getPeakInterval(freq,doppler)) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "", ButtonType.YES, ButtonType.NO);
                alert.setTitle("Peak Finder");
                alert.setHeaderText(String.format("Peak too close to another one. Abort insertion?"));
                alert.setContentText(null);
                alert.initOwner(Main.getPrimaryStage());
                Optional<ButtonType> result = alert.showAndWait();
                if (result.get() == ButtonType.YES){
                    return;
                }
            }
            spectrum.insertPeak(freq, Main.mainfrm.lwplotStage);
            double fmin = spectrum.getFreqMin();
            double fmax = spectrum.getFreqMax();
            db.getPeakData(fmin, fmax); // peak
            setPeakResultSet(db.getPeaksCache());
            showPeaks();
        } else {
            setSelectedPredictedTransition(index);
            // updating to a correct peak result set before showing
            double fmin = spectrum.getFreqMin();
            double fmax = spectrum.getFreqMax();
            db.getPeakData(fmin, fmax); // peaks
            CachedRowSet cachers = db.getPeaksCache();
            setPeakResultSet(cachers);
            // trying to select a peak
            checkPeakAtPosition(me, true);
        }
    }

    public int getIndex() {
        return index;
    }

    public void readFileList(){
        List<String> fileList = new ArrayList<>();
        String fileName1 = Main.mainfrm.getDatabase().getFileAtFrequency(fmin);
        if (!fileName1.isEmpty()) fileList.add(fileName1);
        String fileName2 = Main.mainfrm.getDatabase().getFileAtFrequency(fmax);
        if (!fileName2.isEmpty()&&!fileName2.equalsIgnoreCase(fileName1)) fileList.add(fileName2);
        if (fileList.size()>0){
            readFiles(fileList);
        }
        else
            showMessage("NO DATA");
    }

    public void readFiles(List<String> files) {
        showMessage("Reading ...");

        final Service<Void> calculateService = new Service<Void>() {
            @Override
            protected Task<Void> createTask() {
                return new Task<Void>() {

                    @Override
                    protected Void call() throws Exception {
                        spectrum.readFileList(files,fmin, fmax);
                        return null;
                    }
                };
            }
        };
        calculateService.stateProperty().addListener((ObservableValue<? extends Worker.State> observableValue, Worker.State oldValue, Worker.State newValue) -> {
            switch (newValue) {
                case FAILED:
                case CANCELLED:
                case SUCCEEDED: Main.mainfrm.getDatabase().getPeakData(fmin, fmax); // peaks
                                CachedRowSet cachers = Main.mainfrm.getDatabase().getPeaksCache();
                                setPeakResultSet(cachers);
                                Platform.runLater(()->showSpectrum());
                                break;
            }
        });
        calculateService.start();
    }

    public void saveFile(String dir, String fileName) throws IOException {
        double[] x = spectrum.getScreenFrequenciesArray();
        double[] y = spectrum.getScreenYArray(cv1.getGraphicsContext2D());
        for (int i=0;i<x.length;i++){
            x[i]-=freq;
        }
        FileWriter fileWriter = new FileWriter(new File(dir, fileName));
        for (int i=0;i<x.length;i++){
            String str = String.format(Locale.US,"%12.3f %12.3f",x[i],-y[i]);
            fileWriter.write(str+"\r\n");
        }
        fileWriter.close();
    }


}
