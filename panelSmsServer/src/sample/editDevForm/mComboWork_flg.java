package sample.editDevForm;

public class mComboWork_flg {
    private  String name;
    private int value;


    @Override
    public String toString() {
        return name;
    }

    public mComboWork_flg(String name, int value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }
}
