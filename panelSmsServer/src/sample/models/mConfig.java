package sample.models;

import java.util.ArrayList;

/**
 * Конфигурационный файл.
 */
public class mConfig {
    private ArrayList<mHosts> hosts=new ArrayList<mHosts>();
    private String defaultPhone;
    private String defaultText;
    private String localhostName; //Имя хоста для которого задается первой строка с параметром localhost.

    public void  add(mHosts m)
    {
        hosts.add(m);
    }

    public ArrayList<mHosts> getHosts() {
        return hosts;
    }

    public void setHosts(ArrayList<mHosts> hosts) {
        this.hosts = hosts;
    }

    public String getDefaultPhone() {
        return defaultPhone;
    }

    public void setDefaultPhone(String defaultPhone) {
        this.defaultPhone = defaultPhone;
    }

    public String getDefaultText() {
        return defaultText;
    }

    public void setDefaultText(String defaultText) {
        this.defaultText = defaultText;
    }

    public String getLocalhostName() {
        return localhostName;
    }

    public void setLocalhostName(String localhostName) {
        this.localhostName = localhostName;
    }
}
