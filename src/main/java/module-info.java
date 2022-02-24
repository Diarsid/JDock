module diarsid.jdock {

    requires java.desktop;
    requires javafx.controls;
    requires javafx.swing;
    requires org.slf4j;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires diarsid.filesystem;
    requires diarsid.support;
    requires diarsid.support.javafx;

    opens diarsid.jdock.jfx to javafx.base;

    opens diarsid.jdock.json to com.fasterxml.jackson.databind;
}
