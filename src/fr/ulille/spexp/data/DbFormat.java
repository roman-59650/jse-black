
package fr.ulille.spexp.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 *
 * @author Roman Motiyenko
 */

public class DbFormat {

    public enum qnDataType {StrData, IntData, FloData}
    private List<qnDataType> qnTypes;
    private ArrayList qnNames;
    private String formatString;

    public DbFormat(String str){
        qnTypes = new ArrayList<>();
        qnNames = new ArrayList();
        formatString = str;
        doScan();
    }

    private void doScan(){
        Scanner s = new Scanner(formatString);
        s.useDelimiter(",");
        qnNames.clear();
        qnTypes.clear();
        while (s.hasNext()){
            String str = s.next();
            if (str.charAt(0)=='i') qnTypes.add(qnDataType.IntData);
            if (str.charAt(0)=='s') qnTypes.add(qnDataType.StrData);
            if (str.charAt(0)=='d') qnTypes.add(qnDataType.FloData);
            String qnn = str.substring(2, str.length());
            qnNames.add(qnn);
        }
        s.close();
    }

    public void setFormat(String str){
        formatString = str;
        doScan();
    }

    public qnDataType getDataType(int index){
        if (index>=qnTypes.size()) index -= qnTypes.size();
        return (qnDataType) qnTypes.get(index);
    }

    public String getQName(int index){
        return (String)qnNames.get(index);
    }

    public int getLength(){
        return qnTypes.size();
    }

}
