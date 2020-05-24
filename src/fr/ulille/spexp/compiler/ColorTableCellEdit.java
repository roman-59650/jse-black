package fr.ulille.spexp.compiler;

import javafx.event.EventHandler;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.util.Callback;

import java.util.ArrayList;
import java.util.List;

public class ColorTableCellEdit<T> extends TableCell<T, Color> {

    private final Region graphic = new Region();

    private List<Color> standardColors;

    public ColorTableCellEdit() {
        graphic.setId("graphic");
        standardColors = new ArrayList<>();
        standardColors.add(Color.rgb(255,0,0));
        standardColors.add(Color.rgb(255,128,0));
        standardColors.add(Color.rgb(255,255,0));
        standardColors.add(Color.rgb(128,255,0));
        standardColors.add(Color.rgb(0,255,0));
        standardColors.add(Color.rgb(0,255,128));
        standardColors.add(Color.rgb(0,255,255));
        standardColors.add(Color.rgb(0,128,255));
        standardColors.add(Color.rgb(0,0,255));
        standardColors.add(Color.rgb(128,0,255));
        standardColors.add(Color.rgb(255,0,255));
        standardColors.add(Color.rgb(255,0,128));
    }

    @Override
    protected void updateItem(Color value, boolean empty) {
        super.updateItem(value, empty);
        setText(null);
        String style = null;
        if (value != null && !empty) {
            final int red = (int) (value.getRed() * 255);
            final int green = (int) (value.getGreen() * 255);
            final int blue = (int) (value.getBlue() * 255);
            style = String.format("-fx-background-color: rgb(%d, %d, %d)", red, green, blue);
        }
        graphic.setStyle(style);
        setGraphic(graphic);
    }
    
    public static <T> Callback<TableColumn<T, Color>, TableCell<T, Color>> forTableColumn() {
        return (TableColumn<T, Color> tableColumn) -> new ColorTableCellEdit<>();
    }

    private ColorPicker colorPicker;

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

    @Override
    public void commitEdit(Color value) {
        super.commitEdit(value);
        configureEditorOff();
    }

    @Override
    public void cancelEdit() {
        super.cancelEdit();
        configureEditorOff();
    }

    private void configureEditorOn() {
        if (colorPicker == null) {
            colorPicker = new ColorPicker();
            colorPicker.setMaxWidth(Double.MAX_VALUE);
            colorPicker.getCustomColors().addAll(standardColors);
        }
        colorPicker.setValue(getItem());
        colorPicker.setOnHiding(e->{
            if (isEditing()) commitEdit(this.colorPicker.getValue());
        });
        colorPicker.setOnKeyPressed(escHandler);
        setGraphic(colorPicker);
        colorPicker.arm();
    }

    private void configureEditorOff() {
        colorPicker.setOnKeyPressed(null);
        setGraphic(graphic);
    }

    private final EventHandler<KeyEvent> escHandler = (KeyEvent keyEvent) -> {
        switch (keyEvent.getCode()) {
            case ESCAPE:
                cancelEdit();
                break;
        }
    };

}
