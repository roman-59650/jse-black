package fr.ulille.spexp.fx;

import fr.ulille.spexp.compiler.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.util.converter.DoubleStringConverter;
import org.controlsfx.control.StatusBar;
import fr.ulille.spexp.data.Database;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class Compiler implements Initializable {

    private CompilationData row;

    @FXML TableColumn<CompilationData, String> pathColumn;
    @FXML TableColumn<CompilationData, Color> colorColumn;
    @FXML TableColumn<CompilationData, String> aliasColumn;
    @FXML TableColumn<CompilationData, Double> intensityColumn;
    @FXML TableColumn<CompilationData, Boolean> statusColumn;
    @FXML TableView<CompilationData> table;
    @FXML StatusBar statusBar;
    @FXML TextField spectraPath;
    @FXML TextField molMass;
    @FXML TextField tempData;
    @FXML TextField qfuncData;
    @FXML TextField icutoffData;

    private ObservableList<CompilationData> list = FXCollections.observableArrayList();
    private Database db;
    private MiscData misc;
    private DbCompiler comp;

    public void setCompiler(Database db){
        this.db = db;
        list.clear();
        list = this.db.getCompDataList();
        table.setItems(list);
        misc = this.db.getMiscData();
        spectraPath.setText(misc.getPath());
        molMass.setText(String.valueOf(misc.getMass()));
        tempData.setText(String.valueOf(misc.getTemp()));
        qfuncData.setText(String.valueOf(misc.getQfunc()));
        icutoffData.setText(String.valueOf(misc.getIcutoff()));
    }

    public Compiler(){
        list.clear();
    }

    public void onButtonClick(ActionEvent e){
        CompilationData td = new CompilationData("", "file"+table.getItems().size(), Color.BLACK,1, true);
        table.getItems().add(td);
    }

    public void onButton2CLick(ActionEvent e){
        int id = table.getSelectionModel().getSelectedIndex();
        if (id > -1) table.getItems().remove(id);
    }

    public void saveCompSettings(ActionEvent e){
        ObservableList<CompilationData> compilationData = table.getItems();
        this.db.setCompDataTable(compilationData);
        misc.setPath(spectraPath.getText());
        misc.setMass(Double.parseDouble(molMass.getText()));
        misc.setTemp(Double.parseDouble(tempData.getText()));
        misc.setQfunc(Double.parseDouble(qfuncData.getText()));
        misc.setIcutoff(Double.parseDouble(icutoffData.getText()));
        this.db.setMiscData(misc);
    }

    public void getSpectraPath(ActionEvent e){
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDirectory = directoryChooser.showDialog(Main.mainfrm.compilerStage);
        spectraPath.setText(selectedDirectory.getPath());
    }

    public void compileDbClick(ActionEvent e){
        saveCompSettings(e);
        comp = new DbCompiler(table.getItems(),Main.mainfrm.getDatabase(),statusBar);
        Thread th = new Thread(comp);
        th.setDaemon(true);
        th.start();
    }

    public boolean isCompiled(){
        if (comp!=null)
            return comp.isCompiled();
        else
            return false;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colorColumn.setCellFactory(ColorTableCellEdit.forTableColumn());
        colorColumn.setCellValueFactory(cell->cell.getValue().getColorProperty());

        pathColumn.setCellFactory(TextFieldTableCellEdit.forTableColumn());
        pathColumn.setCellValueFactory(cell->cell.getValue().getPathProperty());

        aliasColumn.setCellFactory(AliasTableCellEdit.forTableColumn());
        aliasColumn.setCellValueFactory(cell->cell.getValue().getAliasProperty());
        aliasColumn.setStyle("-fx-alignment: CENTER-LEFT;");

        intensityColumn.setCellValueFactory(cell->cell.getValue().getIntensityProperty());
        intensityColumn.setStyle("-fx-alignment: CENTER-LEFT;");
        intensityColumn.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));

        statusColumn.setCellValueFactory(cell->cell.getValue().getStatusProperty());
        statusColumn.setStyle("-fx-alignment: CENTER;");
        statusColumn.setCellFactory(statusColumn-> new CheckBoxTableCell());

        table.setEditable(true);
    }
}
