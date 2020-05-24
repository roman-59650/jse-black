package fr.ulille.spexp.fx;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import fr.ulille.spexp.data.Database;
import fr.ulille.spexp.spectrum.FileInfo;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class FileList implements Initializable {

    @FXML TreeView<FileInfo> treeView;
    @FXML AnchorPane searchBox;
    @FXML TextField searchField;
    @FXML Button searchButton;
    @FXML Button highlightButton;
    private Rectangle clipRect;
    private TreeItem<FileInfo> root;
    private int selectedIndex;
    private List<FileInfo> list;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        root = new TreeItem<>(new FileInfo("Files",0,0));
        root.setExpanded(true);

        treeView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue==null) return;
            if (!newValue.isLeaf()) return;
            TreeItem<FileInfo> selectedItem = newValue;
            selectedIndex = root.getChildren().indexOf(selectedItem);
            openSelectedFile(selectedItem);
        });

        double widthInitial = 200;
        double heightInitial = 40;
        clipRect = new Rectangle();
        clipRect.setWidth(widthInitial);
        clipRect.setHeight(0);
        clipRect.translateYProperty().set(heightInitial);
        searchBox.setClip(clipRect);
        searchBox.translateYProperty().set(-heightInitial);
        searchBox.prefHeightProperty().set(0);

        searchField.setOnMouseClicked(e->{
            searchField.selectAll();
        });
        searchField.setOnKeyPressed(e->{
            if (e.getCode()== KeyCode.ENTER)
                onSearchClick(new ActionEvent());
            if (e.isControlDown()&&e.getCode()==KeyCode.ENTER)
                onHighlightClick(new ActionEvent());
        });
        searchButton.setOnAction(this::onSearchClick);
        highlightButton.setOnAction(this::onHighlightClick);
    }

    private void openSelectedFile(TreeItem<FileInfo> fileInfoTreeItem){
        if (!fileInfoTreeItem.isLeaf()) return;
        String path = Main.mainfrm.getDatabase().getMiscData().getPath()+"/"+fileInfoTreeItem.getValue().toString();
        File file = new File(path);
        if (Main.mainfrm.tabpane.getTabs().size()==0)
            Main.mainfrm.openNewTabFile(file);
        else
            Main.mainfrm.openFile(file);
    }

    public void setFilesTree(Database db){
        root.getChildren().clear();
        list = db.getFileInfo();
        for (FileInfo i:list) {
            TreeItem<FileInfo> item = new TreeItem<>(i);
            root.getChildren().add(item);
        }
        treeView.setRoot(root);
        selectedIndex = -1;
    }

    public void expandSearch(ActionEvent e){
        clipRect.setWidth(searchBox.getWidth());

        if (clipRect.heightProperty().get() != 0) {

            // Animation for scroll up.
            Timeline timelineUp = new Timeline();

            // Animation of sliding the search pane up, implemented via
            // clipping.
            final KeyValue kvUp1 = new KeyValue(clipRect.heightProperty(), 0);
            final KeyValue kvUp2 = new KeyValue(clipRect.translateYProperty(), searchBox.getPrefHeight());

            // The actual movement of the search pane. This makes the table
            // grow.
            final KeyValue kvUp4 = new KeyValue(searchBox.prefHeightProperty(), 0);
            final KeyValue kvUp3 = new KeyValue(searchBox.translateYProperty(), -searchBox.getPrefHeight());

            final KeyFrame kfUp = new KeyFrame(Duration.millis(200), kvUp1, kvUp2, kvUp3, kvUp4);
            timelineUp.getKeyFrames().add(kfUp);
            timelineUp.play();
        } else {

            // Animation for scroll down.
            Timeline timelineDown = new Timeline();
            searchBox.setPrefHeight(33);
            // Animation for sliding the search pane down. No change in size,
            // just making the visible part of the pane
            // bigger.
            final KeyValue kvDwn1 = new KeyValue(clipRect.heightProperty(), searchBox.getPrefHeight());
            final KeyValue kvDwn2 = new KeyValue(clipRect.translateYProperty(), 0);

            // Growth of the pane.
            final KeyValue kvDwn4 = new KeyValue(searchBox.prefHeightProperty(), searchBox.getPrefHeight());
            final KeyValue kvDwn3 = new KeyValue(searchBox.translateYProperty(), 0);

            final KeyFrame kfDwn = new KeyFrame(Duration.millis(200), kvDwn1, kvDwn2, kvDwn3, kvDwn4);
            timelineDown.getKeyFrames().add(kfDwn);

            timelineDown.play();
        }

    }

    public void openFirst(ActionEvent e){
        TreeItem<FileInfo> item = root.getChildren().get(0);
        treeView.getSelectionModel().select(item);
        treeView.scrollTo(selectedIndex);
    }

    public void openPrev(ActionEvent e){
        if (selectedIndex>0) selectedIndex--;
        TreeItem<FileInfo> item = root.getChildren().get(selectedIndex);
        treeView.getSelectionModel().select(item);
        treeView.scrollTo(selectedIndex);
    }

    public void openNext(ActionEvent e){
        if (selectedIndex<root.getChildren().size()-1) selectedIndex++;
        TreeItem<FileInfo> item = root.getChildren().get(selectedIndex);
        treeView.getSelectionModel().select(item);
        treeView.scrollTo(selectedIndex);
    }

    public void openLast(ActionEvent e){
        TreeItem<FileInfo> item = root.getChildren().get(root.getChildren().size()-1);
        treeView.getSelectionModel().select(item);
        treeView.scrollTo(selectedIndex);
    }

    public void searchFrequency(double freq){
        for (FileInfo i:list){
            if (freq>=i.getMinfrequency()&&freq<i.getMaxfrequency()){
                selectedIndex = list.indexOf(i);
                TreeItem<FileInfo> item = root.getChildren().get(selectedIndex);
                treeView.getSelectionModel().select(item);
                treeView.scrollTo(selectedIndex);
            }
        }
    }

    public void onSearchClick(ActionEvent e){
        searchFrequency(Double.parseDouble(searchField.getText()));
    }

    public void onHighlightClick(ActionEvent e){
        Main.mainfrm.highlightFrequency(Double.parseDouble(searchField.getText()));
    }
}
