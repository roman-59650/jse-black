package fr.ulille.spexp.fftprofile;

import javafx.beans.value.ObservableValue;
import javafx.beans.value.WritableValue;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;
import javafx.util.StringConverter;

import java.util.Locale;

public class FrequencySpinnerCell<T> extends TableCell<T, Double> {

    private Spinner<Double> spinner;
    private boolean ignoreUpdate;

    public FrequencySpinnerCell(double step){

        SpinnerValueFactory<Double> factory = new SpinnerValueFactory<Double>() {
            @Override
            public void decrement(int i) {
                final double value = getValue();
                setValue(value-step*i);
            }

            @Override
            public void increment(int i) {
                final double value = getValue();
                setValue(value+step*i);
            }
        };

        factory.setConverter(new StringConverter<Double>() {
            @Override
            public String toString(Double value) {
                if (value == null) {
                    return "";
                }
                return String.format(Locale.US,"%12.3f",value);
            }

            @Override
            public Double fromString(String string) {
                // If the specified value is null or zero-length, return null
                if (string == null) {
                    return null;
                }
                string = string.trim();
                if (string.length() < 1) {
                    return null;
                }
                // Perform the requested parsing
                return Double.parseDouble(string);
            }
        });

        spinner = new Spinner<Double>(factory);
        spinner.valueProperty().addListener((o, oldValue, newValue) -> {
            if (!ignoreUpdate){
                ignoreUpdate = true;
                ObservableValue<Double> value = getTableColumn().getCellObservableValue(getIndex());
                if (value instanceof WritableValue){
                    ((WritableValue<Double>)value).setValue(newValue);
                }
                ignoreUpdate = false;
            }
        });
        spinner.setOnScroll(event -> {
            if (event.getDeltaY()<0) spinner.decrement();
            else spinner.increment();
        });
    }

    @Override
    protected void updateItem(Double value, boolean empty) {
        super.updateItem(value, empty);
        if (value != null && !empty) {
            ignoreUpdate = true;
            spinner.getValueFactory().setValue(getItem().doubleValue());
            setText(null);
            setGraphic(spinner);
            ignoreUpdate = false;
        } else {
            setGraphic(null);
            setText(null);
        }
    }

    public static <T> Callback<TableColumn<T, Double>, TableCell<T, Double>> forTableColumn(double step) {
        return (TableColumn<T, Double> tableColumn) -> new FrequencySpinnerCell<T>(step);
    }
}
