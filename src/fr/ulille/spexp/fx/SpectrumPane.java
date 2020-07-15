package fr.ulille.spexp.fx;

import fr.ulille.spexp.data.AssignRowData;
import fr.ulille.spexp.data.Database;
import fr.ulille.spexp.data.PredictRowData;
import fr.ulille.spexp.data.SelectedPeak;
import fr.ulille.spexp.spectrum.Spectrum;
import javafx.fxml.FXMLLoader;
import javafx.geometry.VPos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import javax.sql.rowset.CachedRowSet;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SpectrumPane extends Pane {
    protected Spectrum spectrum;
    protected Canvas cv1;
    protected Canvas cv2;
    protected static final int MAXRENDERINGPOINTS = 16384;
    protected CachedRowSet peakResultSet;
    protected boolean isPeakSelected;
    protected boolean isPeakSelectionCancelled;

    private SelectedPeak selectedPeak;
    private int selectedPrediction;           // row in predrs
    private int selectedPredictedTransition;  // row in tranrs
    private Font assignedPeakDialogFont;
    private double decorationHeight;

    Stage peakDlgStage;
    Stage assignedPeakDlgStage;
    static PeakDialog peakDialog;
    static AssignedPeakDialogNew assignedPeakDialog;

    public SpectrumPane(double width, double height){
        isPeakSelected = false;
        isPeakSelectionCancelled = false;
        // create two canvas for plotting the spectrum (1) and for animated zoom window (2)
        cv1 = new Canvas(width, height);
        cv1.setId("C1");
        cv2 = new Canvas(width, height);
        cv2.setId("C2");
        this.getChildren().add(cv1);
        this.getChildren().add(cv2);
        // create new spectrum connected to the database db
        spectrum = new Spectrum();

        cv1.widthProperty().bind(this.widthProperty());
        cv1.heightProperty().bind(this.heightProperty());
        cv2.widthProperty().bind(this.widthProperty());
        cv2.heightProperty().bind(this.heightProperty());

        widthProperty().addListener((observable, oldValue, newValue) -> showSpectrum());
        heightProperty().addListener((observable, oldValue, newValue) -> showSpectrum());

        selectedPeak = new SelectedPeak();
        selectedPrediction = -1;
        selectedPredictedTransition = -1;

        peakDlgStage = new Stage();
        assignedPeakDlgStage = new Stage();

        assignedPeakDialogFont = Font.font("monospaced",12);

        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("PeakDialog.fxml"));
            Parent tools = fxmlLoader.load();
            peakDialog = fxmlLoader.getController();
            peakDialog.setSpectrumPane(this);
            peakDialog.accSpinner.getValueFactory().
                    setValue(Double.parseDouble(Main.getProperties().getProperty("default uncertainty")));
            peakDlgStage.setScene(new Scene(tools));
            peakDlgStage.initModality(Modality.NONE);
            peakDlgStage.initStyle(StageStyle.UTILITY);
            peakDlgStage.setTitle("Peak");
            peakDlgStage.setResizable(false);
            peakDlgStage.setAlwaysOnTop(true);
            peakDlgStage.setOnCloseRequest(e->{
                spectrum.getSelectedPeak().clearSelected();
                showSpectrum();
            });
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("AssignedPeakDialogNew.fxml"));
            Parent tools = fxmlLoader.load();
            assignedPeakDialog = fxmlLoader.getController();
            assignedPeakDialog.setSpectrumPane(this);
            assignedPeakDlgStage.setScene(new Scene(tools));
            assignedPeakDlgStage.initModality(Modality.NONE);
            assignedPeakDlgStage.initStyle(StageStyle.UTILITY);
            assignedPeakDlgStage.maxWidthProperty().bind(assignedPeakDlgStage.widthProperty());
            assignedPeakDlgStage.minWidthProperty().bind(assignedPeakDlgStage.widthProperty());
            assignedPeakDlgStage.setTitle("Peak");
            assignedPeakDlgStage.setResizable(true);
            assignedPeakDlgStage.setAlwaysOnTop(true);
            assignedPeakDlgStage.setOnCloseRequest(e->{
                spectrum.getSelectedPeak().clearSelected();
                showSpectrum();
            });
            System.out.println(decorationHeight);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void setPeakResultSet(CachedRowSet peakResultSet) {
        this.peakResultSet = peakResultSet;
    }

    protected void plotXY(GraphicsContext gc, double[] xdata, double[] ydata, int npoints, Color color){
        gc.setStroke(color);
        gc.beginPath();

        if (npoints>=MAXRENDERINGPOINTS) {
            double ix = xdata[0];
            final double DIX = 1./2.;  // plotting each 1/2 pixel
            double miny = ydata[0];
            double maxy = ydata[0];
            for (int i = 1; i < npoints; i++) {
                if ((xdata[i] >= ix) && (xdata[i] < ix + DIX)) {
                    if (ydata[i] > maxy) maxy = ydata[i];
                    if (ydata[i] < miny) miny = ydata[i];
                } else {
                    gc.lineTo(ix, miny);
                    gc.lineTo(ix, maxy);
                    ix = xdata[i];
                    miny = ydata[i];
                    maxy = ydata[i];
                }
            }
        }
        else {
            gc.moveTo(xdata[0],ydata[0]);
            for (int i = 1; i < npoints; i++) {
                gc.lineTo(xdata[i], ydata[i]);
            }
        }
        gc.stroke();
    }

    public Spectrum getSpectrum(){
        return spectrum;
    }

    public void setSpectrum(Spectrum spectrum){
        this.spectrum = spectrum;
    }

    public void showSpectrum() {
        if (!spectrum.isFileRead()) return;
        GraphicsContext gc = cv1.getGraphicsContext2D();
        gc.clearRect(0,0, cv1.getWidth(), cv1.getHeight());
        double height = gc.getCanvas().getHeight();
        double width = gc.getCanvas().getWidth();
        int nPoints = spectrum.getScreenSize();

        double[] ixData = spectrum.getScreenXArray(gc);
        double[] iyData = spectrum.getScreenYArray(gc);

        gc.setFill(Color.valueOf(Main.getProperties().getProperty("background color")));
        gc.fillRect(0,0,width,height);
        gc.setLineWidth(Double.parseDouble(Main.getProperties().getProperty("pen width")));
        plotXY(gc,ixData,iyData,nPoints,Color.valueOf(Main.getProperties().getProperty("spectrum color")));
        if (spectrum.isBaselined()){
            double[] iblData = spectrum.getScreenBArray(gc);
            plotXY(gc,ixData,iblData,nPoints,Color.RED);
        }
        cv2.toFront();
        cv2.requestFocus();
    }

    public void drawText(double x, double y, double rot, String text, GraphicsContext gc) {
        gc.save();
        gc.translate(x, y);
        gc.rotate(rot);
        gc.setFill(Color.BLUE);
        gc.fillText(text, 5, 0);
        gc.restore();
    }

    public void showPeaks(){
        if (!spectrum.isFileRead()) return;
        GraphicsContext gc = cv1.getGraphicsContext2D();
        gc.setFont(new Font(gc.getFont().getName(),10));
        gc.setLineDashes(null);
        int length = 0;
        CachedRowSet rs = peakResultSet;
        double peakSize = Double.parseDouble(Main.getProperties().getProperty("peak size"));
        double halfPeakSize = peakSize/2;
        try {
            if (rs==null) return;
            if (!rs.first()) return;
            if (rs.last()) length = rs.getRow();
            else return;
            double freq = 0.0;
            long ipos = 0;
            double ix = 0;
            double iy = 0;
            rs.first();
            for (int j=0;j<length;j++){
                rs.absolute(j+1);
                freq = rs.getDouble("PEAK_FR");
                ix = spectrum.getScreenXCoord(freq,gc);
                ipos = spectrum.getDataIndex(freq);
                iy = spectrum.getScreenYCoord(spectrum.getYData((int)ipos), gc);
                boolean status = rs.getBoolean("STATUS");
                if (!status){
                    gc.setFill(Color.YELLOW);
                    gc.fillOval(ix-halfPeakSize, iy-halfPeakSize, peakSize, peakSize);
                }
                else{
                    gc.setFill(Color.LIME);
                    gc.fillOval(ix-halfPeakSize, iy-halfPeakSize, peakSize, peakSize);
                }
                gc.setStroke(Color.BLUE);
                gc.strokeOval(ix-halfPeakSize, iy-halfPeakSize, peakSize, peakSize);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    protected void checkPeakAtPosition(MouseEvent me, boolean onClick){
        CachedRowSet rs = peakResultSet;
        GraphicsContext gc = cv2.getGraphicsContext2D();
        int length = 0;
        double freq;
        long ipos;
        double ix, iy;
        double positionX = me.getX();
        double positionY = me.getY();
        double peakSize = Double.parseDouble(Main.getProperties().getProperty("peak size"));
        double halfPeakSize = peakSize/2;
        try {
            if (rs.last()) length = rs.getRow();
            else return;
            for (int j=0;j<length;j++){
                rs.absolute(j + 1);
                freq = rs.getDouble("PEAK_FR");
                ipos = Math.round((spectrum.getSize() - 1) * (freq - spectrum.getFreqMin()) / spectrum.getSpan());
                ix = spectrum.getScreenXCoord(freq,gc);
                iy = spectrum.getScreenYCoord(spectrum.getYData((int)ipos), gc);
                if ((positionX>=ix-halfPeakSize)&&(positionX<=ix+halfPeakSize)&&
                        (positionY>=iy-halfPeakSize)&&(positionY<=iy+halfPeakSize)){
                    gc.setLineDashes(0);
                    gc.setStroke(Color.RED);
                    gc.strokeOval(ix-halfPeakSize-1, iy-halfPeakSize-1, peakSize+2, peakSize+2);
                    if (onClick){
                        boolean isAssigned = rs.getBoolean("STATUS");
                        if (!isAssigned){
                            if (assignedPeakDlgStage.isShowing()) assignedPeakDlgStage.hide();
                            setPeakDialog(me.getSceneX(), me.getSceneY());
                            int idx = rs.getInt("ID");
                            spectrum.getSelectedPeak().setFrequency(freq);
                            spectrum.getSelectedPeak().setIndex((int) ipos);
                            spectrum.getSelectedPeak().setAbsoluteDbIndex(idx);
                            spectrum.getSelectedPeak().setRelativeDbIndex(j+1);
                            selectedPeak = spectrum.getSelectedPeak();
                        }
                        else {
                            if (peakDlgStage.isShowing()) peakDlgStage.hide();
                            setAssignedPeakDialog(me.getSceneX(), me.getSceneY());
                            int idx = rs.getInt("ID");
                            spectrum.getSelectedPeak().setFrequency(freq);
                            spectrum.getSelectedPeak().setIndex((int) ipos);
                            spectrum.getSelectedPeak().setAbsoluteDbIndex(idx);
                            spectrum.getSelectedPeak().setRelativeDbIndex(j + 1);
                            selectedPeak = spectrum.getSelectedPeak();
                        }
                    }
                    isPeakSelected = true;
                    return;
                }
            }
            if (isPeakSelected){
                gc.clearRect(0,0,cv2.getWidth(), cv2.getHeight());
                isPeakSelectionCancelled = true;
                isPeakSelected = false;
            }
            if (onClick&&(peakDlgStage.isShowing()||assignedPeakDlgStage.isShowing())){
                spectrum.getSelectedPeak().clearSelected();
                peakDlgStage.hide();
                assignedPeakDlgStage.hide();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void setPeakDialog(double positionX, double positionY) throws SQLException {
        CachedRowSet rs = peakResultSet;
        Label lab = (Label) peakDlgStage.getScene().lookup("#flab");
        peakDlgStage.setTitle(String.format(Locale.US,"%.3f",rs.getDouble("PEAK_FR")));
        if (rs.getDouble("GAUSS_FR")!=0){
            lab.setText(String.format(Locale.US,"%.3f +/-",rs.getDouble("GAUSS_FR")));
            lab.setTooltip(new Tooltip(String.format(Locale.US,"%.3f",rs.getDouble("PEAK_FR"))));
            lab.setTextFill(Color.BLUE);
        }
        else{
            lab.setText(String.format(Locale.US,"%.3f +/-",rs.getDouble("PEAK_FR")));
            lab.setTooltip(new Tooltip(String.format(Locale.US,"%.3f",rs.getDouble("GAUSS_FR"))));
            lab.setTextFill(Color.RED);
        }
        double psx = cv1.getParent().getScene().getWindow().getX()+positionX;
        double psy = cv1.getParent().getScene().getWindow().getY()+positionY;
        peakDlgStage.setX(psx);
        peakDlgStage.setY(psy);
        peakDialog.accSpinner.getValueFactory().
                setValue(Double.parseDouble(Main.getProperties().getProperty("default uncertainty")));
        peakDlgStage.show();
    }

    private void setAssignedPeakDialog(double positionX, double positionY) throws SQLException {
        AssignRowData row = null;
        CachedRowSet rs = peakResultSet;
        String s;
        String str = "";
        double textwidth = 0.;
        assignedPeakDlgStage.setTitle(String.format(Locale.US,"%.3f",rs.getDouble("PEAK_FR")));
        ArrayList<Text> assignment = new ArrayList<>();
        try {
            // filling in the list with assigned transitions and preparing the dialog to show
            s = rs.getString("REFS");
            String[] ss = {""};
            if (!s.isEmpty()) ss = s.split(",");
            int size = ss.length;
            int ref = Integer.parseInt(ss[0]);
            row = Main.mainfrm.getDatabase().getADataRow(ref);
            Text text1 = new Text();
            text1.setFont(assignedPeakDialogFont);
            str = row.toString();
            text1.setText(str);
            if (textwidth < text1.getBoundsInLocal().getWidth()) textwidth = text1.getBoundsInLocal().getWidth();
            assignment.add(text1);
            for (int i=1;i<size;i++){
                ref = Integer.parseInt(ss[i]);
                row = Main.mainfrm.getDatabase().getADataRow(ref);
                str = row.toString();
                Text text2 = new Text();
                text2.setFont(assignedPeakDialogFont);
                text2.setText(str);
                if (textwidth < text2.getBoundsInLocal().getWidth()) textwidth = text2.getBoundsInLocal().getWidth();
                assignment.add(text2);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        double psx = cv1.getParent().getScene().getWindow().getX()+positionX;
        double psy = cv1.getParent().getScene().getWindow().getY()+positionY;
        assignedPeakDlgStage.hide();
        assignedPeakDlgStage.setX(psx);
        assignedPeakDlgStage.setY(psy);

        assignedPeakDialog.listView.getItems().clear();
        for (Text t:assignment){
            assignedPeakDialog.listView.getItems().add(t);
        }

        assignedPeakDlgStage.show();

        //decorationHeight = assignedPeakDlgStage.getHeight()-assignedPeakDlgStage.getScene().getHeight();
        assignedPeakDlgStage.setWidth(textwidth+20); // check what to do with +20
        //assignedPeakDlgStage.setHeight(assignedPeakDialog.getCellHeight()*(assignment.size()+1)+
        //        assignedPeakDialog.tool.getHeight()+decorationHeight+5);
    }

    public void assignPeak(double wght){
        int refs[];
        if (selectedPredictedTransition>0){
            refs = new int[1];
            refs[0] = selectedPredictedTransition;
        }
        else
            refs = Main.mainfrm.getDatabase().getPredictedReferences(selectedPrediction);

        int selectedprow = selectedPeak.getRelativeDbIndex();
        String astr = Main.mainfrm.getDatabase().getAssignedReferencesAsString(selectedprow);
        if (!astr.isEmpty()) astr+=",";

        double freq = Main.mainfrm.getDatabase().getPeakFittedFrequency(selectedprow);
        if (freq==0){
            freq = Main.mainfrm.getDatabase().getPeakFrequency(selectedprow);
            wght*=3;
        }

        for (int j=0;j<refs.length;j++){
            PredictRowData row = Main.mainfrm.getDatabase().getPredictedDataRow(refs[j]);
            int irow = Main.mainfrm.getDatabase().insertADataRow(row, freq, wght, 1.0);
            astr = astr + irow;
            if (j!=refs.length-1) astr = astr + ",";
        }
        int peakid = selectedPeak.getAbsoluteDbIndex();
        Main.mainfrm.getDatabase().updatePeakAssignment(peakid, astr, true);
    }

    public void updateRelativeIntensities(){
        int selectedprow = selectedPeak.getRelativeDbIndex();
        Database db = Main.mainfrm.getDatabase();
        List<AssignRowData> assignedList = new ArrayList<>();
        String references = db.getAssignedReferencesAsString(selectedprow);
        if (references.isEmpty()){
            //System.out.println("no references !");
            return;
        }
        String[] ss = references.split(",");
        int size = ss.length;
        assignedList.clear();
        for (int i=0;i<size;i++){
            int ref = Integer.parseInt(ss[i]);
            AssignRowData row = db.getADataRow(ref);
            assignedList.add(row);
        }
        ArrayList<Double> intens = new ArrayList<>();
        for (AssignRowData adata: assignedList){
            PredictRowData row = db._findTransition(adata.getTransition());
            intens.add(row.getIntensity());
            //System.out.println(row.getId());
        }
        double sum = 0;
        for (double a : intens) sum +=a;
        for (int i=0;i<size;i++){
            int ref = Integer.parseInt(ss[i]);
            db.updateRelativeIntensity(intens.get(i)/sum, ref);
        }
    }

    public void addAssignment(){
        int selectedprow = selectedPeak.getRelativeDbIndex();
        Database db = Main.mainfrm.getDatabase();
        List<AssignRowData> assignedList = new ArrayList<>();
        String[] ss = db.getAssignedReferencesAsString(selectedprow).split(",");
        int size = ss.length;
        assignedList.clear();
        for (int i=0;i<size;i++){
            int ref = Integer.parseInt(ss[i]);
            AssignRowData row = db.getADataRow(ref);
            assignedList.add(row);
        }
        double weight = assignedList.get(0).getWeight();
        assignPeak(weight);
    }

    public void deleteAssignment(){
        Database db = Main.mainfrm.getDatabase();
        int apos = selectedPeak.getAbsoluteDbIndex();
        int rpos = selectedPeak.getRelativeDbIndex();

        int[] refs = db.getAssignedReferences(rpos);
        for (int i=0;i<refs.length;i++)
            db.deleteAssignmentRow(refs[i]);
        db.updatePeakAssignment(apos, "", false); // remove the assignment
    }

    public void setSelectedPrediction(int row){
        selectedPrediction = row;
    }

    public void setSelectedPredictedTransition(int selectedPredictedTransition) {
        this.selectedPredictedTransition = selectedPredictedTransition;
    }

    public void updatePeaks(){
        Database db = Main.mainfrm.getDatabase();
        db.getPeakData(spectrum.getFreqMin(),spectrum.getFreqMax()); // !!!
        setPeakResultSet(Main.mainfrm.getDatabase().getPeaksCache());
        updateRelativeIntensities();
    }

    private void setAlert(String message, Window window){
        Alert alert = new Alert(Alert.AlertType.WARNING, "", ButtonType.YES, ButtonType.NO);
        alert.setTitle("Peak");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(window);
    }

    public void showMessage(String message){
        GraphicsContext gc = cv1.getGraphicsContext2D();
        double height = gc.getCanvas().getHeight();
        double width = gc.getCanvas().getWidth();
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.setStroke(Color.DARKGRAY);
        gc.setFill(Color.LIGHTGRAY);
        gc.fillRect(0,0, width, height);
        gc.setLineDashes(null);
        gc.strokeText(message,width/2,height/2);
    }

    public void test(){
        GraphicsContext gc = cv1.getGraphicsContext2D();
        gc.setStroke(Color.RED);
        gc.strokeLine(0,0,50,100);
    }

    public void verticalZoomIn(double factor){
        spectrum.vertZoomIn(factor);
        showSpectrum();
    }

    public void verticalZoomOut(double factor){
        spectrum.vertZoomOut(factor);
        showSpectrum();
    }

    public void verticalZoom(double factor){
        spectrum.vertZoom(factor);
    }

    public void deletePeak(){
        Database db = Main.mainfrm.getDatabase();
        int id = selectedPeak.getAbsoluteDbIndex();
        db.deleteSelectedPeak(id);
    }

}
