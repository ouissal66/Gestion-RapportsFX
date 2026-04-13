package com.audit.auditaifx.controller;

import com.audit.auditaifx.model.RapportAudit;
import com.audit.auditaifx.model.Recommandation;
import com.audit.auditaifx.model.StatutRapport;
import com.audit.auditaifx.service.RapportService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

public class ClientController {

    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cmbFilterStatut;
    @FXML private ListView<RapportAudit> listRapports;
    
    @FXML private Label lblTitre;
    @FXML private Label lblAuditeur;
    @FXML private Label lblDate;
    @FXML private Label lblStatut;
    @FXML private TextArea txtDescription;
    
    @FXML private VBox recommendationsContainer;
    @FXML private Label lblNoSelection;
    @FXML private VBox detailPane;

    private RapportService service = new RapportService();
    private ObservableList<RapportAudit> allReports;

    @FXML
    public void initialize() {
        allReports = service.getTous();
        
        // Setup Search and Filters
        FilteredList<RapportAudit> filteredData = new FilteredList<>(allReports, p -> true);
        
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            updateFilter(filteredData, newValue, cmbFilterStatut.getValue());
        });
        
        cmbFilterStatut.getItems().addAll("Tous", "BROUILLON", "EN_COURS", "FINALISE");
        cmbFilterStatut.setValue("Tous");
        cmbFilterStatut.valueProperty().addListener((observable, oldValue, newValue) -> {
            updateFilter(filteredData, txtSearch.getText(), newValue);
        });

        listRapports.setItems(filteredData);
        
        // Custom list cell for a better look
        listRapports.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(RapportAudit item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getTitre() + " (" + item.getDateCreation() + ")");
                }
            }
        });

        // Listen for selection
        listRapports.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            showDetails(newVal);
        });

        showDetails(null);
    }

    private void updateFilter(FilteredList<RapportAudit> list, String search, String statut) {
        list.setPredicate(report -> {
            boolean matchesSearch = search == null || search.isEmpty() ||
                    report.getTitre().toLowerCase().contains(search.toLowerCase()) ||
                    report.getAuditeur().toLowerCase().contains(search.toLowerCase());
            
            boolean matchesStatut = statut == null || statut.equals("Tous") ||
                    report.getStatut().name().equals(statut);
            
            return matchesSearch && matchesStatut;
        });
    }

    private void showDetails(RapportAudit report) {
        if (report == null) {
            lblNoSelection.setVisible(true);
            detailPane.setVisible(false);
            return;
        }

        lblNoSelection.setVisible(false);
        detailPane.setVisible(true);

        lblTitre.setText(report.getTitre());
        lblAuditeur.setText("Auditeur: " + report.getAuditeur());
        lblDate.setText("Date: " + report.getDateCreation());
        lblStatut.setText(report.getStatut().name());
        txtDescription.setText(report.getDescription());

        // Display Recommendations as Cards
        recommendationsContainer.getChildren().clear();
        for (Recommandation reco : report.getRecommandations()) {
            VBox card = new VBox(5);
            card.getStyleClass().add("reco-card");
            
            Label desc = new Label(reco.getDescription());
            desc.setWrapText(true);
            desc.setStyle("-fx-font-weight: bold;");
            
            Label meta = new Label("Priorité: " + reco.getPriorite() + " | " + (reco.isResolue() ? "✅ Résolue" : "⏳ En attente"));
            meta.setStyle("-fx-font-size: 11px;");
            
            card.getChildren().addAll(desc, meta);
            recommendationsContainer.getChildren().add(card);
        }
    }

    @FXML
    public void switchAdmin() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/audit/auditaifx/main-view.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage stage = (javafx.stage.Stage) listRapports.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) {
            System.err.println("Erreur switch admin: " + e.getMessage());
        }
    }
}
