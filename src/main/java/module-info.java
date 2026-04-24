module com.audit.auditaifx {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.kordamp.bootstrapfx.core;
    requires net.synedra.validatorfx;
    requires java.sql;
    requires java.desktop;
    requires org.apache.poi.poi;
    requires org.apache.poi.ooxml;
    requires org.apache.commons.collections4;
    requires org.apache.commons.compress;
    requires org.apache.xmlbeans;
    requires org.apache.logging.log4j;
    requires com.github.librepdf.openpdf;

    // Ouvrir les packages à JavaFX pour la réflexion
    opens com.audit.auditaifx to javafx.fxml;
    opens com.audit.auditaifx.controller to javafx.fxml;
    opens com.audit.auditaifx.model to javafx.base, javafx.fxml;
    opens com.audit.auditaifx.service to javafx.fxml;

    // Exporter les packages
    exports com.audit.auditaifx;
    exports com.audit.auditaifx.controller;
    exports com.audit.auditaifx.model;
    exports com.audit.auditaifx.service;
}