package fr.ulille.spexp.fx;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.input.KeyCode;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.ResourceBundle;

public class DBDialog implements Initializable {

    private String userHomeDir;
    private String systemDir;
    private String dbname;
    public Stage dbcreateStage;
    @FXML ListView<String> listview;
    @FXML Button selectdb;
    @FXML Button createdb;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        userHomeDir = System.getProperty("user.dir");
        systemDir = userHomeDir + "/db";
        dbname = "";
        dbcreateStage = new Stage();
        listview.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        listview.setOnKeyPressed(e->{
            if (e.getCode()== KeyCode.ENTER) {
                selectDb();
            }
        });
        updateList();

        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("DbCreateDialog.fxml"));
            Parent dbcreate = fxmlLoader.load();
            dbcreateStage.setScene(new Scene(dbcreate));
            dbcreateStage.initModality(Modality.WINDOW_MODAL);
            dbcreateStage.initStyle(StageStyle.UTILITY);
            dbcreateStage.setTitle("Create Database");
            dbcreateStage.setResizable(false);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private ArrayList<String> getDbList(){
        ArrayList<String> list = new ArrayList();
        File root = new File(systemDir+"/");
        String[] dlist = root.list();
        if (dlist.length!=0){
            for (int i=0;i<dlist.length;i++){
                File file = new java.io.File(systemDir+"/"+dlist[i]);
                if (file.isDirectory())
                    list.add(dlist[i]);
            }
        }
        return list;
    }

    public void updateList(){
        ArrayList<String> alist = getDbList();
        alist.sort(Comparator.comparing(String::toString));
        listview.getItems().clear();
        for (int i=0;i<alist.size();i++){
            listview.getItems().add(alist.get(i));
        }
        listview.getSelectionModel().selectFirst();
        alist.clear();
    }

    private void selectDb(){
        int id = listview.getSelectionModel().getSelectedIndex();
        if (id!=-1) {
            dbname = listview.getSelectionModel().getSelectedItem();
            Stage stage = (Stage) selectdb.getScene().getWindow();
            stage.close();
        }
    }

    public void onCreateDbClick(ActionEvent e){
        dbcreateStage.showAndWait();
        updateList();
    }

    public void onSelectDatabaseClick(ActionEvent e){
        selectDb();
    }

    public String getDbname(){
        return dbname;
    }
}
