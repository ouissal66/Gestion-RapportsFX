module com.audit.auditaifx {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.kordamp.bootstrapfx.core;
    requires net.synedra.validatorfx;
    requires java.sql;

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