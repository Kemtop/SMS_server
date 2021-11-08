package deviceOperator;

import java.util.ArrayList;
import java.util.List;

/**
 * Модель типов устройств.
 */
public class mDevTypes {
    private ArrayList<mDevType> devTypes;

    public  ArrayList<mDevType> getDevTypes() {
        return devTypes;
    }

    public void setDevTypes(ArrayList<mDevType> devTypes) {
        this.devTypes = devTypes;
    }

    /**
     * На основании системного типа возвращает код данного устройства.
     * @param sysType
     * @return
     */
    public String getDevCode(int sysType) {
        ArrayList<mDevType> lines = devTypes;
        mDevType t = lines.stream().filter(x -> x.getSysType() == sysType).findFirst().get();
        return t.getCode();
    }

    /**
     * На основании кода  устройства возвращает системный тип.
     * @param code
     * @return
     */
    public int getSysType(String code) {
        mDevType t = devTypes.stream().filter(x -> x.getCode().equals(code)).findFirst().get();
        return t.getSysType();
    }

    /**
     * Возвращает приоритет для устройства.
     * @param SysType
     * @return
     */
    public int getPriorVal(int SysType) {
        mDevType t = devTypes.stream().filter(x -> x.getSysType()==SysType).findFirst().get();
        return t.getPrior();
    }
}
