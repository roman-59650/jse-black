package fr.ulille.spexp.fftprofile;

public enum Function {
    Voigt("Voigt"),
    Doppler("Doppler"),
    Lorentz("Lorentz");

    private String name = "";

    Function(String name){
        this.name = name;
    }

    @Override
    public String toString(){
        return name;
    }
}
