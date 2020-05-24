package fr.ulille.spexp.fx;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import fr.ulille.spexp.data.Database;

import java.net.URL;
import java.util.ResourceBundle;

public class Filter implements Initializable {

    @FXML ComboBox<String> aliascombo;
    @FXML ListView<String> qnumslist;
    @FXML ListView<String> selrulist;
    @FXML ComboBox<String> compcombo;
    @FXML Spinner qnspinner;
    @FXML ComboBox<String> seriescombo;
    @FXML ListView<String> resultlist;
    @FXML TextField qnvalue;
    @FXML Button clearallbt;
    @FXML Button clearbt;
    @FXML TextField jminfield;
    @FXML TextField jmaxfield;
    @FXML TextField kvalfield;

    public enum FilterResult {Filter, LoomisWood};
    private FilterResult result;
    private String filter;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        compcombo.getItems().addAll("=",">=","<=",">","<","<>");
        compcombo.getSelectionModel().selectFirst();
        SpinnerValueFactory.IntegerSpinnerValueFactory spinnerValueFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(-10,10,1);
        spinnerValueFactory.setValue(0);
        qnspinner.setValueFactory(spinnerValueFactory);
        seriescombo.getItems().addAll("aR(0,1) Ka=","bR(-1,1) Ka=");
        seriescombo.getSelectionModel().selectFirst();

        clearallbt.setOnAction(e->{
            resultlist.getItems().clear();
        });

        clearbt.setOnAction(e->{
            if (resultlist.getItems().size()==0) return;
            int index = resultlist.getSelectionModel().getSelectedIndex();
            resultlist.getItems().remove(index);
            if (index>0) index--;
            if (resultlist.getItems().size()>0) resultlist.getSelectionModel().select(index);
        });

        filter="";
    }

    public void setLists(){
        Database database = Main.mainfrm.getDatabase();
        aliascombo.getItems().clear();
        selrulist.getItems().clear();
        qnumslist.getItems().clear();
        aliascombo.getItems().addAll(database.getAliasList());
        selrulist.getItems().addAll(database.getSelruList());
        qnumslist.getItems().addAll(database.getQnumsList());
        if (aliascombo.getItems().size()>0) aliascombo.getSelectionModel().selectFirst();
        if (selrulist.getItems().size()>0) selrulist.getSelectionModel().selectFirst();
        if (qnumslist.getItems().size()>0) qnumslist.getSelectionModel().selectFirst();
    }

    public void onAddAliasClick(ActionEvent e){
        resultlist.getItems().add(" SPECIES="+"'"+aliascombo.getSelectionModel().getSelectedItem()+"'");
        resultlist.getSelectionModel().selectLast();
        resultlist.scrollTo(resultlist.getSelectionModel().getSelectedIndex());
    }

    public void onORAliasClick(ActionEvent e){
        ObservableList<String> list = resultlist.getItems();
        int index = resultlist.getSelectionModel().getSelectedIndex();
        String s = list.get(index);
        s += " OR SPECIES="+"'"+aliascombo.getSelectionModel().getSelectedItem()+"'";
        list.set(resultlist.getSelectionModel().getSelectedIndex(),s);
        resultlist.setItems(list);
        resultlist.getSelectionModel().select(index);
    }

    public void onAddSelectionRuleClick(ActionEvent e){
        resultlist.getItems().add(" "+selrulist.getSelectionModel().getSelectedItem()+"="+(Integer)qnspinner.getValue());
        resultlist.getSelectionModel().selectLast();
        resultlist.scrollTo(resultlist.getSelectionModel().getSelectedIndex());
    }

    public void onORSelectionRuleClick(ActionEvent e){
        ObservableList<String> list = resultlist.getItems();
        int index = resultlist.getSelectionModel().getSelectedIndex();
        String s = list.get(index);
        s += " OR "+selrulist.getSelectionModel().getSelectedItem()+"="+(Integer)qnspinner.getValue();
        list.set(resultlist.getSelectionModel().getSelectedIndex(),s);
        resultlist.setItems(list);
        resultlist.getSelectionModel().select(index);
    }

    public void onAddSeriesClick(ActionEvent e){
        resultlist.getItems().add(" J2-J1=1");
        resultlist.getItems().add(" Ka2-Ka1=0");
        resultlist.getItems().add(" Kc2-Kc1=1");
        resultlist.getItems().add(" J2>="+jminfield.getText());
        resultlist.getItems().add(" J2<="+jmaxfield.getText());
        if (Integer.parseInt(kvalfield.getText())>0)
            resultlist.getItems().add(" Kc2=J2-Ka2+1");
        else
            resultlist.getItems().add(" Kc2=J2-Ka2");
        resultlist.getItems().add(" Ka2="+Math.abs(Integer.parseInt(kvalfield.getText())));
        resultlist.getSelectionModel().selectFirst();
    }

    public void onAddRangeClick(ActionEvent e){
        resultlist.getItems().add(" "+qnumslist.getSelectionModel().getSelectedItem()+
                compcombo.getSelectionModel().getSelectedItem()+qnvalue.getText());
        resultlist.getSelectionModel().selectLast();
        resultlist.scrollTo(resultlist.getSelectionModel().getSelectedIndex());
    }

    public void onORRangeClick(ActionEvent e){
        ObservableList<String> list = resultlist.getItems();
        int index = resultlist.getSelectionModel().getSelectedIndex();
        String s = list.get(index);
        s += " OR "+qnumslist.getSelectionModel().getSelectedItem()+
                compcombo.getSelectionModel().getSelectedItem()+qnvalue.getText();
        list.set(resultlist.getSelectionModel().getSelectedIndex(),s);
        resultlist.setItems(list);
        resultlist.getSelectionModel().select(index);
    }

    public void onFilterClick(){
        filter = "";
        for (String s:resultlist.getItems())
            filter = filter+" ("+s+") "+"AND";
        if (filter.length()>0){
            filter=filter.substring(0,filter.length()-4); //remove last "AND"
            filter = " WHERE"+filter;
        }
        result = FilterResult.Filter;
        Main.mainfrm.filterStage.close();
    }

    public void onLWClick(){
        filter = "";
        for (String s:resultlist.getItems())
            filter += s+" AND";
        if (filter.length()>0){
            filter=filter.substring(0,filter.length()-4); //remove last "AND"
            filter = " WHERE"+filter;
        }
        result = FilterResult.LoomisWood;
        Main.mainfrm.filterStage.close();
    }

    public void onCancelClick(){
        filter = "";
        Main.mainfrm.filterStage.close();
    }

    public FilterResult getResult(){
        return result;
    }

    public String getFilter(){
        return filter;
    }
}
