package fr.ulille.spexp.fx;

import fr.ulille.spexp.compiler.MiscData;
import fr.ulille.spexp.data.*;
import fr.ulille.spexp.math.VoigtFunctionInt;
import fr.ulille.spexp.spectrum.Spectrum;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import org.controlsfx.control.StatusBar;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class WorkPane extends SplitPane {

    private static final int LINEWIDTHFACTOR = 50;

    private WorkingSpectrumPane spectrumPane;
    private PredsPane predsPane;
    private Database db;
    private List<PredictRowData> predictedList;
    private List<AssignRowData> assignedList;
    private List<Integer> arefs;
    private boolean isPeakInsertEnabled;
    private List<Double> predictedFrequencies;
    private List<Double> predictedIntesities;
    private double[] xpr;
    private double[] ypr;
    private double yprmin;
    private double yprmax;
    private boolean isProfileReady;
    private StatusBar statusBar;

    public WorkPane(double width, double height, Label lab, StatusBar statusBar){
        this.setOrientation(Orientation.VERTICAL);
        this.setId("workpane");
        spectrumPane = new WorkingSpectrumPane(width,height,lab);
        predsPane = new PredsPane(width,height);
        this.getItems().addAll(predsPane,spectrumPane);
        predictedList = new ArrayList<>();
        assignedList = new ArrayList<>();
        arefs = new ArrayList<>();
        isPeakInsertEnabled = false;
        predictedFrequencies = new ArrayList<>();
        predictedIntesities = new ArrayList<>();
        isProfileReady = false;
        this.statusBar = statusBar;
    }

    public void setPeakInsertEnabled(boolean peakInsertEnabled) {
        isPeakInsertEnabled = peakInsertEnabled;
        if (isPeakInsertEnabled) spectrumPane.setCanvasCursor(Cursor.CROSSHAIR);
        else spectrumPane.setCanvasCursor(Cursor.DEFAULT);
    }

    public void highlightFrequency(double frequency){
        spectrumPane.highlightFrequency(frequency);
        double xc = spectrumPane.getSpectrum().getScreenXCoord(frequency, spectrumPane.cv1.getGraphicsContext2D());
        predsPane.getPredAtCursor(xc);
    }

    public Spectrum getSpectrum(){
        return spectrumPane.getSpectrum();
    }

    public void setDatabase(Database database){
        this.db = database;
        if (!db.isConnected()) return;
        if (!spectrumPane.getSpectrum().isFileRead()) return;
        // database tuning for a given spectrum
        double fmin = spectrumPane.getSpectrum().getFreqMin();
        double fmax = spectrumPane.getSpectrum().getFreqMax();

        db.getPeakData(fmin, fmax); // peaks
        spectrumPane.setPeakResultSet(db.getPeaksCache());
        db.getTransList("");  // transitions
        db.getPredList(fmin, fmax); // compiled transitions
        db.getAsgnList();           // assigned transitions
    }

    public void plotAll(){
        if (spectrumPane.getSpectrum().isFileRead()) {
            spectrumPane.showSpectrum();
            predsPane.showPredictions();
            if (Boolean.parseBoolean(Main.getProperties().getProperty("plot profile"))) generateProfile();
        }
    }

    private void yprScale(){
        int xaLimit = spectrumPane.getSpectrum().getXA();
        int xbLimit = spectrumPane.getSpectrum().getXB();
        double dmin = ypr[xaLimit];
        double dmax = ypr[xaLimit];
        for (int i=xaLimit;i<xbLimit;i++){
            if (ypr[i]>dmax) dmax = ypr[i];
            if (ypr[i]<dmin) dmin = ypr[i];
        }
        yprmin = dmin-0.05*(dmax-dmin);
        yprmax = dmax+0.05*(dmax-dmin);
    }

    private void generateProfile(){
        double fmin = spectrumPane.getSpectrum().getFreqMin();
        double fmax = spectrumPane.getSpectrum().getFreqMax();

        final Service<Void> generateProfileService = new Service<Void>() {
            @Override
            protected Task<Void> createTask() {
                statusBar.progressProperty().bind(this.progressProperty());
                statusBar.getRightItems().clear();
                return new Task<Void>() {
                    @Override
                    protected Void call() throws Exception {
                        // get all transitions in the file
                        predictedFrequencies.clear();
                        predictedIntesities.clear();
                        try {
                            String extfilter = "WHERE FREQ>"+fmin+" AND FREQ<"+fmax;
                            db.getTransList(extfilter);
                            db.tranrs.first();
                            predictedFrequencies.add(db.tranrs.getDouble("FREQ"));
                            predictedIntesities.add(db.tranrs.getDouble("ALPHA"));
                            while (db.tranrs.next()) {
                                predictedFrequencies.add(db.tranrs.getDouble("FREQ"));
                                predictedIntesities.add(db.tranrs.getDouble("ALPHA"));
                            }
                            System.out.println(predictedFrequencies.size());
                        } catch (SQLException throwables) {
                            throwables.printStackTrace();
                        }
                        db.getTransList("");
                        double[] x0 = predictedFrequencies.stream().mapToDouble(d->d.doubleValue()).toArray();
                        double[] a0 = predictedIntesities.stream().mapToDouble(Double::doubleValue).toArray();
                        xpr = spectrumPane.getSpectrum().getXData();
                        ypr = new double[xpr.length];
                        double linewidthMultiplier = Double.parseDouble(Main.getProperties().getProperty("lw multiplier"));
                        double molMass = Main.mainfrm.getDatabase().getMiscData().getMass();
                        double Temp = Main.mainfrm.getDatabase().getMiscData().getTemp();
                        int drv = Integer.parseInt(Main.getProperties().getProperty("profile derivative"));
                        for (int i=0;i<xpr.length;i++){
                            double s = 0.;
                            for (int k=0;k<x0.length;k++){
                                double dw = 3.58e-7*x0[k]*Math.sqrt(Temp/molMass); // Doppler HWHM
                                if (Math.abs(xpr[i]-x0[k])<LINEWIDTHFACTOR*dw){
                                    double lw = dw*(linewidthMultiplier-1);
                                    if (lw<0) lw = 0;
                                    VoigtFunctionInt vfi = new VoigtFunctionInt(drv,xpr[i],x0[k],dw,lw,a0[k]);
                                    s+=vfi.value(xpr[i]);
                                }
                            }
                            ypr[i] = s;
                            updateProgress(i,ypr.length);
                        }
                        return null;
                    }
                };
            }
        };
        generateProfileService.stateProperty().addListener((ObservableValue<? extends Worker.State> observableValue, Worker.State oldValue, Worker.State newValue) -> {
            switch (newValue) {
                case FAILED:
                case CANCELLED:
                case SUCCEEDED:
                    yprScale();
                    predsPane.showPredictionsProfile();
                    isProfileReady = true;
                    statusBar.progressProperty().unbind();
                    statusBar.progressProperty().setValue(0);
                    statusBar.getRightItems().add(new Text("Profile: OK"));
                    break;
            }
        });
        generateProfileService.start();
    }

    public void readFile(File file) {
        spectrumPane.showMessage("Opening "+file.getName()+" ...");
        spectrumPane.resetCounters();
        isProfileReady = false;
        final Service<Void> calculateService = new Service<Void>() {
            @Override
            protected Task<Void> createTask() {
                return new Task<Void>() {
                    @Override
                    protected Void call() throws Exception {
                        if (getFileExtension(file).contains(".dat"))
                            spectrumPane.getSpectrum().readData(file.getPath());
                        if (getFileExtension(file).contains(".000"))
                            spectrumPane.getSpectrum().readKharkivData(file.getPath());
                        return null;
                    }
                };
            }
        };
        calculateService.stateProperty().addListener((ObservableValue<? extends Worker.State> observableValue, Worker.State oldValue, Worker.State newValue) -> {
            switch (newValue) {
                case FAILED:
                case CANCELLED:
                case SUCCEEDED: setDatabase(Main.mainfrm.getDatabase());
                                spectrumPane.getSpectrum().scaleData();
                                plotAll();
                                break;
            }
        });
        calculateService.start();
    }

    public void showFittedProfile(){
        spectrumPane.showFittedProfile();
    }

    public void selectNext(){
        predsPane.selectNext();
    }

    public void selectPrev(){
        predsPane.selectPrev();
    }

    private String getFileExtension(File file) {
        String name = file.getName();
        int lastIndexOf = name.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return ""; // empty extension
        }
        System.out.println(name.substring(lastIndexOf));
        return name.substring(lastIndexOf);
    }

    private class WorkingSpectrumPane extends SpectrumPane {
        private double mouseXstart;
        private double mouseXstop;
        private boolean zoomMode;
        private static final int maxZoom = 10;
        private int zoomCount;
        private int[] zoomXA = new int[maxZoom];
        private int[] zoomXB = new int[maxZoom];
        private Label labelXY;
        private final Set<KeyCode> pressedKeys;

        public WorkingSpectrumPane(double width, double height, Label lab){

            super(width, height);

            mouseXstart = 0;
            mouseXstop = 0;
            zoomMode = false;
            zoomCount = -1;
            labelXY = lab;
            pressedKeys = new HashSet<>();

            cv2.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::onMouseDragged);
            cv2.addEventHandler(MouseEvent.MOUSE_PRESSED, this::onMouseDown);
            cv2.addEventHandler(MouseEvent.MOUSE_RELEASED, this::onMouseUp);
            cv2.addEventHandler(MouseEvent.MOUSE_MOVED, this::onMouseMove);
            cv2.addEventHandler(MouseEvent.MOUSE_CLICKED, this::onMouseClick);
            cv2.addEventHandler(KeyEvent.KEY_PRESSED,this::onKeyPressed);
            cv2.addEventHandler(KeyEvent.KEY_RELEASED,this::onKeyReleased);
            cv2.setOnScroll(scrollEvent -> {
                onMouseScroll(scrollEvent);
            });

            cv2.setFocusTraversable(true);
        }

        @Override
        public void showSpectrum() {
            if (!spectrum.isFileRead()) return;
            super.showSpectrum();
            showPeaks();
            if ((spectrum.getSelectedPeak().getIndex()>0)&&(spectrum.getSelectedPeak().isFitted())) showFittedProfile();
            showPredictionAt(predsPane.getSelectedPredFrequency());
            if (Boolean.valueOf(Main.getProperties().getProperty("show x-axis"))) showScale();
        }

        public void showScale(){
            GraphicsContext gc = cv1.getGraphicsContext2D();
            Color lineColor = Color.valueOf(Main.getProperties().getProperty("background color")).invert().deriveColor(0,0.5,1,1);
            gc.setStroke(lineColor);
            gc.setLineWidth(gc.getLineWidth()/2.);
            gc.setFont(Font.font("Sans serif",10));
            String fmt = "";
            if (spectrum.getScreenSpan()>100.) fmt = "%.0f";
            else if (spectrum.getScreenSpan()<=100.&&spectrum.getScreenSpan()>10.) fmt="%.1f";
            else if (spectrum.getScreenSpan()<=10.) fmt = "%.2f";
            gc.setTextAlign(TextAlignment.CENTER);
            int ticks = Integer.parseInt(Main.getProperties().getProperty("x-axis ticks"));
            double step = spectrum.getScreenSpan()/ticks;
            for (int i=1;i<ticks;i++){
                gc.setLineDashes(2.,8.);
                gc.strokeLine(i* cv1.getWidth()/ticks,0,i*cv1.getWidth()/ticks,cv1.getHeight()-20);
                gc.setLineDashes(null);
                gc.setFill(lineColor);
                gc.fillText(String.format(Locale.US,fmt, spectrum.getScreenFreqMin()+i*step),
                        i* cv1.getWidth()/ticks, cv1.getHeight()-10);
            }
        }

        public void resetCounters(){
            mouseXstart = 0;
            mouseXstop = 0;
            zoomMode = false;
            zoomCount = -1;
        }

        public void highlightFrequency(double frequency){
            GraphicsContext gc = cv1.getGraphicsContext2D();
            double x = spectrum.getScreenXCoord(frequency, gc);
            gc.setStroke(Color.RED);
            gc.setLineWidth(2.5);
            gc.strokeLine(x,0,x,cv1.getHeight());
        }

        public void showPeaks(){
            super.showPeaks();

            GraphicsContext gc = cv1.getGraphicsContext2D();
            gc.setTextAlign(TextAlignment.LEFT);
            gc.setFont(new Font(gc.getFont().getName(),10));
            double width = getWidth();
            int npeaks = spectrum.getPeakData().getLength();
            if (npeaks>0){
                double ixData[] = new double[npeaks];
                double iyData[] = new double[npeaks];
                for (int j=0;j<npeaks;j++){
                    double pfr = spectrum.getXData(spectrum.getPeakData().getFrequency(j));
                    ixData[j] = spectrum.getScreenXCoord(pfr,gc);
                    iyData[j] = spectrum.getScreenYCoord(spectrum.getPeakData().getIntensity(j),gc);
                }
                double ixPrev = -1;
                double iix, iiy, textix;
                for (int i=0;i<npeaks;i++){
                    iix = ixData[i]; iiy = iyData[i];
                    if ((iix>0)&(iix<this.getWidth())){
                        textix = iix;
                        if (Math.abs(textix-ixPrev)<10) textix = ixPrev+10;
                        if (width-textix<10) textix = width-11;
                        if (textix<10) textix = 11;
                        gc.setStroke(Color.BLUE);
                        gc.strokeOval(iix-3, iiy-3, 6, 6);
                        if (spectrum.getPeakData().getViewMode(i)== PeakList.PeakViewMode.SimpleView){
                            int ifr = spectrum.getPeakData().getFrequency(i);
                            // drawing text with 90° turn
                            if (iiy-30<0)
                                drawText(textix+3, iiy, 90, Double.toString(spectrum.getXData(ifr)),gc);
                            else
                                drawText(textix+3, iiy, -90, Double.toString(spectrum.getXData(ifr)),gc);
                        }
                        ixPrev = iix;
                    }
                }
            } // if npeaks
        }

        public void showFittedProfile(){
            GraphicsContext gc = cv1.getGraphicsContext2D();
            double height = gc.getCanvas().getHeight();
            double width = gc.getCanvas().getWidth();
            double xv[] = spectrum.getSelectedPeak().getXtable();
            double yv[] = spectrum.getSelectedPeak().getYtable();
            int np = xv.length;
            double[] pxdata = new double[np];
            double[] pydata = new double[np];
            for (int i=0; i<np; i++){
                pxdata[i] = (xv[i]-spectrum.getXA())*width/(spectrum.getXB()-spectrum.getXA());
                pydata[i] = height -
                        height*(yv[i]-spectrum.getYScreenMin())/(spectrum.getYScreenMax()-spectrum.getYScreenMin());
            }
            plotXY(gc,pxdata,pydata,np,Color.SALMON);
        }

        private void onMouseDown(MouseEvent me){
            if (isPeakInsertEnabled) return;
            if (me.getButton() == MouseButton.PRIMARY&&pressedKeys.contains(KeyCode.COMMAND)){
                System.out.println(me.getX());
                return;
            }

            if (me.getButton() == MouseButton.PRIMARY) {
                if (spectrum.isFileRead()){
                    mouseXstart = me.getX();
                    mouseXstop = mouseXstart;
                    zoomMode = true;
                }
            }
        }

        private void onMouseDragged(MouseEvent me){
            if (isPeakInsertEnabled) return;
            if (zoomMode){
                mouseXstop = me.getX();
                GraphicsContext gcc = cv2.getGraphicsContext2D();
                gcc.setLineWidth(1);
                gcc.clearRect(0, 0, cv2.getWidth(), cv2.getHeight());
                gcc.setStroke(Color.GRAY);
                gcc.setFill(Color.rgb(150, 100, 250, 0.5));
                if (mouseXstop>=mouseXstart)
                    gcc.fillRect(mouseXstart, 0, mouseXstop-mouseXstart, cv2.getHeight());
                else
                    gcc.fillRect(mouseXstop, 0, mouseXstart-mouseXstop, cv2.getHeight());
            }
        }

        private void onMouseUp(MouseEvent me){
            if (isPeakInsertEnabled) return;
            if (zoomMode){
                zoomMode = false;
                if (mouseXstop>cv2.getWidth()) mouseXstop=cv2.getWidth();
                if (me.getButton() == MouseButton.PRIMARY){
                    if (mouseXstop==mouseXstart) return;
                    if (zoomCount<maxZoom){
                        zoomCount++;
                        zoomXA[zoomCount]=spectrum.getXA();
                        zoomXB[zoomCount]=spectrum.getXB();
                        if (mouseXstop<mouseXstart){
                            double tmp = mouseXstart;
                            mouseXstart = mouseXstop;
                            mouseXstop = tmp;
                        }
                        int newxb = (int)(spectrum.getXA()+mouseXstop*(spectrum.getXB()-spectrum.getXA())/cv1.getWidth());
                        int newxa = (int)(spectrum.getXA()+mouseXstart*(spectrum.getXB()-spectrum.getXA())/cv1.getWidth());
                        spectrum.setXScreenLimits(newxa, newxb);
                        db.getPeakData(spectrum.getScreenFreqMin(),spectrum.getScreenFreqMax());
                        setPeakResultSet(db.getPeaksCache());
                        cv2.getGraphicsContext2D().clearRect(0, 0, cv2.getWidth(), cv2.getHeight());
                        showSpectrum();
                        predsPane.showPredictions();
                    }
                } // if me.getButton()
            } // if zoomMode
            if (me.getButton()==MouseButton.SECONDARY){
                if (zoomCount>-1){
                    spectrum.setXScreenLimits(zoomXA[zoomCount], zoomXB[zoomCount]);
                    db.getPeakData(spectrum.getScreenFreqMin(),spectrum.getScreenFreqMax());
                    setPeakResultSet(db.getPeaksCache());
                    zoomCount--;
                    showSpectrum();
                    predsPane.showPredictions();
                }
            }
        }

        private void onMouseMove(MouseEvent me){
            double xvalue;
            double yvalue;
            double dxvalue = 0.;
            if (spectrum.isFileRead()){
                xvalue = spectrum.getScreenFreqMin()+me.getX()*spectrum.getScreenSpan()/cv2.getWidth();
                yvalue = spectrum.getYScreenMax()-me.getY()*(spectrum.getYScreenMax()-spectrum.getYScreenMin())/cv2.getHeight();
                if (predsPane.getSelectedPredFrequency()>0)
                    dxvalue = spectrum.getScreenFreqMin()+me.getX()*spectrum.getScreenSpan()/cv2.getWidth()
                            - predsPane.getSelectedPredFrequency();
                super.checkPeakAtPosition(me,false);
                if (isPeakSelectionCancelled) {
                    showPredictionAt(predsPane.getSelectedPredFrequency());
                    isPeakSelectionCancelled = false;
                }
            }
            else{
                xvalue = 0.0;
                yvalue = 0.0;
                dxvalue = 0.0;
            }
            labelXY.setText(String.format(Locale.US,
                    "X : %10.3f | Y : %.3E | ∆ : %.3E",
                    xvalue, yvalue, dxvalue));
        }

        private void onKeyPressed(KeyEvent key){
            if (key.isMetaDown()) {
                cv2.cursorProperty().set(Cursor.CROSSHAIR);
                pressedKeys.add(key.getCode());
            }
        }

        private void onMouseScroll(ScrollEvent e){
            if (!e.isControlDown()) return;
            if (e.getDeltaY()<0)
                spectrumPane.verticalZoomIn(1.1);
            else
                spectrumPane.verticalZoomOut(1.1);
        }

        private void onKeyReleased(KeyEvent key){
            cv2.cursorProperty().set(Cursor.DEFAULT);
            pressedKeys.clear();
        }

        private void onMouseClick(MouseEvent me){
            if (isPeakInsertEnabled&&me.getButton()==MouseButton.PRIMARY){
                double freq = spectrum.getScreenFreqMin()+me.getX()*spectrum.getScreenSpan()/cv2.getWidth();
                if (me.isShiftDown()){
                    db.insertValue(freq,0);
                    double fmin = spectrumPane.getSpectrum().getFreqMin();
                    double fmax = spectrumPane.getSpectrum().getFreqMax();
                    db.getPeakData(fmin, fmax); // peak
                    setPeakResultSet(db.getPeaksCache());
                    showPeaks();
                    return;
                }
                MiscData misc = db.getMiscData();
                double lwmult = Double.parseDouble(Main.getProperties().getProperty("lw multiplier"));
                double doppler = lwmult*3.58e-7*freq*Math.sqrt(misc.getTemp()/misc.getMass());
                if (db.getPeakInterval(freq,doppler)) {
                    Alert alert = new Alert(Alert.AlertType.WARNING, "", ButtonType.YES, ButtonType.NO);
                    alert.setTitle("Peak Finder");
                    alert.setHeaderText(null);
                    alert.setContentText(String.format("Peak too close to another one. Abort insertion?"));
                    alert.initOwner(Main.getPrimaryStage());
                    Optional<ButtonType> result = alert.showAndWait();
                    if (result.get() == ButtonType.YES){
                        return;
                    } else {
                        db.insertValue(freq,0);
                        double fmin = spectrumPane.getSpectrum().getFreqMin();
                        double fmax = spectrumPane.getSpectrum().getFreqMax();
                        db.getPeakData(fmin, fmax); // peak
                        setPeakResultSet(db.getPeaksCache());
                        showPeaks();
                        return;
                    }
                }
                spectrum.insertPeak(freq, Main.getPrimaryStage());

                setPeakFinderBounds(freq, doppler);

                double fmin = spectrumPane.getSpectrum().getFreqMin();
                double fmax = spectrumPane.getSpectrum().getFreqMax();
                db.getPeakData(fmin, fmax); // peak
                setPeakResultSet(db.getPeaksCache());
                showPeaks();
            } else {
                checkPeakAtPosition(me, true);
                if (spectrumPane.isPeakSelected) spectrumPane.setSelectedPrediction(predsPane.getSelectedPrediction());
            }
        }

        public void setPeakFinderBounds(double freq, double doppler){
            GraphicsContext gcc = cv2.getGraphicsContext2D();
            double fmin = spectrum.getScreenFreqMin();
            double fmax = spectrum.getScreenFreqMax();
            double leftbound = (freq-doppler-fmin)*cv2.getWidth()/(fmax-fmin);
            double rightbound = (freq+doppler-fmin)*cv2.getWidth()/(fmax-fmin);
            gcc.clearRect(0, 0, cv2.getWidth(), cv2.getHeight());
            gcc.setFill(Color.rgb(250, 250, 0, 0.5));
            gcc.fillRect(leftbound,0, rightbound-leftbound, cv2.getHeight());
        }

        public void setCanvasCursor(Cursor cursor){
            cv1.cursorProperty().setValue(cursor);
            cv2.cursorProperty().setValue(cursor);
        }

        public void showPredictionAt(double freq){
            GraphicsContext gc = cv2.getGraphicsContext2D();
            double fmin = spectrum.getScreenFreqMin();
            double fmax = spectrum.getScreenFreqMax();
            double nix = (freq-fmin)*cv2.getWidth()/(fmax-fmin);
            gc.clearRect(0,0,cv2.getWidth(),cv2.getHeight());
            gc.setStroke(Color.valueOf(Main.getProperties().getProperty("background color")).brighter());
            gc.setLineWidth(0.7);
            gc.setLineDashes(2);
            gc.strokeLine(nix, 2, nix, this.getHeight()-2);
        }

        protected void checkPeakAtPosition(MouseEvent me, boolean onClick){
            super.checkPeakAtPosition(me, onClick);
            if (onClick&&isPeakSelected)
                predsPane.getPredAtCursor(me.getX());
            else
                showPredictionAt(predsPane.getSelectedPredFrequency());
        }
    }

    private class PredsPane extends Pane {

        private Canvas pcv;
        private Canvas procv;
        private Database db;
        private double alphascale = 1.0e-3;
        private double fselected = 0.0;
        private int tselected = -1;
        private int rowcount = -1;
        //private static final int SCROLLHEIGHT = 28;
        //private static final int SCRFONTHEIGHT = SCROLLHEIGHT/2;
        private static final int MARKERSIZE = 8;
        private TextFlow textFlow;
        private double SCROLLHEIGHT;
        private Font predsLabelFont;

        public PredsPane(double width, double height){
            pcv = new Canvas(width,height);
            pcv.widthProperty().bind(this.widthProperty());
            pcv.heightProperty().bind(this.heightProperty());

            procv = new Canvas(width, height);
            procv.widthProperty().bind(this.widthProperty());
            procv.heightProperty().bind(this.heightProperty());

            widthProperty().addListener(((observable, oldValue, newValue) -> showPredictions()));
            heightProperty().addListener(((observable, oldValue, newValue) -> showPredictions()));

            //pcv.addEventHandler(ScrollEvent.SCROLL,this::onMouseScroll);
            //pcv.addEventHandler(MouseEvent.MOUSE_CLICKED,this::onMouseDoubleClick);

            this.addEventHandler(ScrollEvent.SCROLL,this::onMouseScroll);
            this.addEventHandler(MouseEvent.MOUSE_CLICKED,this::onMouseDoubleClick);

            this.getChildren().add(pcv);
            this.getChildren().add(procv);


            db = Main.mainfrm.getDatabase();

            textFlow = new TextFlow();
            textFlow.setLineSpacing(0.);
            textFlow.setPadding(new Insets(0,0,0,0));
            textFlow.setTextAlignment(TextAlignment.CENTER);
            this.getChildren().add(textFlow);

            //predsLabelFont = new Font("Monaco",10);
            //predsLabelFont = Font.loadFont("file:src/fr/ulille/spexp/resources/fonts/CourierPrimeSans.ttf",10);
            predsLabelFont = Font.font("monospaced",10);

            // estimation of the predictions label height (SCROLLHEIGHT) using current predictions label font
            Text textA = new Text(" ");
            Text textB = new Text(" ");
            Text textC = new Text("\n");
            textA.setFont(predsLabelFont);
            textB.setFont(predsLabelFont);
            textC.setFont(predsLabelFont);
            textFlow.getChildren().addAll(textA,textC,textB);
            SCROLLHEIGHT = textFlow.getBoundsInParent().getHeight();

            fselected = 0;
        }

        private Color getRGB(int rgb){
            final int red = (rgb >> 16) & 0xFF;
            final int green = (rgb >> 8) & 0xFF;
            final int blue = rgb & 0xFF;
            return Color.rgb(red, green, blue);
        }

        private void onMouseScroll(ScrollEvent e){
            if (e.isShortcutDown()){
                if (e.getDeltaY()<0) profileZoomIn();
                else profileZoomOut();
            } else {
                if (e.getDeltaY()<0) zoomIn();
                else zoomOut();
            }
        }

        private void onMouseDoubleClick(MouseEvent e){
            if (e.getClickCount()==2)
                getPredAtCursor(e.getX());
        }

        private void zoomIn(){
            alphascale = alphascale*1.2;
            showPredictions();
        }

        private void zoomOut(){
            alphascale = alphascale/1.2;
            showPredictions();
        }

        private void profileZoomIn(){
            yprmin*=1.1;
            yprmax*=1.1;
            showPredictions();
        }

        private void profileZoomOut(){
            yprmin/=1.1;
            yprmax/=1.1;
            showPredictions();
        }

        public void selectNext(){
            if (rowcount<1) return;
            tselected++;
            if (tselected>rowcount) tselected = 1;
            spectrumPane.setSelectedPrediction(tselected);
            try {
                db.predrs.absolute(tselected);
                fselected = db.predrs.getDouble("CFREQ");
            } catch (SQLException e) {
                e.printStackTrace();
            }
            showPredictions();
        }

        public void selectPrev(){
            if (rowcount<1) return;
            tselected--;
            if (tselected<1) tselected = rowcount;
            spectrumPane.setSelectedPrediction(tselected);
            try {
                db.predrs.absolute(tselected);
                fselected = db.predrs.getDouble("CFREQ");
            } catch (SQLException e) {
                e.printStackTrace();
            }
            showPredictions();
        }

        public void showPredictionsProfile(){
            if (!spectrumPane.getSpectrum().isFileRead()) return;

            // this part of the code is a modified Spectrum.getScreenYArray function
            double height = procv.getHeight();
            double yspan = yprmax-yprmin;
            int xaLimit = spectrumPane.getSpectrum().getXA();
            int xbLimit = spectrumPane.getSpectrum().getXB();
            double [] ys = new double[xbLimit-xaLimit+1];
            for (int i=xaLimit;i<=xbLimit;i++){
                ys[i-xaLimit] = height-SCROLLHEIGHT - (height-SCROLLHEIGHT)*(ypr[i]-yprmin)/yspan ;
                if (ys[i-xaLimit]>height) ys[i-xaLimit]=height;
                if (ys[i-xaLimit]<0) ys[i-xaLimit]=0;
            }

            double[] xs = spectrumPane.getSpectrum().getScreenXArray(procv.getGraphicsContext2D());

            GraphicsContext gc = procv.getGraphicsContext2D();
            gc.clearRect(0,0,procv.getWidth(),procv.getHeight());
            Color color = Color.valueOf(Main.getProperties().getProperty("predictions color"));
            gc.setLineWidth(Double.parseDouble(Main.getProperties().getProperty("pen width")));
            spectrumPane.plotXY(procv.getGraphicsContext2D(),xs,ys,xbLimit-xaLimit+1,color);
        }

        public void showPredictions(){
            if (!spectrumPane.getSpectrum().isFileRead()) return;
            double pfr = 0.0;
            double pamax = 0.0;
            int color = 0;

            GraphicsContext gcc = procv.getGraphicsContext2D();
            gcc.clearRect(0,0,procv.getWidth(),procv.getHeight());

            GraphicsContext gc = pcv.getGraphicsContext2D();
            double height = gc.getCanvas().getHeight();
            double width = gc.getCanvas().getWidth();

            double fmin = spectrumPane.getSpectrum().getScreenFreqMin();
            double fmax = spectrumPane.getSpectrum().getScreenFreqMax();
            db.getPredList(fmin,fmax);
            ResultSet prs = db.predrs;
            ResultSet trs = db.tranrs;

            gc.setFill(Color.valueOf(Main.getProperties().getProperty("predictions background")));
            gc.fillRect(0,0,width,height);
            gc.setStroke(Color.BLACK);
            gc.strokeLine(0, height-SCROLLHEIGHT+1, width, height-SCROLLHEIGHT+1);

            rowcount = 0;

            if (isProfileReady&&Boolean.parseBoolean(Main.getProperties().getProperty("plot profile"))) showPredictionsProfile();
            else statusBar.getRightItems().clear();

            try {
                if (fselected<fmin||fselected>fmax){
                    tselected = 1;
                    if (prs.first()) fselected = prs.getDouble("CFREQ");
                    else fselected = 0.0;
                }

                if (prs.first()){
                    pfr = prs.getDouble("CFREQ");
                    pamax = prs.getDouble("AMAX");
                    color = prs.getInt("COLOR");

                    gc.setStroke(getRGB(color));

                    double pix = width*(pfr - fmin) / (fmax - fmin);
                    double piy = (height / 4) * (pamax / alphascale);

                    gc.strokeLine(pix, height-SCROLLHEIGHT, pix, height-piy-SCROLLHEIGHT);
                    rowcount = prs.getRow();
                    if (pfr==fselected) tselected = rowcount;
                    else tselected = -1;
                    if (rowcount==tselected){
                        fillLabel(prs, trs, (int)pix);
                        gc.setFill(getRGB(color));
                        gc.fillOval(pix-MARKERSIZE/2, height-SCROLLHEIGHT-MARKERSIZE/2, MARKERSIZE, MARKERSIZE);
                    }
                }
                while (prs.next()) {
                    rowcount++;
                    pfr = prs.getDouble("CFREQ");
                    pamax = prs.getDouble("AMAX");
                    color = prs.getInt("COLOR");
                    gc.setStroke(getRGB(color));
                    double pix = width*(pfr - fmin) / (fmax - fmin);
                    double piy = (height / 4) * (pamax / alphascale);
                    gc.strokeLine(pix, height-SCROLLHEIGHT, pix, height-piy-SCROLLHEIGHT);
                    if (pfr==fselected) tselected = rowcount;

                    if (rowcount==tselected){
                        fillLabel(prs, trs, (int)pix);
                        gc.setFill(getRGB(color));
                        gc.fillOval(pix-MARKERSIZE/2, height-SCROLLHEIGHT-MARKERSIZE/2, MARKERSIZE, MARKERSIZE);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        private void fillLabel(ResultSet rs, ResultSet trs,  int pix){
            predictedList.clear();
            double txtpos = 0;
            double width = this.getWidth();
            double height = this.getHeight();
            double textwidth = 0.;
            try {
                DbFormat format = db.getDbFormat();
                int qnc = format.getLength();
                int nlines = rs.getInt("NLINES");
                String s = rs.getString("REFS");
                String[] ss = s.split(",");
                String linstr = "";
                String tmpstr = "";
                String labstr = "";
                String frmstr = "";
                ArrayList<Text> label = new ArrayList<>();
                int length = 0;
                if (nlines>2) textFlow.setStyle("-fx-background-color: yellow;");
                else textFlow.setStyle("-fx-background-color: white;");
                for (int j = 0; j < nlines; j++) {
                    int pos = Integer.parseInt(ss[j]);
                    trs.absolute(pos);
                    predictedList.add(db.getPredictedDataRow(pos));
                    linstr = trs.getString(2) + ": ";
                    for (int i = 0; i < format.getLength(); i++) {
                        if (format.getDataType(i) == DbFormat.qnDataType.IntData) {
                            linstr = linstr + String.format(Locale.US, "%3d", trs.getInt(i+3));
                        }
                        if (format.getDataType(i) == DbFormat.qnDataType.FloData) {
                            linstr = linstr + String.format(Locale.US, "%5.1f", trs.getDouble(i+3));
                        }
                        if (format.getDataType(i) == DbFormat.qnDataType.StrData) {
                            linstr = linstr + trs.getString(i+3) + " ";
                        }
                    }
                    linstr = linstr + " -";
                    for (int i = 0; i < format.getLength(); i++) {
                        if (format.getDataType(i) == DbFormat.qnDataType.IntData) {
                            linstr = linstr + String.format(Locale.US, "%3d", trs.getInt(i+3+qnc));
                        }
                        if (format.getDataType(i) == DbFormat.qnDataType.FloData) {
                            linstr = linstr + String.format(Locale.US, "%5.1f", trs.getDouble(i+3+qnc));
                        }
                        if (format.getDataType(i) == DbFormat.qnDataType.StrData) {
                            linstr = linstr + trs.getString(i+3+qnc) + " ";
                        }
                    }
                    linstr = linstr + String.format(Locale.US, "%12.3f", trs.getDouble("FREQ")) + " ";
                    linstr = linstr + String.format(Locale.US, "%3.2E", trs.getDouble("ALPHA"));
                    Text text = new Text();
                    text.setFont(predsLabelFont);
                    text.setText(linstr);
                    text.setBoundsType(TextBoundsType.VISUAL);
                    if (textwidth < text.getBoundsInLocal().getWidth()) textwidth = text.getBoundsInLocal().getWidth();

                    label.add(text);
                    tmpstr = tmpstr + linstr;
                    if (j<=1) labstr = tmpstr;
                    if (j!=nlines-1) tmpstr = tmpstr + "\n";
                    if ((linstr.length()-frmstr.length())> length) {
                        length = linstr.length()-frmstr.length()-4;
                    }
                }

                textFlow.getChildren().clear();
                for (int k=0;k<label.size();k++) {
                    textFlow.getChildren().add(label.get(k));
                    Text textLFCR = new Text("\n");
                    textLFCR.setFont(predsLabelFont);
                    textFlow.getChildren().add(textLFCR);
                }

                double insets = textFlow.getInsets().getLeft()+textFlow.getInsets().getRight();
                textwidth = textwidth+insets;

                Tooltip.install(textFlow,new Tooltip(tmpstr));
                if ((pix - textwidth / 2 > 0) && (pix + textwidth / 2 < width)) {
                    txtpos = pix - textwidth / 2;
                } else {
                    if (pix - textwidth / 2 <= 0) {
                        txtpos = 0;
                    }
                    if (pix + textwidth / 2 > width) {
                        txtpos = width - textFlow.getWidth();
                    }
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            double ypos = height-SCROLLHEIGHT+1.5;

            textFlow.setLayoutX(txtpos);
            textFlow.setLayoutY(ypos);
            textFlow.setPrefWidth(textwidth+10);
            textFlow.setPrefHeight(SCROLLHEIGHT);

            textFlow.setVisible(true);
        }

        public double getSelectedPredFrequency(){
            return fselected;
        }

        public int getSelectedPrediction(){ return tselected; }

        public void getPredAtCursor(double cursorX){
            try {
                double fmin = spectrumPane.getSpectrum().getScreenFreqMin();
                double fmax = spectrumPane.getSpectrum().getScreenFreqMax();
                double pfr = fmin + (cursorX * (fmax - fmin))/this.getWidth();
                double freqlo;
                double freqhi;
                ResultSet rs = db.predrs;
                if (rs.first()) {
                    tselected = 1;
                    freqlo = rs.getDouble("CFREQ");
                    while (rs.next()) {
                        freqhi = rs.getDouble("CFREQ");
                        if ((pfr > freqlo)&(pfr < freqhi)) {
                            if (Math.abs(pfr-freqlo)>Math.abs(pfr-freqhi)) tselected = rs.getRow();
                            else tselected= rs.getRow()-1;
                            break;
                        }
                        freqlo = freqhi;
                    }
                    rs.last();
                    freqhi = rs.getDouble("CFREQ");
                    if (pfr>freqhi) tselected = rs.getRow();

                    spectrumPane.setSelectedPrediction(tselected);
                    rs.absolute(tselected);
                    double prevselect = fselected;
                    fselected = rs.getDouble("CFREQ");
                    if (fselected!=prevselect){
                        showPredictions();
                        spectrumPane.showPredictionAt(fselected);
                    }
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }
}
