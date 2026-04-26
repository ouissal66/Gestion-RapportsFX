package com.audit.auditaifx.controller;

import com.audit.auditaifx.model.RapportAudit;
import com.audit.auditaifx.model.Recommandation;
import com.audit.auditaifx.model.Risque;
import com.audit.auditaifx.model.StatutRapport;
import com.audit.auditaifx.service.RapportService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.layout.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import java.util.List;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.FileOutputStream;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;

import java.util.List;

public class ClientController {

    @FXML
    private ScrollPane lblNoSelection;
    @FXML
    private VBox allReportsView;
    @FXML
    private PieChart chartRecoStatus;
    @FXML
    private LineChart<String, Number> chartTendanceClient;
    @FXML
    private VBox vboxRecentReportsClient;
    @FXML
    private VBox vboxRecentRecosClient;
    @FXML
    private TableView<RapportAudit> rapportsTable;
    @FXML
    private TableColumn<RapportAudit, String> colTitre;
    @FXML
    private TableColumn<RapportAudit, String> colAuditeur;
    @FXML
    private TableColumn<RapportAudit, String> colDate;
    @FXML
    private TableColumn<RapportAudit, String> colStatut;
    @FXML
    private TableColumn<RapportAudit, Void> colActionsRapport;

    @FXML
    private Label lblTitre;
    @FXML
    private Label lblAuditeur;
    @FXML
    private Label lblDate;
    @FXML
    private Label lblStatut;
    @FXML
    private TextArea txtDescription;

    @FXML
    private VBox recommendationsContainer;
    @FXML
    private VBox risquesContainer;
    @FXML
    private ScrollPane detailPane;
    @FXML
    private Label lblStatTotalRapports;
    @FXML
    private Label lblStatTotalReco;
    @FXML
    private Label lblStatRecoResolues;

    private RapportService service = new RapportService();
    private ObservableList<RapportAudit> allReports;

    @FXML
    public void initialize() {
        // Config table rapports
        colTitre.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("titre"));
        colAuditeur.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("auditeur"));
        colDate.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("dateCreation"));
        colStatut.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("statut"));

        // Boutons actions
        ajouterBoutonsRapportsTable();

        // Listen for selection in Table (Don't switch view)
        rapportsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            // No automatic switch
        });

        rafraichirListe();
        showDashboard();
    }

    private void ajouterBoutonsRapportsTable() {
        colActionsRapport.setCellFactory(param -> new TableCell<>() {
            private final Button btnVoir = new Button("Voir");
            private final Button btnEdit = new Button("Edit");
            private final Button btnDel = new Button("Supp");
            private final HBox container = new HBox(5, btnVoir, btnEdit, btnDel);

            {
                btnVoir.setStyle(
                        "-fx-background-color: #3498db; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 4; -fx-font-size: 11px; -fx-font-weight: bold;");
                btnEdit.setStyle(
                        "-fx-background-color: #f1c40f; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 4; -fx-font-size: 11px; -fx-font-weight: bold;");
                btnDel.setStyle(
                        "-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 4; -fx-font-size: 11px; -fx-font-weight: bold;");

                btnVoir.setFocusTraversable(false);
                btnEdit.setFocusTraversable(false);
                btnDel.setFocusTraversable(false);

                btnVoir.setOnAction(e -> {
                    RapportAudit r = getTableView().getItems().get(getIndex());
                    showReportDetails(r);
                });
                btnEdit.setOnAction(e -> {
                    RapportAudit r = getTableView().getItems().get(getIndex());
                    ouvrirFormulaireRapport(r);
                });
                btnDel.setOnAction(e -> {
                    RapportAudit r = getTableView().getItems().get(getIndex());
                    supprimerRapportManual(r);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : container);
            }
        });
    }

    private void supprimerRapportManual(RapportAudit rapport) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer le rapport ?");
        confirm.setContentText("« " + rapport.getTitre() + " » sera supprimé définitivement.");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                service.supprimer(rapport);
                rafraichirListe();
            }
        });
    }

    @FXML
    public void showDashboard() {
        lblNoSelection.setVisible(true);
        detailPane.setVisible(false);
        allReportsView.setVisible(false);
        mettreAJourStats();

        ObservableList<RapportAudit> rapports = service.getTous();
        int totalReco = 0;
        int resolues = 0;

        for (RapportAudit r : rapports) {
            totalReco += r.getRecommandations().size();
            for (com.audit.auditaifx.model.Recommandation reco : r.getRecommandations()) {
                if (reco.isResolue())
                    resolues++;
            }
        }

        // 1. Stats Textuelles
        lblStatTotalRapports.setText(String.valueOf(rapports.size()));
        lblStatTotalReco.setText(String.valueOf(totalReco));
        lblStatRecoResolues.setText(String.valueOf(resolues));

        // 2. PieChart
        chartRecoStatus.getData().clear();
        if (totalReco > 0) {
            chartRecoStatus.getData().add(new PieChart.Data("Résolues", resolues));
            chartRecoStatus.getData().add(new PieChart.Data("En attente", totalReco - resolues));
        }

        // 2.5 LineChart Tendance des Créations Client
        if (chartTendanceClient != null) {
            chartTendanceClient.getData().clear();
            XYChart.Series<String, Number> seriesTendance = new XYChart.Series<>();
            
            java.util.Map<String, Long> countParDate = rapports.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    r -> r.getDateCreation().toString(), 
                    java.util.stream.Collectors.counting()
                ));
                
            countParDate.entrySet().stream()
                .sorted(java.util.Map.Entry.comparingByKey())
                .forEach(e -> seriesTendance.getData().add(new XYChart.Data<>(e.getKey(), e.getValue())));
                
            chartTendanceClient.getData().add(seriesTendance);
        }

        // 3. Activités Récentes (Rapports)
        vboxRecentReportsClient.getChildren().clear();
        List<RapportAudit> sortedRapports = rapports.stream()
                .sorted((r1, r2) -> r2.getDateCreation().compareTo(r1.getDateCreation()))
                .limit(2)
                .toList();

        for (RapportAudit r : sortedRapports) {
            VBox card = new VBox(5);
            card.setStyle(
                    "-fx-padding: 8; -fx-background-color: #f8f9fa; -fx-background-radius: 8; -fx-border-color: #e9ecef; -fx-border-radius: 8;");
            Label t = new Label(r.getTitre());
            t.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
            Label d = new Label(r.getDateCreation() + " | " + r.getStatut());
            d.setStyle("-fx-font-size: 10px; -fx-text-fill: #6c757d;");
            card.getChildren().addAll(t, d);
            vboxRecentReportsClient.getChildren().add(card);
        }

        // 4. Recos Récentes
        vboxRecentRecosClient.getChildren().clear();
        List<com.audit.auditaifx.model.Recommandation> recentRecos = rapports.stream()
                .flatMap(r -> r.getRecommandations().stream())
                .limit(2) // Simplicité pour l'exemple
                .toList();

        for (com.audit.auditaifx.model.Recommandation reco : recentRecos) {
            HBox recoItem = new HBox(10);
            recoItem.setAlignment(Pos.CENTER_LEFT);
            Label dot = new Label("•");
            dot.setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold;");
            Label desc = new Label(reco.getDescription());
            desc.setStyle("-fx-font-size: 11px;");
            desc.setWrapText(true);
            recoItem.getChildren().addAll(dot, desc);
            vboxRecentRecosClient.getChildren().add(recoItem);
        }
    }

    @FXML
    public void showAllReports() {
        lblNoSelection.setVisible(false);
        detailPane.setVisible(false);
        allReportsView.setVisible(true);
        rapportsTable.getSelectionModel().clearSelection();
    }

    private void rafraichirListe() {
        allReports = service.getTous();
        rapportsTable.setItems(allReports);
        mettreAJourStats();
    }

    private void showReportDetails(RapportAudit report) {
        lblNoSelection.setVisible(false);
        detailPane.setVisible(true);
        allReportsView.setVisible(false);

        lblTitre.setText(report.getTitre());
        lblAuditeur.setText("Auditeur: " + report.getAuditeur());
        lblDate.setText("Date: " + report.getDateCreation());
        lblStatut.setText(report.getStatut().name());
        txtDescription.setText(report.getDescription());

        // Recommendations
        recommendationsContainer.getChildren().clear();
        for (Recommandation reco : report.getRecommandations()) {
            VBox card = new VBox(8);
            card.getStyleClass().add("reco-card");

            Label desc = new Label(reco.getDescription());
            desc.setWrapText(true);
            desc.setStyle("-fx-font-weight: bold;");

            Label meta = new Label(
                    "Priorité: " + reco.getPriorite() + " | " + (reco.isResolue() ? "✅ Résolue" : "⏳ En attente"));
            meta.setStyle("-fx-font-size: 11px;");
            card.getChildren().addAll(desc, meta);
            recommendationsContainer.getChildren().add(card);
        }

        // Risks
        risquesContainer.getChildren().clear();
        for (Risque risque : report.getRisques()) {
            VBox card = new VBox(8);
            card.getStyleClass().add("reco-card");
            card.setStyle("-fx-border-color: #e67e22; -fx-background-color: #fff9f5;");

            Label desc = new Label(risque.getDescription());
            desc.setWrapText(true);
            desc.setStyle("-fx-font-weight: bold; -fx-text-fill: #d35400;");

            Label impact = new Label("Impact: " + risque.getImpact());
            impact.setWrapText(true);
            impact.setStyle("-fx-font-size: 11px;");

            Label niveau = new Label("Niveau: " + risque.getNiveau());
            niveau.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: white; -fx-background-color: " +
                    (risque.getNiveau().contains("Critique") ? "#c0392b" : "#e67e22") +
                    "; -fx-padding: 2 5; -fx-background-radius: 3;");

            card.getChildren().addAll(desc, impact, niveau);
            risquesContainer.getChildren().add(card);
        }
    }

    private void mettreAJourStats() {
        if (allReports == null)
            return;

        int totalRapports = allReports.size();
        int totalReco = 0;
        int recoResolues = 0;

        for (RapportAudit r : allReports) {
            totalReco += r.getRecommandations().size();
            for (com.audit.auditaifx.model.Recommandation reco : r.getRecommandations()) {
                if (reco.isResolue())
                    recoResolues++;
            }
        }

        lblStatTotalRapports.setText(String.valueOf(totalRapports));
        lblStatTotalReco.setText(String.valueOf(totalReco));
        lblStatRecoResolues.setText(String.valueOf(recoResolues));
    }

    @FXML
    public void ajouterRapport() {
        ouvrirFormulaireRapport(null);
    }

    @FXML
    public void modifierRapport() {
        RapportAudit selected = rapportsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            ouvrirFormulaireRapport(selected);
        }
    }

    @FXML
    public void supprimerRapport() {
        RapportAudit selected = rapportsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirmation");
            confirm.setHeaderText("Supprimer le rapport ?");
            confirm.setContentText("« " + selected.getTitre() + " » sera supprimé définitivement.");
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    service.supprimer(selected);
                    rafraichirListe();
                    showAllReports();
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
                if (rapportSelectionne() != null) {
                    showReportDetails(rapportSelectionne());
                } else {
                    showDashboard();
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

    private RapportAudit rapportSelectionne() {
        return rapportsTable.getSelectionModel().getSelectedItem();
    }

    @FXML
    public void exporterPDF() {
        RapportAudit selected = rapportsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter le Rapport en PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        String safeTitre = selected.getTitre().replaceAll("[^a-zA-Z0-9]", "_");
        fileChooser.setInitialFileName("Rapport_Audit_" + safeTitre + ".pdf");

        File file = fileChooser.showSaveDialog(rapportsTable.getScene().getWindow());
        if (file == null)
            return;

        try {
            com.lowagie.text.Document document = new com.lowagie.text.Document(com.lowagie.text.PageSize.A4);
            PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();

            // Polices
            com.lowagie.text.Font titleFont = com.lowagie.text.FontFactory.getFont(
                    com.lowagie.text.FontFactory.HELVETICA_BOLD, 18, com.lowagie.text.Font.BOLD, Color.DARK_GRAY);
            com.lowagie.text.Font headerFont = com.lowagie.text.FontFactory
                    .getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 12, com.lowagie.text.Font.BOLD, Color.WHITE);
            com.lowagie.text.Font normalFont = com.lowagie.text.FontFactory
                    .getFont(com.lowagie.text.FontFactory.HELVETICA, 11);
            com.lowagie.text.Font boldFont = com.lowagie.text.FontFactory
                    .getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 11);

            // Titre
            Paragraph pTitle = new Paragraph("RAPPORT D'AUDIT", titleFont);
            pTitle.setAlignment(Element.ALIGN_CENTER);
            document.add(pTitle);
            document.add(new Paragraph(" "));

            // Infos du rapport
            document.add(new Paragraph("Titre: " + selected.getTitre(), boldFont));
            document.add(new Paragraph("Auditeur: " + selected.getAuditeur(), normalFont));
            document.add(new Paragraph("Entité: " + selected.getEntiteAuditee(), normalFont));
            document.add(new Paragraph("Statut: " + selected.getStatut(), normalFont));
            document.add(new Paragraph("Date: " + selected.getDateCreation(), normalFont));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Description:", boldFont));
            document.add(new Paragraph(selected.getDescription(), normalFont));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("────────────────────────────────────────────────────────────────────────"));
            document.add(new Paragraph(" "));

            // Recommandations
            document.add(new Paragraph("RECOMMANDATIONS",
                    com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 14)));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            table.setSpacingAfter(10f);

            String[] headers = { "Description", "Priorité", "Résolue" };
            for (String h : headers) {
                com.lowagie.text.pdf.PdfPCell cell = new com.lowagie.text.pdf.PdfPCell(new Phrase(h, headerFont));
                cell.setBackgroundColor(Color.GRAY);
                cell.setPadding(5);
                table.addCell(cell);
            }

            for (Recommandation reco : selected.getRecommandations()) {
                table.addCell(new Phrase(reco.getDescription(), normalFont));
                table.addCell(new Phrase(reco.getPriorite(), normalFont));
                table.addCell(new Phrase(reco.isResolue() ? "OUI" : "NON", normalFont));
            }

            document.add(table);
            document.close();

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Export PDF Réussi");
            alert.setHeaderText(null);
            alert.setContentText("Le rapport PDF a été généré avec succès !");
            alert.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setContentText("Erreur lors de l'export PDF : " + e.getMessage());
            alert.show();
        }
    }

    @FXML
    public void switchAdmin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/audit/auditaifx/main-view.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) allReportsView.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) {
            System.err.println("Erreur switch admin: " + e.getMessage());
        }
    }
}