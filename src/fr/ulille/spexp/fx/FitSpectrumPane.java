package fr.ulille.spexp.fx;

import fr.ulille.spexp.data.Database;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import fr.ulille.spexp.fftprofile.*;
import javafx.scene.text.TextAlignment;

import javax.sql.rowset.CachedRowSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;

public class FitSpectrumPane extends SpectrumPane {

    private final static int COMMONS_NUM = 2;
    private static final int A_CHARACTER = 0x41;
    private boolean isBaselineBoundsInsert;
    private boolean isLineInsert;
    private ArrayList<Integer> boundsList;
    private LineProfile fitprofile;
    private ProfileFitter profileFitter;
    private double[] xvalues;
    private double[] yvalues;
    private int psize;
    private Baseline baseline;
    private Function model;
    private Derivative derivative;
    private Database db;

    public FitSpectrumPane(double width, double height) {
        super(width, height);
        boundsList = new ArrayList<>();
        cv2.setOnMouseClicked(this::onMouseClick);
    }

    public void onMouseClick(MouseEvent me){
        if (isBaselineBoundsInsert){
            if (boundsList.size()>=4){
                isBaselineBoundsInsert = false;
                Main.mainfrm.getBaslineSelectButton().setSelected(false);
                return;
            }
            double pos = me.getX();
            int index = (int)(spectrum.getXA()+pos*(spectrum.getXB()-spectrum.getXA())/cv1.getWidth());
            boundsList.add(index);
            double ypos = spectrum.getScreenYValue(cv2.getGraphicsContext2D(),index);
            double xpos = spectrum.getScreenXValue(cv2.getGraphicsContext2D(),index);
            GraphicsContext gc = cv2.getGraphicsContext2D();
            gc.setStroke(Color.RED);
            gc.strokeLine(xpos,ypos-5,xpos,ypos+5);
            char stroke = (char)(A_CHARACTER+boundsList.size()-1);
            gc.strokeText(String.valueOf(stroke),xpos,ypos-10);

            if (boundsList.size()==4){
                isBaselineBoundsInsert = false;
                Main.mainfrm.getBaslineSelectButton().setSelected(false);
                Collections.sort(boundsList);
                int imin = boundsList.get(0);
                int imax = boundsList.get(3);
                psize = imax-imin+1;
                xvalues = new double[psize];
                yvalues = new double[psize];
                for (int i=0;i<psize;i++){
                    xvalues[i] = spectrum.getXData(i+imin);
                    yvalues[i] = spectrum.getYData(i+imin);
                }
                double dw = spectrum.getDopplerWidth(spectrum.getXData(boundsList.get(1)))/(xvalues[1]-xvalues[0]);

                db.getPeakData(xvalues[0], xvalues[psize-1]);
                peakResultSet = db.getPeaksCache();

                CommonParameters commons = new CommonParameters("Doppler width",
                        spectrum.getDopplerWidth(spectrum.getXData(boundsList.get(1))));
                Main.mainfrm.getCommonsTable().getItems().add(commons);
                CommonParameters commons1 = new CommonParameters("Lorentz width",
                        spectrum.getDopplerWidth(spectrum.getXData(boundsList.get(1)))/2.);
                Main.mainfrm.getCommonsTable().getItems().add(commons1);

                baseline = new Baseline(spectrum.getYData(),boundsList.stream().mapToInt(Integer::intValue).toArray());
                fitprofile = new LineProfile(model, derivative, baseline,
                        dw, dw/2., psize, xvalues[0], xvalues[1]-xvalues[0]);
                showProfile();
                return;
            }
        }

        if (isLineInsert){
            if (fitprofile==null) return;
            double f = spectrum.getScreenFreqMin()+me.getX()*spectrum.getScreenSpan()/cv2.getWidth();
            long pos = spectrum.getDataIndex(f)-boundsList.get(0);
            System.out.println(pos);
            if (pos<0||pos>psize) return;
            fitprofile.addLine(pos-psize/2,1.);
            // initial amplitude estimation
            //-----------------------------------
            double[] pf = fitprofile.getAmplitudeDerivative(fitprofile.getLines()-1);
            double s1 = 0;
            double s2 = 0;
            for (int i=0;i<pf.length;i++){
                s2+=pf[i]*pf[i];
                s1+=pf[i]*yvalues[i];
            }
            double amp = s1/s2;
            if (amp>0) fitprofile.setAmplitude(fitprofile.getLines()-1,amp);
            showProfile();
            LineParameters pars = fitprofile.getLineParameters(fitprofile.getLines());
            Main.mainfrm.getParamsTable().getItems().add(pars);
            Main.mainfrm.getParamsTable().getSelectionModel().select(fitprofile.getLines()-1);
        }
    }

    public void insertFromDatabase(){
        int length = 0;
        CachedRowSet rs = peakResultSet;
        try {
            if (rs == null||!rs.first()) return;
            if (rs.last()) length = rs.getRow();
            else return;
            double freq = 0.0;
            rs.first();
            for (int j = 0; j < length; j++) {
                rs.absolute(j + 1);
                freq = rs.getDouble("PEAK_FR");
                long pos = spectrum.getDataIndex(freq)-boundsList.get(0);
                if (pos<0||pos>psize) return;
                fitprofile.addLine(pos-psize/2,1.);
                // initial amplitude estimation
                //-----------------------------------
                double[] pf = fitprofile.getAmplitudeDerivative(fitprofile.getLines()-1);
                double s1 = 0;
                double s2 = 0;
                for (int i=0;i<pf.length;i++){
                    s2+=pf[i]*pf[i];
                    s1+=pf[i]*yvalues[i];
                }
                double amp = s1/s2;
                if (amp>0) fitprofile.setAmplitude(fitprofile.getLines()-1,amp);
                showProfile();
                LineParameters pars = fitprofile.getLineParameters(fitprofile.getLines());
                Main.mainfrm.getParamsTable().getItems().add(pars);
                Main.mainfrm.getParamsTable().getSelectionModel().select(fitprofile.getLines()-1);

            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updatePeaks(){
        CachedRowSet rs = peakResultSet;
        int length = 0;
        try {
            if (rs == null || !rs.first()) return;
            if (rs.last()) length = rs.getRow();
            for (int i = 0; i < length; i++) {
                rs.absolute(i + 1);
                int id = rs.getInt("ID");
                db.updatePeakFrequency(id,Main.mainfrm.getParamsTable().getItems().get(i).getFrequency());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setBaselineBoundsInsert(boolean baselineBoundsInsert) {
        isBaselineBoundsInsert = baselineBoundsInsert;
    }

    public void setLineInsert(boolean lineInsert) {
        isLineInsert = lineInsert;
    }

    @Override
    public void showSpectrum() {
        super.showSpectrum();
        super.showPeaks();
        //showScale();
        showProfile();
    }

    public void showScale(){
        GraphicsContext gc = cv1.getGraphicsContext2D();
        int nstep = 10;
        double step = (spectrum.getScreenFreqMax()-spectrum.getScreenFreqMin())/(nstep-1);
        for (int i = 1;i<nstep-1;i++){
            double x = spectrum.getScreenXCoord(spectrum.getScreenFreqMin()+i*step,gc);
            gc.setStroke(Color.BLACK);
            gc.strokeLine(x,cv1.getHeight(),x,cv1.getHeight()-5);
        }
    }

    public void showProfile(){
        if (fitprofile!=null){
            GraphicsContext gc = cv2.getGraphicsContext2D();
            gc.clearRect(0,0, cv2.getWidth(), cv2.getHeight());
            double[] yf = fitprofile.getProfile();
            double[] xs = spectrum.getScreenXArray(gc,xvalues);
            double[] ys = spectrum.getScreenYArray(gc,yf);
            this.plotXY(gc,xs,ys,psize,Color.RED);
            gc.setStroke(Color.RED);
            gc.setTextAlign(TextAlignment.CENTER);
            for (int i=0;i<fitprofile.getLines();i++){
                double xl = spectrum.getScreenXCoord(fitprofile.getFrequency(i),gc);
                gc.strokeText(String.valueOf(i+1),xl,10);
            }
        }
    }

    public Baseline getBaseline(){
        return baseline;
    }

    public void updateFrequency(int index, double value){
        fitprofile.setFrequency(index-1, value);
        showProfile();
    }

    public void updateAmplitude(int index, double value){
        fitprofile.setAmplitude(index-1, value);
        showProfile();
    }

    public void updateDopplerWidth(double value){
        fitprofile.setDopplerWidth(value);
        showProfile();
    }

    public void updateLorentzWidth(double value){
        fitprofile.setLorentzWidth(value);
        showProfile();
    }

    public ProfileFitter getProfileFitter(){
        return profileFitter;
    }

    public void fitProfile(){
        profileFitter = new ProfileFitter(fitprofile, yvalues);
        profileFitter.fit();
        if (profileFitter.isConverged()){
            for (int i=0;i<fitprofile.getLines();i++){
                LineParameters pars = fitprofile.getLineParameters(i+1);
                Main.mainfrm.getParamsTable().getItems().set(i,pars);
            }
            for (int i=0;i<COMMONS_NUM;i++){
                CommonParameters pars = fitprofile.getCommonParameters(i);
                Main.mainfrm.getCommonsTable().getItems().set(i,pars);
            }
            fitprofile.setProfileParameters(profileFitter.getOptimizedParametersVector());
            showProfile();
        }
    }

    public void resetProfile(){
        fitprofile = null;
        baseline = null;
        isLineInsert = false;
        isBaselineBoundsInsert = false;
        GraphicsContext gc = cv2.getGraphicsContext2D();
        gc.clearRect(0,0, cv2.getWidth(), cv2.getHeight());
        Main.mainfrm.getParamsTable().getItems().clear();
        Main.mainfrm.getCommonsTable().getItems().clear();
        boundsList.clear();
    }

    public void setModel(Function function, Derivative derivative){
        this.model = function;
        this.derivative = derivative;
    }

    public void removeLine(int index){
        fitprofile.removeLine(index);
        showProfile();
        Main.mainfrm.getParamsTable().getItems().clear();
        for (int i=0;i<fitprofile.getLines();i++){
            LineParameters pars = fitprofile.getLineParameters(i+1);
            Main.mainfrm.getParamsTable().getItems().add(pars);
        }
    }

    public void setDatabase(Database database){
        this.db = database;
        double fmin = spectrum.getFreqMin();
        double fmax = spectrum.getFreqMax();
        db.getPeakData(fmin, fmax); // peaks
        setPeakResultSet(db.getPeaksCache());
    }
}
