package org.jezve.app;

import org.jezve.util.Platform;
import org.jezve.util.Misc;
import org.jezve.util.ResourceBundleUtf8;

import javax.swing.*;
import java.util.*;

public final class Strings {

    private static final ResourceBundle app;
    private static final ResourceBundle en;
    private static final Set appKeys = new HashSet();
    private static final Set keys = new HashSet();
    private static ResourceBundle i18n;
    private static Locale loc;
    private static final String suffix = Platform.isWindows() ? ".win" : Platform.isMac() ? ".mac" : ".nix";

    static {
        en = loadBundle(Locale.ENGLISH, "i18n", null);
        app = loadBundle(Locale.ENGLISH, "app", appKeys);
    }

    public static String getString(String k) {
        return getString(k, getI18N());
    }

    public static String getEnglishString(String k) {
        return getString(k, en);
    }

    public static String getApplicationString(String k) {
        return getString(k, app);
    }

    public static Set getLocaleKeys() {
        return Collections.unmodifiableSet(keys);
    }

    public static Set getApllicationKeys() {
        return Collections.unmodifiableSet(appKeys);
    }

    public static boolean hasString(String key) {
        String k = key + suffix;
        return keys.contains(k) || keys.contains(key) ||
                appKeys.contains(k) || appKeys.contains(key);
    }

    private static String getString(String k, ResourceBundle rb) {
        String p = k + suffix;
        if (keys.contains(p)) {
            return rb.getString(p);
        } else if (keys.contains(k)) {
            return rb.getString(k);
        } else if (appKeys.contains(p)) {
            return app.getString(p);
        } else {
            assert appKeys.contains(k) : k;
            return app.getString(k);
        }
    }

    private static ResourceBundle loadBundle(Locale loc, String name, Set keys) {
        String base = Icons.class.getPackage().getName() + ".resources." + name;
        ResourceBundle rb = ResourceBundleUtf8.getBundle(base, loc);
        if (keys != null) {
            for (Enumeration i = rb.getKeys(); i.hasMoreElements(); ) {
                String key = (String)i.nextElement();
                keys.add(key);
            }
        }
        return rb;
    }

    private static ResourceBundle getI18N() {
        if (!Misc.equals(loc, Locale.getDefault())) {
            loc = Locale.getDefault();
            String def = loc.getLanguage() +
                    (loc.getCountry().length() > 0 ? "_" + loc.getCountry() : "");
            String ul = UserSettings.get("locale", def);
            String language = ul.substring(0, 2);
            String country = ul.length() <= 3 ? "" : ul.substring(3);
            loc = new Locale(language, country);
            Locale.setDefault(loc);
            JComponent.setDefaultLocale(loc);
            try {
                UIManager.setLookAndFeel(UIManager.getLookAndFeel().getClass().getName());
            } catch (Throwable e) {
                throw new Error(e);
            }
            i18n = loadBundle(loc, "i18n", keys);
        }
        return i18n;
    }

    private Strings() { /* no instantiation */ }

}
