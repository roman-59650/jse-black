package fr.ulille.spexp.data;

import java.util.ArrayList;

public class Transition {

    protected ArrayList qnums;
    protected DbFormat format;

    public Transition(DbFormat format){
        this.format = format;
        qnums = new ArrayList();
        for (int i=0;i<2*format.getLength();i++){
            qnums.add(null);
        }
    }

    /**
     * sets the quantum number value at a given index
     * @param idx quantum number index
     * @param value quantum number value
     */
    public void setQns(int idx, Object value){
        qnums.set(idx, value);
    }

    public Object getQns(int idx){
        return qnums.get(idx);
    }

    public boolean isEqual(Transition transition){
        boolean result = false;
        int size1 = this.format.getLength();
        int size2 = transition.format.getLength();
        if (size1!=size2) {
        }
        else {
            result = true;
            for (int i=0;i<size1;i++){
                boolean cond = this.qnums.get(i)==transition.qnums.get(i);
                result = result && cond;
            }
        }
        return result;
    }

}
