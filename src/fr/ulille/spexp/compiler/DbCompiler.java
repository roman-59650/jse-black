package fr.ulille.spexp.compiler;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import fr.ulille.spexp.data.Database;
import fr.ulille.spexp.data.PredictRowData;
import fr.ulille.spexp.fx.Main;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import org.controlsfx.control.StatusBar;
import fr.ulille.spexp.spectrum.FileInfo;

/**
 *
 * @author Roman
 */
public class DbCompiler extends Task {

    private static final int STRINGS = 500000;
    private static final int ASRSTARTST = 11;
    private static final int ASRMISS1   = 17;
    private static final int ASRMISS2   = 12;
    private static final int ASRBLOCK   = 11;
    private static final int RAM36START = 10;
    private String filename;
    private ObservableList<CompilationData> data;
    private Database db;
    private PredictRowData rowdata;
    private String[] inputstr; 
    private static int strcount;
    private List<String> strlist;
    private List<String> files;
    private StatusBar statusBar;
    private double progressSize;
    private MiscData misc;
    private List<FileInfo> fileinfo;
    private boolean isCompiled;

    public DbCompiler(ObservableList<CompilationData> data, Database db, StatusBar bar){
        this.data = data;
        this.db = db;
        inputstr = new String[STRINGS];
        strcount = 0;
        rowdata = new PredictRowData(db.getDbFormat());
        statusBar = bar;
        misc = db.getMiscData();
        fileinfo = new ArrayList<>();
        statusBar.textProperty().bind(this.messageProperty());
        statusBar.progressProperty().bind(this.progressProperty());
        isCompiled = false;
    }

    private void getFiles(){
        Predicate<String> isDatFile = f -> f.endsWith(".dat")||f.endsWith(".000");
        try (Stream<Path> list= Files.list(Paths.get(misc.getPath()))) {
            files = list.filter(Files::isRegularFile)
                    .map(x -> x.toString())
                    .filter(isDatFile)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isCompiled() {
        return isCompiled;
    }

    public void compileData(){

        updateMessage("Reading spectra...");
        getFiles();
        fileinfo.clear();
        for (String s : files) {
            fileinfo.add(new FileInfo(s));
        }
        db.setFileData(fileinfo);

        updateMessage("Preparing compilation...");
        db.clearSpTable();

        for (int i=0;i<data.size();i++){
            boolean iscomp = false;
            iscomp = data.get(i).getStatus();
            if (iscomp){
                String fileName = data.get(i).getPath();
                String species = data.get(i).getAlias();

                int color = data.get(i).getColor();
                double iscale = data.get(i).getIntensity();

                updateMessage("Reading: "+Paths.get(fileName).getFileName().toString());
                ReadToList(fileName);
                updateMessage("Compiling: "+Paths.get(fileName).getFileName().toString());
                if (fileName.endsWith(".asr"))
                    ASRCompile(fileName, species, color, iscale);
                if (fileName.endsWith(".cat"))
                    CATCompile(fileName, species, color, iscale);
                if (fileName.endsWith(".ram"))
                    RAM36Compile(fileName, species, color, iscale);
                if (fileName.endsWith(".vhf"))
                    RAM36HFCompile(fileName, species, color, iscale);
                if (fileName.endsWith(".ikf"))
                    IKCompile(species, color, iscale);
            }
        }
        updateMessage("Merging...");
        int rows = db.setCompiledPreds(""); // compile predictions for stick spectra without any filter
        if (rows>0) {
            updateMessage("Done. Predictions compiled: " + rows);
            isCompiled = true;
        } else {
            updateMessage("Compilation failed :(");
            isCompiled = false;
        }
    }
    
    /**
     * 
     * @param fileName - predictions file name
     * @return inputstr - array of read strings
     * @return strcount - number of read strings
     */
    private void ReadPredFile(String fileName){
        BufferedReader inputStream = null;
        for (int i=0;i<STRINGS-1;i++) inputstr[i] = "";
        strcount = 0;
        try {
            inputStream = new BufferedReader(new FileReader(fileName));
            int count = 0;
            String line = inputStream.readLine();
            inputstr[count] = line;
            while (line != null) {
                count++;
                line = inputStream.readLine();
                inputstr[count] = line;
            }
            strcount = count;
        } catch (IOException ex) {
            Logger.getLogger(DbCompiler.class.getName()).log(Level.SEVERE, null, ex);
        }
        finally {
            try {
                inputStream.close();
            } catch (IOException ex) {
                Logger.getLogger(DbCompiler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }        
    }

    /**
     * Reads the predictions file into @param strlist list
     * @param fileName - file name to read
     * @return strlist
     */
    private void ReadToList(String fileName){
        try {
            strlist = Files.readAllLines(Paths.get(fileName), Charset.defaultCharset());
            progressSize = strlist.size()-1;
            //System.out.println(progressSize);
        } catch (IOException ex) {
            Logger.getLogger(DbCompiler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void IKCompile(String species, int color, double iscale){
        double T = misc.getTemp();
        double Qrs = misc.getQfunc();

/*        int i = 0;
        for (String s:strlist){
            String[] ss = s.split("\\s+");
            if (ss.length==22){
                System.out.println(s);
                double elo = Double.parseDouble(ss[17]);
                double eup = Double.parseDouble(ss[18]);
                if (elo>eup){
                    double a = eup;
                    eup=elo;
                    elo=a;
                }
                double sm2 = Double.parseDouble(ss[19]);
                double g = Double.parseDouble(ss[21]);
                double f = Double.parseDouble(ss[13].substring(0,ss[13].length()-2));
                if (f<1.) continue;
                double its = 4.16231e-05*f*sm2*(Math.exp(-1.438*elo/T)-Math.exp(-1.438*eup/T))*g/Qrs;
                if (its*iscale<misc.getIcutoff()) continue;
                rowdata.setFrequency(f);
                rowdata.setIntensity(its*iscale);
                rowdata.setColor(color);

                rowdata.setQns(0, Integer.parseInt(ss[1]));
                rowdata.setQns(1, Integer.parseInt(ss[2]));
                rowdata.setQns(2, Integer.parseInt(ss[3]));
                rowdata.setQns(3, Integer.parseInt(ss[4]));
                rowdata.setQns(4, ss[5]);

                rowdata.setQns(5, Integer.parseInt(ss[6]));
                rowdata.setQns(6, Integer.parseInt(ss[7]));
                rowdata.setQns(7, Integer.parseInt(ss[8]));
                rowdata.setQns(8, Integer.parseInt(ss[9]));
                rowdata.setQns(9, ss[10]);
                rowdata.setSpecies(species);
                db.insertPDataRow(rowdata);
                //System.out.println(i+ " "+f);
            }
            i++;
            updateProgress(i, progressSize);
        }*/

        int omit = 0;
        for (int i=0;i<strlist.size()-1;i++){
            String s = strlist.get(i);
            if (s.length()>6)
                if (s.substring(0,6).trim().equals("VT")){
                    omit = i+2;
                    break;
                }
        }

        for (int i=omit;i<strlist.size()-1;i++){
            String str = strlist.get(i);
            if (str.substring(0,22).trim().equals("STD.DEV.(UNITLESS)")) {
                System.out.println(i);
                break;
            }
            if (!str.substring(122,130).trim().isEmpty()) continue;   // skip assigned transitions

            double elo = Double.parseDouble(str.substring(112,121));
            double eup = Double.parseDouble(str.substring(102,111));
            if (elo>eup){
                double a = eup;
                eup=elo;
                elo=a;
            }
            double sm2 = Double.parseDouble(str.substring(133,141));
            double g = Double.parseDouble(str.substring(152,155));
            double f = Double.parseDouble(str.substring(58,69));
            if (f<1000.) continue;                                  // skip IR data (frequency < 1000)
            double its = 4.16231e-05*f*sm2*(Math.exp(-1.438*elo/T)-Math.exp(-1.438*eup/T))*g/Qrs;
            if (its*iscale<misc.getIcutoff()) continue;
            rowdata.setFrequency(f);
            rowdata.setIntensity(its*iscale);
            rowdata.setColor(color);

            rowdata.setQns(0, Integer.parseInt(str.substring(0,4).trim()));
            rowdata.setQns(1, Integer.parseInt(str.substring(5,8).trim()));
            rowdata.setQns(2, Integer.parseInt(str.substring(9,12).trim()));
            rowdata.setQns(3, Integer.parseInt(str.substring(13,16).trim()));
            rowdata.setQns(4, str.substring(16,19).trim());

            rowdata.setQns(5, Integer.parseInt(str.substring(20,23).trim()));
            rowdata.setQns(6, Integer.parseInt(str.substring(24,27).trim()));
            rowdata.setQns(7, Integer.parseInt(str.substring(28,31).trim()));
            rowdata.setQns(8, Integer.parseInt(str.substring(32,35).trim()));
            rowdata.setQns(9, str.substring(36,39));
            rowdata.setSpecies(species);
            db.insertPDataRow(rowdata);
            updateProgress(i, progressSize);
        }
    }

    private void ASRCompile(String fileName, String species, int color, double iscale){
        int jj = ASRSTARTST;
        int len = strlist.get(jj).length();
        while(len!=0){
            jj++;
            len = strlist.get(jj).length();
        }
        int ststr = jj+ASRMISS1+1;
        for (int i=ststr;i<strlist.size()-ASRMISS1;i++){
            String str = strlist.get(i);
            if (!str.isEmpty()){
                String strdint = String.copyValueOf(str.toCharArray(),16,9);
                if (Double.parseDouble(strdint)*iscale<misc.getIcutoff()) continue;
                rowdata.setIntensity(Double.parseDouble(strdint)*iscale);
                rowdata.setColor(color);
                if (str.charAt(25)!='D'){
                    int k = 26;
                    for (int q=1;q<=3;q++){
                        String strqn = String.copyValueOf(str.toCharArray(),k,3);
                        rowdata.setQns(q-1, Integer.parseInt(strqn.trim()));
                        k = k + 4;
                    }
                    k = 39;
                    for (int q=4;q<=6;q++){
                        String strqn = String.copyValueOf(str.toCharArray(),k,3);
                        rowdata.setQns(q-1, Integer.parseInt(strqn.trim()));
                        k = k + 4;
                    }
                    String strfr = String.copyValueOf(str.toCharArray(),0,15);
                    rowdata.setFrequency(Double.parseDouble(strfr));
                    rowdata.setSpecies(species);
                    db.insertPDataRow(rowdata);
                }
                if (str.charAt(25)=='D'){
                    String strqn = String.copyValueOf(str.toCharArray(),26,3);
                    rowdata.setQns(0, Integer.parseInt(strqn.trim()));
                    strqn = String.copyValueOf(str.toCharArray(),39,3);
                    rowdata.setQns(3, Integer.parseInt(strqn.trim()));
                    if (str.substring(31,33).equals("  ")){
                        strqn = String.copyValueOf(str.toCharArray(),34,3);
                        rowdata.setQns(2, Integer.parseInt(strqn.trim()));
                        strqn = String.copyValueOf(str.toCharArray(),47,3);
                        rowdata.setQns(5, Integer.parseInt(strqn.trim()));
                        rowdata.setQns(1, (Integer)rowdata.getQns(0)
                                          -(Integer)rowdata.getQns(2)+1);
                        rowdata.setQns(4, (Integer)rowdata.getQns(3)
                                          -(Integer)rowdata.getQns(5)+1);
                        String strfr = String.copyValueOf(str.toCharArray(),0,15);
                        rowdata.setFrequency(Double.parseDouble(strfr));
                        rowdata.setSpecies(species);
                        db.insertPDataRow(rowdata);
                        rowdata.setQns(1, (Integer)rowdata.getQns(0)
                                          -(Integer)rowdata.getQns(2));
                        rowdata.setQns(4, (Integer)rowdata.getQns(3)
                                          -(Integer)rowdata.getQns(5));
                        db.insertPDataRow(rowdata);
                    }
                    if (str.substring(35,37).equals("  ")){
                        strqn = String.copyValueOf(str.toCharArray(),30,3);
                        rowdata.setQns(1, Integer.parseInt(strqn.trim()));
                        strqn = String.copyValueOf(str.toCharArray(),43,3);
                        rowdata.setQns(4, Integer.parseInt(strqn.trim()));
                        rowdata.setQns(2, (Integer)rowdata.getQns(0)
                                          -(Integer)rowdata.getQns(1)+1);
                        rowdata.setQns(5, (Integer)rowdata.getQns(3)
                                          -(Integer)rowdata.getQns(4)+1);
                        String strfr = String.copyValueOf(str.toCharArray(),0,15);
                        rowdata.setFrequency(Double.parseDouble(strfr));
                        rowdata.setSpecies(species);
                        db.insertPDataRow(rowdata);
                        rowdata.setQns(2, (Integer)rowdata.getQns(0)
                                          -(Integer)rowdata.getQns(1));
                        rowdata.setQns(5, (Integer)rowdata.getQns(3)
                                          -(Integer)rowdata.getQns(4));
                        db.insertPDataRow(rowdata);
                    }
                }
            } // if str.isEmpty()
            updateProgress(i, progressSize);
        }
    } // ASRCompile
    
    private void CATCompile(String fileName, String species, int color, double iscale){
        for (int i=0;i<strlist.size()-1;i++){
            String str = strlist.get(i);
            String strdint = String.copyValueOf(str.toCharArray(),22,8);
            double dint = Double.parseDouble(strdint);
            dint = Math.pow(10, dint);
            if (dint*iscale<misc.getIcutoff()) continue;
            rowdata.setIntensity(dint*iscale);
            rowdata.setColor(color);
            int qnlength = rowdata.getQnsLength();
            int k = 55;
            for (int q=0;q<qnlength/2;q++){
                String strqn = String.copyValueOf(str.toCharArray(),k,2);
                if (strqn.trim().isEmpty())
                    rowdata.setQns(q, 0);
                else
                    rowdata.setQns(q, Integer.parseInt(strqn.trim()));
                k = k + 2;
            }
            k = 67;
            for (int q=qnlength/2;q<qnlength;q++){
                String strqn = String.copyValueOf(str.toCharArray(),k,2);
                if (strqn.trim().isEmpty())
                    rowdata.setQns(q, 0);
                else
                    rowdata.setQns(q, Integer.parseInt(strqn.trim()));
                k = k + 2;                
            }
            String strfr = String.copyValueOf(str.toCharArray(),0,13);
            rowdata.setFrequency(Double.parseDouble(strfr));
            rowdata.setSpecies(species);
            db.insertPDataRow(rowdata);
            updateProgress(i, progressSize);
        }
    } // CATCompile

    /**
     * Compile RAM36 predictions file
     * @param fileName - name of RAM36 predictions file
     * @param species - species associated with @param fileName
     * @param color - color associated with @param fileName
     */
    private void RAM36Compile(String fileName, String species, int color, double iscale){

        double T = misc.getTemp();
        double Qrs = misc.getQfunc();
        double alimit = misc.getIcutoff();
        for (int i=RAM36START;i<strlist.size()-1;i++){
            String str = strlist.get(i);
            String strdint = String.copyValueOf(str.toCharArray(),43,9);
            double dint = Double.parseDouble(strdint);

            double f = Double.parseDouble(str.substring(54,65).trim());
            double fcm = f/29979.2458;
            double elo = Double.parseDouble(str.substring(78,87).trim());
            double sm2 = Double.parseDouble(str.substring(90,97).trim());
            if (sm2==0) continue;

            double its = 4.16231e-05*f*sm2*(Math.exp(-1.438*elo/T)-Math.exp(-1.438*(elo+fcm)/T))/Qrs;

            if (its*iscale<alimit) continue;

            rowdata.setIntensity(its*iscale);
            rowdata.setColor(color);
            int qnlength = rowdata.getQnsLength();
            int k = 3;
            for (int q=0;q<qnlength/2;q++){
                String strqn = String.copyValueOf(str.toCharArray(),k,3);
                rowdata.setQns(q, Integer.parseInt(strqn.trim()));
                k+=4;
            }
            k = 24;
            for (int q=qnlength/2;q<qnlength;q++){
                String strqn = String.copyValueOf(str.toCharArray(),k,3);
                rowdata.setQns(q, Integer.parseInt(strqn.trim()));
                k+=4;                
            }
            rowdata.setFrequency(f);
            rowdata.setSpecies(species);
            db.insertPDataRow(rowdata);
            updateProgress(i, progressSize);
        }
    }

    /**
     * Compile RAM36HF predictions file
     * @param fileName - name of RAM36 predictions file
     * @param species - species associated with @param fileName
     * @param color - color associated with @param fileName
     */
    private void RAM36HFCompile(String fileName, String species, int color, double iscale){
        for (int i=RAM36START-1;i<strlist.size()-1;i++){
            String str = strlist.get(i);
            String strdint = String.copyValueOf(str.toCharArray(),55,9);
            double dint = Double.parseDouble(strdint);
            dint = dint*1.0E-06*iscale;
            rowdata.setIntensity(dint);
            rowdata.setColor(color);
            int qnlength = rowdata.getQnsLength();
            String strqn = String.copyValueOf(str.toCharArray(),3,3);
            rowdata.setQns(0, Integer.parseInt(strqn.trim()));
            strqn = String.copyValueOf(str.toCharArray(),7,5);
            rowdata.setQns(1, Double.parseDouble(strqn.trim()));            
            int k = 13;
            for (int q=2;q<qnlength/2;q++){
                strqn = String.copyValueOf(str.toCharArray(),k,3);
                rowdata.setQns(q, Integer.parseInt(strqn.trim()));
                k+=4;
            }
            strqn = String.copyValueOf(str.toCharArray(),30,3);
            rowdata.setQns(5, Integer.parseInt(strqn.trim()));
            strqn = String.copyValueOf(str.toCharArray(),34,5);
            rowdata.setQns(6, Double.parseDouble(strqn.trim()));                        
            k = 40;
            for (int q=qnlength/2+2;q<qnlength;q++){
                strqn = String.copyValueOf(str.toCharArray(),k,3);
                rowdata.setQns(q, Integer.parseInt(strqn.trim()));
                k+=4;                
            } 
            String strfr = String.copyValueOf(str.toCharArray(),65,12);
            rowdata.setFrequency(Double.parseDouble(strfr));
            rowdata.setSpecies(species);
            db.insertPDataRow(rowdata);
            updateProgress(i, progressSize);
        }
    }

    @Override
    protected Object call() throws Exception {
        compileData();
        return null;
    }
}
