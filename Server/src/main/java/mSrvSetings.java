/**
 * Модель настроек сервера.
 */
public class mSrvSetings {
    private String name;
    private int value;
    private  String info; //Описание переменной согласно тз.

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

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }
}
