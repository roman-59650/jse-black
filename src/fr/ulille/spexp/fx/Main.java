package fr.ulille.spexp.fx;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.*;
import java.util.Properties;

public class Main extends Application {

    private static final String versionID = "0.4";
    private static final String copyrightID = " © Roman Motiyenko, PhLAM - Univ. Lille ";

    public static MainController mainfrm;
    private static Stage pstage;
    private static Properties properties;

    @Override
    public void start(Stage primaryStage) throws Exception{
        FXMLLoader loader = new FXMLLoader(getClass().getResource("Main.fxml"));
        Parent root = loader.load();
        mainfrm = loader.getController();
        primaryStage.setTitle("Java Spectrum Explorer (JSE) v"+versionID+copyrightID);
        primaryStage.setScene(new Scene(root));
        primaryStage.onCloseRequestProperty().setValue(e -> {
            if (mainfrm.toolStage.isShowing()) mainfrm.toolStage.hide();
            mainfrm.getDatabase().deleteDatabase("_temp");
            File configFile = new File("config.xml");
            try {
                OutputStream outputStream = new FileOutputStream(configFile);
                properties.storeToXML(outputStream, "application settings");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            Platform.exit();
        });
        primaryStage.getIcons().add(new Image("fr/ulille/spexp/resources/icons/application_red.png"));
        primaryStage.show();
        pstage = primaryStage;
        mainfrm.toolStage.initOwner(primaryStage);
        mainfrm.fileListStage.initOwner(primaryStage);

        properties = new Properties();
        InputStream inputStream = new FileInputStream("config.xml");
        properties.loadFromXML(inputStream);
    }

    public static Properties getProperties() {
        return properties;
    }

    public static Stage getPrimaryStage(){
        return pstage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
