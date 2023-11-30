(ns whalker.notification
  (:import
   [java.awt
    MenuItem
    PopupMenu
    SystemTray
    TrayIcon
    TrayIcon$MessageType]
   [java.awt Toolkit]))

(defn notify [title message]
  (when (SystemTray/isSupported)
    (let [tray (SystemTray/getSystemTray)
          icon (-> (Toolkit/getDefaultToolkit)
                   (.getImage "image.png"))
          popup-menu (PopupMenu.)
          exit-item (MenuItem. "Exit")]

      (.add popup-menu exit-item)

      (let [tray-icon (TrayIcon. icon title popup-menu)]
        (.add tray tray-icon)
        (.displayMessage tray-icon title message TrayIcon$MessageType/INFO)))))
