package models;

public enum SumValues {
    MINDEPOSIT(0.01f),
    MAXDEPOSIT(5000.0f),
    MINTRANSFER(0.01f),
    MAXTRANSFER(10000.0f),
    LESSMIN(0.0f),
    OVERMAXTRANSFER(10000.01f),
    OVERMAXDEPOSIT(5000.01f),
    SOMEDEPOSIT(0.02f);

    private final float value;

    SumValues(float value){
       this.value=value;
    }

    public float getValue(){
        return value;
    }
}
