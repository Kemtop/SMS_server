package deviceOperator;

/**
 * Строка таблицы типов устройств.
 */
public class mDevType {
    private String code; //Символический код типа устройства. Например MODEM,GSM,API.
    private int prior; //Приоритет устройства.
    private int sysType; //Числовой тип устройства, параметр ТОЛЬКО для кода СМС сервера. 1-Модем, 2-GSM шлюз 3 API

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public int getPrior() {
        return prior;
    }

    public void setPrior(int prior) {
        this.prior = prior;
    }

    public int getSysType() {
        return sysType;
    }

    public void setSysType(int sysType) {
        this.sysType = sysType;
    }
}
