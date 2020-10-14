package fr.ulille.spexp.fx;

import fr.ulille.spexp.fftprofile.CommonParameters;
import fr.ulille.spexp.fftprofile.LineParameters;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.*;
import org.controlsfx.control.StatusBar;
import fr.ulille.spexp.data.Database;
import fr.ulille.spexp.spectrum.FileInfo;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    private final FileChooser fileChooser = new FileChooser();

    @FXML VBox vbox;
    @FXML MenuBar menubar;
    @FXML public StatusBar statusbar;
    @FXML TabPane tabpane;
    Label labelXY;
    Label labelDB;
    @FXML RadioMenuItem filterOnItem;
    @FXML RadioMenuItem filterOffItem;

    public Stage toolStage;
    public Stage dbselStage;
    public Stage compilerStage;
    public Stage fftviewStage;
    public Stage fileListStage;
    public Stage filterStage;
    public Stage lwplotStage;
    public Stage settingsStage;
    public Stage outputStage;
    public Stage fitspStage;
    private Database db;
    static DBDialog dbdialog;
    static Compiler compiler;
    static FileList fileList;
    static Filter filter;
    static LoomisWoodPlot lwplot;
    static AppSettings appSettings;
    static DbOutput dbOutput;
    static FitSpectrum fitSpectrum;
    static FFTView fftView;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        toolStage = new Stage();
        dbselStage = new Stage();
        compilerStage = new Stage();
        fftviewStage = new Stage();
        fileListStage = new Stage();
        filterStage = new Stage();
        lwplotStage = new Stage();
        settingsStage = new Stage();
        outputStage = new Stage();
        fitspStage = new Stage();

        labelXY = new Label();
        labelXY.setFont(Font.font("Monaco",10));

        labelDB = new Label();
        labelDB.setFont(Font.font("Monaco", 10));

        labelDB.setStyle(" -fx-text-fill: red; ");

        Region region = new Region();
        region.setPrefWidth(10);
        region.setPrefHeight(statusbar.getPrefHeight());

        statusbar.getLeftItems().addAll(labelXY,region,labelDB);

        menubar.useSystemMenuBarProperty().set(true);

        String tempDirPath = System.getProperty("user.dir")+"/db/_temp";
        File tempDir = new File(tempDirPath);
        if (tempDir.exists()){
            try {
                Files.walk(tempDir.toPath())
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        db = new Database("_temp");
        db.setConnection();
        labelDB.setText("< DB: "+db.getDbName()+" >");

        tabpane.addEventHandler(KeyEvent.KEY_PRESSED,e->{
            if (tabpane.getTabs().size()<1) return;
            WorkPane workPane = (WorkPane) tabpane.getSelectionModel().getSelectedItem().getContent().lookup("#workpane");
            if (e.isShortcutDown()&&e.getCode()==KeyCode.RIGHT)
                workPane.selectNext();
            if (e.isShortcutDown()&&e.getCode()==KeyCode.LEFT)
                workPane.selectPrev();
        });

        tabpane.getSelectionModel().selectedItemProperty().addListener(
                //called when the a tab becomes active !!!!
                (observable, oldtab, newtab) -> {
                    if (newtab==null) return;
                    if (oldtab==null) return;
                    WorkPane workPane = (WorkPane) newtab.getContent().lookup("#workpane");
                    // database tuning for a given spectrum
                    if (workPane!=null) {
                        workPane.setDatabase(db);
                    }
                }
        );

        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("SpectrumTool.fxml"));
            Parent tools = fxmlLoader.load();
            toolStage.setScene(new Scene(tools));
            toolStage.initModality(Modality.NONE);
            toolStage.initStyle(StageStyle.UTILITY);
            toolStage.setTitle("Spectrum");
            toolStage.resizableProperty().setValue(Boolean.FALSE);
            toolStage.setResizable(false);
            toolStage.toBack();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("FFTView.fxml"));
            Parent tools = fxmlLoader.load();
            fftView = fxmlLoader.getController();
            fftviewStage.setScene(new Scene(tools));
            fftviewStage.initModality(Modality.NONE);
            fftviewStage.initStyle(StageStyle.UTILITY);
            fftviewStage.setTitle("FFT");
            fftviewStage.setResizable(false);
            fftviewStage.setOnCloseRequest(e->{
                Main.getProperties().setProperty("hpf cutoff", String.valueOf(fftView.getHPFcutoff()));
            });
            fftviewStage.toBack();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("DBDialog.fxml"));
            Parent dbsel = fxmlLoader.load();
            dbdialog = fxmlLoader.getController();
            dbselStage.setScene(new Scene(dbsel));
            dbselStage.initModality(Modality.APPLICATION_MODAL);
            dbselStage.initStyle(StageStyle.UTILITY);
            dbselStage.setTitle("Database");
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("Compiler.fxml"));
            Parent comp = fxmlLoader.load();
            compiler = fxmlLoader.getController();
            compilerStage.setScene(new Scene(comp));
            compilerStage.initModality(Modality.NONE);
            compilerStage.initStyle(StageStyle.DECORATED);
            compilerStage.setTitle("Compiler");
            compilerStage.setResizable(false);
            compilerStage.setOnCloseRequest(windowEvent -> {
                if (compiler.isCompiled()) updateWindow();
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("Filter.fxml"));
            Parent comp = fxmlLoader.load();
            filter = fxmlLoader.getController();
            filterStage.setScene(new Scene(comp));
            filterStage.initModality(Modality.NONE);
            filterStage.initStyle(StageStyle.DECORATED);
            filterStage.setTitle("Filter");
            filterStage.setResizable(false);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("FileList.fxml"));
            Parent comp = fxmlLoader.load();
            fileList = fxmlLoader.getController();
            fileListStage.setScene(new Scene(comp));
            fileListStage.initModality(Modality.NONE);
            fileListStage.initStyle(StageStyle.UTILITY);
            fileListStage.setTitle("Files");
            fileListStage.setResizable(false);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("LoomisWood.fxml"));
            Parent comp = fxmlLoader.load();
            lwplot = fxmlLoader.getController();
            lwplotStage.setScene(new Scene(comp));
            lwplotStage.initModality(Modality.NONE);
            lwplotStage.initStyle(StageStyle.DECORATED);
            lwplotStage.setTitle("Loomis-Wood");
            Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();
            lwplotStage.setHeight(primaryScreenBounds.getHeight());
            lwplotStage.setResizable(false);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("ExtSettings.fxml"));
            Parent comp = fxmlLoader.load();
            appSettings = fxmlLoader.getController();
            settingsStage.setScene(new Scene(comp));
            settingsStage.initModality(Modality.NONE);
            settingsStage.initStyle(StageStyle.UTILITY);
            settingsStage.setTitle("Settings");
            settingsStage.setResizable(false);

            settingsStage.setOnShowing(windowEvent -> {
                configureSettingsStage();
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("DbOutput.fxml"));
            Parent comp = fxmlLoader.load();
            dbOutput = fxmlLoader.getController();
            outputStage.setScene(new Scene(comp));
            outputStage.initModality(Modality.NONE);
            outputStage.initStyle(StageStyle.UTILITY);
            outputStage.setTitle("Database Output");
            outputStage.setResizable(false);
            outputStage.setAlwaysOnTop(true);

            outputStage.setOnShowing(windowEvent -> {
                dbOutput.setAliases(db.getAliasList());
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("FitSpectrumPane.fxml"));
            Parent comp = fxmlLoader.load();
            fitSpectrum = fxmlLoader.getController();
            fitspStage.setScene(new Scene(comp));
            fitspStage.initModality(Modality.NONE);
            fitspStage.initStyle(StageStyle.DECORATED);
            fitspStage.setTitle("Line Profile Fit");
            fitspStage.getScene().getStylesheets().add("fr/ulille/spexp/fx/tablespinner.css");
            fitspStage.setOnCloseRequest(e->{
                fitSpectrum.onResetClick(new ActionEvent());
                WorkPane workPane = (WorkPane) tabpane.getSelectionModel().getSelectedItem().getContent().lookup("#workpane");
                workPane.setDatabase(db);
                workPane.plotAll();  // reset spectrum and predictions !!!
            });
            //**********
            // This part of code is put here because we need to create fitSpectrumPane before creating any other
            // WorkPane. Otherwise it provokes a strange error with assignedPeakDlg that still has to be understood
            fitSpectrum.fitSpectrumPane = new FitSpectrumPane(fitSpectrum.box1.getWidth(),fitSpectrum.box1.getHeight());
            fitSpectrum.box1.getChildren().add(fitSpectrum.fitSpectrumPane);
            VBox.setVgrow(fitSpectrum.fitSpectrumPane, Priority.ALWAYS);
            //**********
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void configureSettingsStage(){
        appSettings.spectrumColor.setValue(Color.valueOf(Main.getProperties().getProperty("spectrum color")));
        appSettings.spBackgroundColor.setValue(Color.valueOf(Main.getProperties().getProperty("background color")));
        appSettings.predictionColor.setValue(Color.valueOf(Main.getProperties().getProperty("predictions color")));
        appSettings.prBackgroundColor.setValue(Color.valueOf(Main.getProperties().getProperty("predictions background")));
        appSettings.pwSpinner.getValueFactory().setValue(Double.parseDouble(Main.getProperties().getProperty("pen width")));
        appSettings.lwSpinner.getValueFactory().setValue(Double.parseDouble(Main.getProperties().getProperty("lw multiplier")));
        appSettings.psSpinner.getValueFactory().setValue(Double.parseDouble(Main.getProperties().getProperty("peak size")));
        appSettings.duText.setText(Main.getProperties().getProperty("default uncertainty"));
        appSettings.ulwText.setText(Main.getProperties().getProperty("unres linewidth"));
        appSettings.tkSpinner.getValueFactory().setValue(Integer.parseInt(Main.getProperties().getProperty("x-axis ticks")));
        appSettings.showXScaleBox.setSelected(Boolean.parseBoolean(Main.getProperties().getProperty("show x-axis")));
        appSettings.derivativeSelect.getSelectionModel().select(Integer.parseInt(Main.getProperties().getProperty("profile derivative")));
        appSettings.showProfileBox.setSelected(Boolean.parseBoolean(Main.getProperties().getProperty("plot profile")));
        appSettings.settingsApplied = false;
    }

    public Database getDatabase(){
        return db;
    }

    public void setDatabase(String dbname){
        db.setDbName(dbname);
        db.setConnection();
    }

    public void openNewTabFile(File file){
        Tab tab = new Tab();
        WorkPane workPane;
        workPane = new WorkPane(tabpane.getWidth(), tabpane.getHeight()-tabpane.getTabMaxHeight(), labelXY, statusbar);
        tab.setText(file.getName());
        tab.setOnClosed(t -> {
            if ((tabpane.getTabs().size()==0)&&(toolStage.isShowing())) toolStage.close();
            WorkPane w = (WorkPane) tab.getContent().lookup("#workpane");
            w.getSpectrum().clearPeaks();
        });
        tabpane.getTabs().add(tab);
        tab.setContent(workPane);
        Platform.runLater(()->workPane.readFile(file));
        tabpane.getSelectionModel().select(tab);
    }

    public void openFile(File file){
        WorkPane workPane = (WorkPane) tabpane.getSelectionModel().getSelectedItem().getContent().lookup("#workpane");
        Platform.runLater(()->workPane.readFile(file));
        tabpane.getSelectionModel().getSelectedItem().setText(file.getName());
    }

    public void onOpenClick(ActionEvent e){
        File file = fileChooser.showOpenDialog(Main.getPrimaryStage());
        if (file!=null) {
            openNewTabFile(file);
            if (fileListStage.isShowing()){
                fileList.treeView.getSelectionModel().select(-1);
            }
        }
    }

    public void onOpenFileClick(ActionEvent e){
        if (tabpane.getTabs().size()==0) {
            onOpenClick(e);
            if (fileListStage.isShowing()){
                fileList.treeView.getSelectionModel().select(-1);
            }
        }
        else {
            File file = fileChooser.showOpenDialog(Main.getPrimaryStage());
            if (file!=null){
                openFile(file);
                if (fileListStage.isShowing()){
                    fileList.treeView.getSelectionModel().select(-1);
                }
            }
        }
    }

    public void onSpectrumToolsClick(ActionEvent e){
        if (toolStage.isShowing()) return;
        if (tabpane.getTabs().size()==0) return;
        else {
            Stage stage = (Stage) vbox.getScene().getWindow();
            Bounds bounds = vbox.getBoundsInLocal();
            Bounds screen = vbox.localToScreen(bounds);
            System.out.println(screen.getMinX());
            double xpos = screen.getMinX()-170;
            double ypos = screen.getMinY()+25;
            if (xpos<0) xpos=0;
            toolStage.setX(xpos);
            toolStage.setY(ypos);
            toolStage.toBack();
            toolStage.setAlwaysOnTop(false);
            toolStage.show();
            stage.toFront();
        }
    }

    public void onOpenCompilerClick(ActionEvent e){
        if (compilerStage.isShowing()) return;
        compiler.setCompiler(db);
        compilerStage.show();
    }

    public void onOpenFilterClick(ActionEvent e){
        if (filterStage.isShowing()) return;
        filter.setLists();
        filterStage.showAndWait();
        if (filter.getResult()== Filter.FilterResult.Filter){
            System.out.println(filter.getFilter());
            db.setCompiledPreds(filter.getFilter());
            WorkPane workPane = (WorkPane) tabpane.getSelectionModel().getSelectedItem().getContent().lookup("#workpane");
            filterOnItem.setSelected(true);
            if (workPane!=null) {
                workPane.setDatabase(db);
                workPane.plotAll();
            }
        }
        if (filter.getResult() == Filter.FilterResult.LoomisWood){
            List<FileInfo> info = Main.mainfrm.getDatabase().getFileInfo();
            double minfreq = info.get(0).getMinfrequency();
            double maxfreq = info.get(info.size()-1).getMaxfrequency();
            String extfilter = filter.getFilter()+" AND FREQ>"+minfreq+" AND FREQ<"+maxfreq;

            db.getTransList(extfilter);
            //db.setCompiledPreds("");

            ResultSet rs = db.tranrs;
            List<Double> freq = new ArrayList<>();
            try {
                if (rs.first()) freq.add(rs.getDouble("FREQ"));
                while (rs.next()) freq.add(rs.getDouble("FREQ"));
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            lwplotStage.show();
            lwplotStage.toFront();
            //Main.getPrimaryStage().toBack();
            Platform.runLater(()->{
                lwplot.setPlots(freq);
            });
            lwplotStage.setOnCloseRequest(c->{
                lwplotStage.hide();
                onFilterOffClick();
            });
        }
    }

    public void updateWindow(){
        if (Main.mainfrm.tabpane.getSelectionModel().isEmpty()) return;
        WorkPane workPane = (WorkPane) tabpane.getSelectionModel().getSelectedItem().getContent().lookup("#workpane");
        if (workPane!=null) {
            workPane.setDatabase(db);
            workPane.plotAll();
        }
    }

    public void onFilterOnClick(){
        db.setCompiledPreds(filter.getFilter());
        WorkPane workPane = (WorkPane) tabpane.getSelectionModel().getSelectedItem().getContent().lookup("#workpane");
        filterOnItem.setSelected(true);
        if (workPane!=null) {
            workPane.setDatabase(db);
            workPane.plotAll();
        }
    }

    public void onFilterOffClick(){
        db.setCompiledPreds("");
        WorkPane workPane = (WorkPane) tabpane.getSelectionModel().getSelectedItem().getContent().lookup("#workpane");
        filterOffItem.setSelected(true);
        if (workPane!=null) {
            workPane.setDatabase(db);
            workPane.plotAll();
        }
    }

    public void highlightFrequency(double freq){
        WorkPane workPane = (WorkPane) tabpane.getSelectionModel().getSelectedItem().getContent().lookup("#workpane");
        workPane.highlightFrequency(freq);
    }

    public void onShowOutput(){
        outputStage.show();
    }

    public void onCreateNewDbClick(ActionEvent e){
        dbdialog.dbcreateStage.showAndWait();
        dbdialog.updateList();
    }

    public void onFileListClick(ActionEvent e){
        fileList.setFilesTree(db);
        fileListStage.show();
    }

    public void onSelectDbClick(ActionEvent e){
        dbselStage.showAndWait();
        // get the selected database and set the connection to the db
        if (!dbdialog.getDbname().isEmpty()){
            db.closeConnection();
            db.setDbName(dbdialog.getDbname());
            db.setConnection();
            if (db.isConnected()) {
                labelDB.setText("[ DB: " + db.getDbName() + " ]");
                if (!db.getDbName().startsWith("_")) labelDB.setStyle(" -fx-text-fill: green; ");
                else labelDB.setStyle(" -fx-text-fill: red;");
                Tab tab = tabpane.getSelectionModel().getSelectedItem();
                if (tab != null) {
                    WorkPane workPane = (WorkPane) tab.getContent().lookup("#workpane");
                    if (workPane != null) {
                        workPane.setDatabase(db);
                        workPane.plotAll();
                    }
                }
            }
        }
    }

    public void onSettingsClick(ActionEvent e){
        settingsStage.showAndWait();
    }

    public void onProfileFitClick(ActionEvent e){
        if (Main.mainfrm.tabpane.getSelectionModel().isEmpty()) return;
        WorkPane workPane = (WorkPane) tabpane.getSelectionModel().getSelectedItem().getContent().lookup("#workpane");
        if (workPane!=null){
            fitspStage.show();
            fitSpectrum.fitSpectrumPane.setSpectrum(workPane.getSpectrum());
            fitSpectrum.fitSpectrumPane.setDatabase(db);
            fitSpectrum.fitSpectrumPane.showSpectrum();
            fitSpectrum.fitSpectrumPane.showPeaks();
            fitSpectrum.initModel();
            /*if (fitSpectrum.fitSpectrumPane!=null){
                fitSpectrum.fitSpectrumPane.setSpectrum(workPane.getSpectrum());
                fitSpectrum.fitSpectrumPane.setDatabase(db);
                fitSpectrum.fitSpectrumPane.showSpectrum();
                fitSpectrum.fitSpectrumPane.showPeaks();
            } else {
                fitSpectrum.fitSpectrumPane.setSpectrum(workPane.getSpectrum());
                fitSpectrum.fitSpectrumPane.setDatabase(db);
                fitSpectrum.fitSpectrumPane.showSpectrum();
                fitSpectrum.fitSpectrumPane.showPeaks();
                fitSpectrum.initModel();
            }*/
        }
    }

    public void test(ActionEvent e){
        db.deleteDatabase("_temp");
    }

    public FitSpectrumPane getFitSpectrumPane(){
        return fitSpectrum.fitSpectrumPane;
    }

    public TableView<LineParameters> getParamsTable(){
        return fitSpectrum.paramsTable;
    }

    public TableView<CommonParameters> getCommonsTable(){
        return fitSpectrum.commonsTable;
    }

    public ToggleButton getBaslineSelectButton(){
        return fitSpectrum.selectBaselineButton;
    }

}
