package fr.ulille.spexp.compiler;

import fr.ulille.spexp.fx.Main;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;
import javafx.util.Callback;
import org.controlsfx.control.textfield.CustomTextField;

import java.io.File;

public class TextFieldTableCellEdit<T> extends TableCell<T, String> {

    private final Region graphic = new Region();
    private TableColumn<T, String> column;
    private final FileChooser fileChooser;


    public TextFieldTableCellEdit(TableColumn<T, String> tc){
        this.graphic.setId("graphic");
        this.column = tc;
        this.fileChooser = new FileChooser();
    }

    @Override
    protected void updateItem(String value, boolean empty) {
        super.updateItem(value, empty);
        setText(null);
        if (value != null && !empty) {
            setText(value);
        }
    }

    /**
     * Fabrique statique.
     */
    public static <T> Callback<TableColumn<T, String>, TableCell<T, String>> forTableColumn() {
        return (TableColumn<T, String> tableColumn) -> new TextFieldTableCellEdit<>(tableColumn);
    }

    private CustomTextField textField;
    private Button button;

    /**
     * Cette méthode est appelée lorsque l'édition commence.
     */
    @Override
    public void startEdit() {
        if (!isEditable()
                || !getTableView().isEditable()
                || !getTableColumn().isEditable()) {
            return;
        }
        super.startEdit();
        configureEditorOn();
    }

    /**
     * Cette méthode est appelée lorsque l'édition est validée.
     */
    @Override
    public void commitEdit(String value) {
        super.commitEdit(value);
        configureEditorOff();
    }

    /**
     * Cette méthode est appelée lorsque l'édition est annulée.
     */
    @Override
    public void cancelEdit() {
        super.cancelEdit();
        configureEditorOff();
    }

    /**
     * Configuration de l'éditeur lorsque l'édition commence.
     */
    private void configureEditorOn() {
        if (textField == null) {
            textField = new CustomTextField();
            textField.setMaxWidth(Double.MAX_VALUE);
            button = new Button("...");
            button.setOnMouseClicked(this::openFile);
            textField.setRight(button);
        }
        setGraphic(textField);
        textField.setText(getItem());
        textField.setPrefWidth(column.getPrefWidth());
        textField.textProperty().addListener(editorValueChangeListener);
        textField.setOnKeyPressed(escHandler);
        textField.setVisible(true);
        textField.selectAll();
        textField.requestFocus();
    }

    /**
     * Configuration de l'éditeur lorsque l'édition se termine.
     */
    private void configureEditorOff() {
        textField.textProperty().removeListener(editorValueChangeListener);
        textField.setOnKeyPressed(null);
        textField.setVisible(false);
        setText(String.valueOf(getItem()));
        setGraphic(null);
    }

    /**
     * Permet de suivre les changements de valeur de l'éditeur.
     */
    private final ChangeListener<String> editorValueChangeListener =
            (ObservableValue<? extends String> observableValue, String oldValue, String newValue) -> {
                setText(newValue);
            };

    /**
     * Permet de réagir à la touche ESC.
     */
    private final EventHandler<KeyEvent> escHandler = (KeyEvent keyEvent) -> {
        switch (keyEvent.getCode()) {
            case ESCAPE:
                cancelEdit();
                break;
            case ENTER:
                commitEdit(textField.getText());
                break;
        }
    };

    private void openFile(MouseEvent me){
        File file = fileChooser.showOpenDialog(Main.mainfrm.compilerStage);
        if (file!=null){
            this.textField.setText(file.getPath());
            commitEdit(file.getPath());
        }
    }

}
