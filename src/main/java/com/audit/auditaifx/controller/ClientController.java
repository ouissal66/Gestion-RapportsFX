package com.audit.auditaifx.controller;

import com.audit.auditaifx.model.RapportAudit;
import com.audit.auditaifx.model.Recommandation;
import com.audit.auditaifx.model.StatutRapport;
import com.audit.auditaifx.service.RapportService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class ClientController {

    @FXML private ListView<RapportAudit> listRapports;
    
    @FXML private Label lblTitre;
    @FXML private Label lblAuditeur;
    @FXML private Label lblDate;
    @FXML private Label lblStatut;
    @FXML private TextArea txtDescription;
    
    @FXML private VBox recommendationsContainer;
    @FXML private VBox lblNoSelection;
    @FXML private ScrollPane detailPane;

    private RapportService service = new RapportService();
    private ObservableList<RapportAudit> allReports;

    @FXML
    public void initialize() {
        rafraichirListe();
        
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

    private void rafraichirListe() {
        allReports = service.getTous();
        listRapports.setItems(allReports);
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
    public void ajouterRapport() {
        ouvrirFormulaireRapport(null);
    }

    @FXML
    public void modifierRapport() {
        RapportAudit selected = listRapports.getSelectionModel().getSelectedItem();
        if (selected != null) {
            ouvrirFormulaireRapport(selected);
        }
    }

    @FXML
    public void supprimerRapport() {
        RapportAudit selected = listRapports.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirmation");
            confirm.setHeaderText("Supprimer le rapport ?");
            confirm.setContentText("« " + selected.getTitre() + " » sera supprimé définitivement.");
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    service.supprimer(selected);
                    rafraichirListe();
                    showDetails(null);
                }
            });
        }
    }

    private void ouvrirFormulaireRapport(RapportAudit rapport) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/audit/auditaifx/rapport-form-view.fxml"));
            Parent root = loader.load();

            RapportFormController controller = loader.getController();
            controller.setService(service);
            controller.setRapport(rapport);
            controller.setOnSave(() -> {
                rafraichirListe();
                if (rapport != null) {
                    showDetails(rapport);
                }
            });

            Stage stage = new Stage();
            stage.setTitle(rapport == null ? "Ajouter un rapport" : "Modifier le rapport");
            stage.setScene(new Scene(root, 500, 450));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

        } catch (Exception e) {
            System.err.println("Erreur ouverture formulaire: " + e.getMessage());
        }
    }

    @FXML
    public void switchAdmin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/audit/auditaifx/main-view.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) listRapports.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) {
            System.err.println("Erreur switch admin: " + e.getMessage());
        }
    }
}
