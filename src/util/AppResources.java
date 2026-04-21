package util;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

public class AppResources {

    private static ImageIcon logoIcon;
    private static Image appIcon;
    private static Image smileWallpaper;

    // ========= LOGO =========
    public static ImageIcon getLogo() {

        if (logoIcon == null) {
            try {
                URL url = AppResources.class.getResource("/logo.png");

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
    // ========= WINDOW ICON =========
    public static Image getAppIcon() {

        if (appIcon == null) {
            try {
                appIcon = new ImageIcon(
                		AppResources.class.getResource("/logo.png")).getImage();
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
                smileWallpaper = new ImageIcon(
                		AppResources.class.getResource("/Smile_Care.png")).getImage();
            } catch (Exception e) {
                System.out.println("Wallpaper not found!");
            }
        }

        return smileWallpaper;
    }
}