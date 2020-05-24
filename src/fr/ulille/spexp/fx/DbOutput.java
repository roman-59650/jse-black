package fr.ulille.spexp.fx;

import javafx.beans.value.ObservableValue;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class DbOutput implements Initializable {

    @FXML ComboBox<String> aliasCombo;
    @FXML ComboBox<String> formatCombo;
    @FXML TextArea outText;
    @FXML Button saveButton;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        aliasCombo.valueProperty().addListener((ov, oldValue, newValue)->
            setOutput(aliasCombo.getSelectionModel().getSelectedItem(),
                    formatCombo.getSelectionModel().getSelectedIndex()));

        formatCombo.getItems().addAll("GENERIC", "ASFIT", "SPFIT", "RAM36");
        formatCombo.getSelectionModel().selectFirst();
        formatCombo.valueProperty().addListener((ov, oldValue, newValue)->
                setOutput(aliasCombo.getSelectionModel().getSelectedItem(),
                        formatCombo.getSelectionModel().getSelectedIndex()));

        saveButton.setOnAction(event -> saveToFile());
    }

    private void setOutput(String alias, int format){
        System.out.println(alias);
        final Service<List<String>> getListService = new Service<List<String>>() {
            @Override
            protected Task<List<String>> createTask() {
                return new Task<List<String>>() {
                    @Override
                    protected List<String> call() throws Exception {
                        outText.setPromptText("Wait...");
                        outText.clear();
                        //List<String> list = Main.mainfrm.getDatabase().getAssignedLineList(alias,format);
                        List<String> list = Main.mainfrm.getDatabase().getAsgnLineList(alias,format);
                        return list;
                    }
                };
            }
        };

        getListService.stateProperty().addListener((ObservableValue<? extends Worker.State> observableValue, Worker.State oldV, Worker.State newV) -> {
            switch (newV) {
                case FAILED:
                case CANCELLED:
                case SUCCEEDED:
                    outText.clear();
                    outText.setPromptText("No assigned transitions.");
                    for (String s : getListService.getValue()){
                        outText.appendText(s+"\n");
                    }
                    break;
            }
        });
        getListService.start();
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
            BufferedWriter bf = new BufferedWriter(new FileWriter(file));
            bf.write(outText.getText());
            bf.flush();
            bf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
