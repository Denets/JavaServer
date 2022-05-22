package com.denets.server;

import java.awt.*;
import java.awt.TrayIcon.MessageType;

public class NotificationManager {
    public boolean displayTray(String text) throws AWTException {
        if (SystemTray.isSupported())
        {
            SystemTray tray = SystemTray.getSystemTray();

            Image image = Toolkit.getDefaultToolkit().createImage("icon.png");

            TrayIcon trayIcon = new TrayIcon(image);
            trayIcon.setImageAutoSize(true);
            tray.add(trayIcon);

            trayIcon.displayMessage("PCRemote", text, MessageType.INFO);

            return true;
        }
        else
        {
            return false;
        }
    }
}
