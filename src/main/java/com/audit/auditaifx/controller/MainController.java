package com.audit.auditaifx.controller;

import com.audit.auditaifx.model.RapportAudit;
import com.audit.auditaifx.model.Recommandation;
import com.audit.auditaifx.service.RapportService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.geometry.Pos;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class MainController {

    // --- Rapports ---
    @FXML private TableView<RapportAudit> rapportsTable;
    @FXML private TableColumn<RapportAudit, String> colTitre;
    @FXML private TableColumn<RapportAudit, String> colAuditeur;
    @FXML private TableColumn<RapportAudit, String> colEntite;
    @FXML private TableColumn<RapportAudit, String> colStatut;
    @FXML private TableColumn<RapportAudit, String> colDate;
    @FXML private TableColumn<RapportAudit, Integer> colNbReco;
    @FXML private TableColumn<RapportAudit, Void> colActionsRapport;

    // --- Recommandations ---
    @FXML private TableView<Recommandation> recoTable;
    @FXML private TableColumn<Recommandation, String> colRecoDesc;
    @FXML private TableColumn<Recommandation, String> colRecoPriorite;
    @FXML private TableColumn<Recommandation, Boolean> colRecoResolue;
    @FXML private TableColumn<Recommandation, Void> colActionsReco;

    // --- Labels + Boutons ---
    @FXML private Label lblRecommandations;
    @FXML private Label lblTotalRapports;
    @FXML private Label lblTotalReco;
    @FXML private Button btnAjouterReco;

    // --- Service ---
    private RapportService service = new RapportService();
    private RapportAudit rapportSelectionne = null;

    @FXML
    public void initialize() {
        // Colonnes rapports
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colAuditeur.setCellValueFactory(new PropertyValueFactory<>("auditeur"));
        colEntite.setCellValueFactory(new PropertyValueFactory<>("entiteAuditee"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateCreation"));

        // Colonne nombre de recommandations
        colNbReco.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleIntegerProperty(
                        cell.getValue().getRecommandations().size()).asObject());

        // Colonnes recommandations
        colRecoDesc.setCellValueFactory(new PropertyValueFactory<>("description"));
        colRecoPriorite.setCellValueFactory(new PropertyValueFactory<>("priorite"));
        colRecoResolue.setCellValueFactory(new PropertyValueFactory<>("resolue"));

        // Boutons actions rapports
        ajouterBoutonsRapport();

        // Boutons actions recommandations
        ajouterBoutonsReco();

        // Charger données
        service.chargerDonneesTest();
        rapportsTable.setItems(service.getTous());
        mettreAJourCompteurs();

        // Écouter la sélection d'un rapport
        rapportsTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, ancien, nouveau) -> {
                    rapportSelectionne = nouveau;
                    if (nouveau != null) {
                        lblRecommandations.setText(
                                "📌 Recommandations — " + nouveau.getTitre());
                        recoTable.setItems(FXCollections.observableArrayList(
                                nouveau.getRecommandations()));
                        btnAjouterReco.setDisable(false);
                        mettreAJourCompteurs();
                    }
                });
    }

    // ─── CRUD Rapports ────────────────────────────────────────

    @FXML
    public void ajouterRapport() {
        ouvrirFormulaireRapport(null);
    }

    private void modifierRapport(RapportAudit rapport) {
        ouvrirFormulaireRapport(rapport);
    }

    private void supprimerRapport(RapportAudit rapport) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer le rapport ?");
        confirm.setContentText("« " + rapport.getTitre() + " » sera supprimé définitivement.");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                service.supprimer(rapport);
                rapportsTable.setItems(service.getTous());
                recoTable.setItems(FXCollections.observableArrayList());
                lblRecommandations.setText("📌 Recommandations — sélectionnez un rapport");
                btnAjouterReco.setDisable(true);
                mettreAJourCompteurs();
            }
        });
    }

    private void ouvrirFormulaireRapport(RapportAudit rapport) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/audit/auditaifx/rapport-form-view.fxml"));
            Parent root = loader.load();

            RapportFormController controller = loader.getController();
            controller.setService(service);
            controller.setRapport(rapport);
            controller.setOnSave(() -> {
                rapportsTable.setItems(service.getTous());
                mettreAJourCompteurs();
            });

            Stage stage = new Stage();
            stage.setTitle(rapport == null ? "Ajouter un rapport" : "Modifier le rapport");
            stage.setScene(new Scene(root, 500, 450));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

        } catch (Exception e) {
            afficherErreur("Erreur ouverture formulaire : " + e.getMessage());
        }
    }

    // ─── CRUD Recommandations ─────────────────────────────────

    @FXML
    public void ajouterRecommandation() {
        if (rapportSelectionne != null)
            ouvrirFormulaireReco(null);
    }

    private void modifierRecommandation(Recommandation reco) {
        ouvrirFormulaireReco(reco);
    }

    private void supprimerRecommandation(Recommandation reco) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer la recommandation ?");
        confirm.setContentText(reco.getDescription());
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                service.supprimerRecommandation(reco.getId());
                rapportSelectionne.getRecommandations().remove(reco);
                recoTable.setItems(FXCollections.observableArrayList(
                        rapportSelectionne.getRecommandations()));
                rapportsTable.refresh();
                mettreAJourCompteurs();
            }
        });
    }

    private void ouvrirFormulaireReco(Recommandation reco) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/audit/auditaifx/reco-form-view.fxml"));
            Parent root = loader.load();

            RecoFormController controller = loader.getController();
            controller.setService(service);
            controller.setRapport(rapportSelectionne);
            controller.setRecommandation(reco);
            controller.setOnSave(() -> {
                rapportSelectionne.setRecommandations(service.getRecommandations(rapportSelectionne.getId()));
                recoTable.setItems(FXCollections.observableArrayList(
                        rapportSelectionne.getRecommandations()));
                rapportsTable.refresh();
                mettreAJourCompteurs();
            });

            Stage stage = new Stage();
            stage.setTitle(reco == null ? "Ajouter une recommandation" : "Modifier");
            stage.setScene(new Scene(root, 450, 320));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

        } catch (Exception e) {
            afficherErreur("Erreur ouverture formulaire : " + e.getMessage());
        }
    }

    // ─── Boutons dans les tableaux ────────────────────────────

    private void ajouterBoutonsRapport() {
        colActionsRapport.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit = new Button("✏️");
            private final Button btnDel = new Button("🗑️");
            private final javafx.scene.layout.HBox box =
                    new javafx.scene.layout.HBox(5, btnEdit, btnDel);

            {
                btnEdit.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-background-radius: 4;");
                btnDel.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-background-radius: 4;");
                btnEdit.setOnAction(e -> modifierRapport(getTableView().getItems().get(getIndex())));
                btnDel.setOnAction(e -> supprimerRapport(getTableView().getItems().get(getIndex())));
                box.setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void ajouterBoutonsReco() {
        colActionsReco.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit = new Button("✏️");
            private final Button btnDel = new Button("🗑️");
            private final javafx.scene.layout.HBox box =
                    new javafx.scene.layout.HBox(5, btnEdit, btnDel);

            {
                btnEdit.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-background-radius: 4;");
                btnDel.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-background-radius: 4;");
                btnEdit.setOnAction(e -> modifierRecommandation(getTableView().getItems().get(getIndex())));
                btnDel.setOnAction(e -> supprimerRecommandation(getTableView().getItems().get(getIndex())));
                box.setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    // ─── Utilitaires ──────────────────────────────────────────

    private void mettreAJourCompteurs() {
        lblTotalRapports.setText(service.getTous().size() + " rapport(s)");
        int nbReco = rapportSelectionne != null ?
                rapportSelectionne.getRecommandations().size() : 0;
        lblTotalReco.setText(nbReco + " recommandation(s)");
    }

    private void afficherErreur(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setContentText(msg);
        alert.show();
    }

    @FXML
    public void switchClient() {
        try {
            var resource = getClass().getResource("/com/audit/auditaifx/client-view.fxml");
            if (resource == null) {
                afficherErreur("Fichier client-view.fxml introuvable dans les ressources.");
                return;
            }
            FXMLLoader loader = new FXMLLoader(resource);
            Parent root = loader.load();
            Stage stage = (Stage) rapportsTable.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace(); // Print to console for debugging
            afficherErreur("Impossible de basculer vers la vue client : " + e.getMessage());
        }
    }
}