package fr.ulille.spexp.fx;

import fr.ulille.spexp.data.AssignRowData;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import org.controlsfx.control.StatusBar;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class DbOutput implements Initializable {

    @FXML ComboBox<String> aliasCombo;
    @FXML ComboBox<String> formatCombo;
    @FXML Button saveButton;
    @FXML StatusBar statusBar;
    @FXML ListView<AssignRowData> assignRowDataListView;
    private List<AssignRowData> dataList;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        aliasCombo.valueProperty().addListener((ov, oldValue, newValue)->{
            setOutput(aliasCombo.getSelectionModel().getSelectedItem(),
                    formatCombo.getSelectionModel().getSelectedIndex());
        });

        formatCombo.getItems().addAll("GENERIC", "ASFIT", "SPFIT", "RAM36");
        formatCombo.getSelectionModel().selectFirst();
        formatCombo.valueProperty().addListener((ov, oldValue, newValue)->{
            if (!aliasCombo.getSelectionModel().isEmpty()){
                setOutput(aliasCombo.getSelectionModel().getSelectedItem(),
                        formatCombo.getSelectionModel().getSelectedIndex());
            }
        });

        saveButton.setOnAction(event -> saveToFile());
        dataList = new ArrayList<>();
    }

    private void setOutput(String alias, int format){
        dataList.clear();
        dataList = Main.mainfrm.getDatabase().getAssignedLineList(alias,format);
        assignRowDataListView.setItems(FXCollections.observableList(dataList));
    }

    public void setAliases(List<String> aliases){
        aliasCombo.getItems().clear();
        aliasCombo.getItems().addAll(aliases);
    }

    public void saveToFile(){
        FileChooser fileChooser = new FileChooser();
        File file = fileChooser.showSaveDialog(Main.mainfrm.outputStage);
        if (file==null) return;
        try {
            Files.write(file.toPath(),dataList.stream().map(a->a.toString()).collect(Collectors.toList()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
