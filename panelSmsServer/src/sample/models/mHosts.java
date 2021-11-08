package sample.models;

public class mHosts {
    private  String name;
    private  String value;
    private  int localhost; //Флаг работы сервера на текущем компьютере под виртуальной машиной.

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public int getLocalhost() {
        return localhost;
    }

    public void setLocalhost(int localhost) {
        this.localhost = localhost;
    }
}
