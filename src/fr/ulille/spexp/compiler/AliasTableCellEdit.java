package fr.ulille.spexp.compiler;

import fr.ulille.spexp.fx.Main;
import javafx.event.EventHandler;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.util.Callback;
import org.controlsfx.control.textfield.CustomTextField;

public class AliasTableCellEdit<T> extends TableCell<T, String> {

    private String alias;
    private TextField textField;
    private TableColumn<T, String> column;

    public AliasTableCellEdit(TableColumn<T, String> tc){
        alias = "";
        this.column = tc;
    }

    public static <T>Callback<TableColumn<T, String>, TableCell<T, String>> forTableColumn() {
        return (TableColumn<T, String> tableColumn) -> new AliasTableCellEdit<T>(tableColumn);
    }

    @Override
    public void startEdit(){
        if (!isEditable()
                || !getTableView().isEditable()
                || !getTableColumn().isEditable()) {
            return;
        }
        super.startEdit();
        alias = getItem();
        configureEditorOn();
    }

    @Override
    public void commitEdit(String newValue) {
        if (newValue.isEmpty()){
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Alias");
            alert.setHeaderText("Empty name!");
            alert.setContentText(null);
            alert.initOwner(Main.mainfrm.compilerStage);
            alert.showAndWait();
            super.commitEdit(alias);
        } else
            super.commitEdit(newValue);
        configureEditorOff();
    }

    @Override
    protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        setText(item);
    }

    @Override
    public void cancelEdit() {
        super.cancelEdit();
        configureEditorOff();
    }

    private void configureEditorOn() {
        if (textField == null) {
            textField = new TextField();
            textField.setMaxWidth(Double.MAX_VALUE);
            textField.setPrefWidth(column.getPrefWidth());
        }
        setGraphic(textField);
        textField.setText(getItem());
        textField.setOnKeyPressed(escHandler);
        textField.setVisible(true);
        textField.selectAll();
        textField.requestFocus();
    }

    private void configureEditorOff() {
        textField.setOnKeyPressed(null);
        textField.setVisible(false);
        setText(String.valueOf(getItem()));
        setGraphic(null);
    }

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

}
