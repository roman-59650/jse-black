package fr.ulille.spexp.fx;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import fr.ulille.spexp.data.Database;
import fr.ulille.spexp.data.DbFormat;

import java.net.URL;
import java.util.ResourceBundle;

public class DbCreateDialog implements Initializable {

    private Database db;
    private DbFormat dbFormat;
    private String dbname;
    private String dbform;

    @FXML TextField dbnameField;
    @FXML TextField dbformatField;
    @FXML Button okbutton;
    @FXML Button ccbutton;
    @FXML HBox buttonbox;

    public void createDbClick(ActionEvent e){
        dbname = dbnameField.getText();
        if (dbname.isEmpty()) return;
        dbform = dbformatField.getText();
        dbFormat.setFormat(dbform);
        db.setDbName(dbname);
        db.setDbFormat(dbFormat);
        db.setConnection();
        db.closeConnection();

        Stage stage = (Stage) buttonbox.getScene().getWindow();
        stage.close();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        dbname = "";
        db = new Database(dbname);
        dbFormat = new DbFormat("");

        okbutton.setDefaultButton(true);
        ccbutton.setCancelButton(true);

        ccbutton.setOnAction(e->{
            Stage stage = (Stage) buttonbox.getScene().getWindow();
            stage.close();
        });
    }
}
