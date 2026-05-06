package util;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.net.URL;

public class AppResources {

    private static ImageIcon logoIcon;
    private static Image appIcon;
    private static Image smileWallpaper;

    // ========= LOGO =========
    public static ImageIcon getLogo() {

        if (logoIcon == null) {
            try {
                URL url = resolveResource("logo.png");

                if (url == null) {
                    System.out.println("Logo not found!");
                    return null;
                }

                ImageIcon icon = new ImageIcon(url);
                Image img = icon.getImage().getScaledInstance(60, 60, Image.SCALE_SMOOTH);
                logoIcon = new ImageIcon(img);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return logoIcon;
    }
    
    public static ImageIcon getIcon(String name, int width, int height) {
        try {
            URL url = resolveResource(name);

            if (url == null) {
                System.out.println("Icon not found: " + name);
                return null;
            }

            ImageIcon icon = new ImageIcon(url);
            Image img = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);

            return new ImageIcon(img);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    // ========= WINDOW ICON =========
    public static Image getAppIcon() {

        if (appIcon == null) {
            try {
                URL url = resolveResource("logo.png");
                if (url != null) {
                    appIcon = new ImageIcon(url).getImage();
                }
            } catch (Exception e) {
                System.out.println("App icon not found!");
            }
        }

        return appIcon;
    }

    // ========= DASHBOARD WALLPAPER =========
    public static Image getSmileWallpaper() {

        if (smileWallpaper == null) {
            try {
                URL url = resolveResource("Smile_Care.png");
                if (url != null) {
                    smileWallpaper = new ImageIcon(url).getImage();
                }
            } catch (Exception e) {
                System.out.println("Wallpaper not found!");
            }
        }

        return smileWallpaper;
    }

    public static URL resolveResource(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        String clean = name.startsWith("/") ? name.substring(1) : name;
        URL url = AppResources.class.getResource("/resources/" + clean);
        if (url == null) {
            url = AppResources.class.getResource("/" + clean);
        }
        if (url == null) {
            url = AppResources.class.getClassLoader().getResource("resources/" + clean);
        }
        if (url == null) {
            url = AppResources.class.getClassLoader().getResource(clean);
        }
        if (url == null) {
            // IDE fallback: read directly from project folders
            String[] paths = {
                    "src/resources/" + clean,
                    "resources/" + clean,
                    "src/" + clean,
                    clean
            };
            for (String p : paths) {
                File f = new File(p);
                if (f.exists()) {
                    try {
                        return f.toURI().toURL();
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        return url;
    }
}