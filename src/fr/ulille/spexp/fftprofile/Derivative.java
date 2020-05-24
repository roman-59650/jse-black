package fr.ulille.spexp.fftprofile;

public enum Derivative {

    Zero(0),
    First(1),
    Second(2);

    private int value;

    Derivative(int value){
        this.value = value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    public int getValue() {
        return value;
    }
}
