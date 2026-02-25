module me.justsid.mcservergui {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires javafx.swing;

    requires org.controlsfx.controls;
    requires java.logging;
    requires org.slf4j;
    requires java.desktop;

    opens me.justsid.mcservergui to javafx.fxml;
    exports me.justsid.mcservergui;
}