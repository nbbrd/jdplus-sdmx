package jdplus.sdmx.desktop.plugin;

import java.util.Properties;

public enum Toggle {

    DEFAULT, DISABLE, ENABLE;

    public void applyTo(Properties properties, CharSequence key) {
        switch (this) {
            case DEFAULT -> properties.remove(key.toString());
            case DISABLE -> properties.setProperty(key.toString(), Boolean.toString(false));
            case ENABLE -> properties.setProperty(key.toString(), Boolean.toString(true));
        }
    }
}
