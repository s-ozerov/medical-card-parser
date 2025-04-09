package ru.work.service.view.component.treeview;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

import static ru.work.service.view.util.Constants.PATCH_CSS;
import static ru.work.service.view.util.StyleFactory.setSize;

@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TreeViewComponent {

    private String header = "Заголовок";
    private List<TreeViewItem> items = new ArrayList<>();

    public VBox create() {
        TreeItem<String> rootItem = new TreeItem<>(header);
        rootItem.setExpanded(true);

        for (TreeViewItem item : items) {
            addSection(rootItem, item);
        }

        TreeView<String> treeView = new TreeView<>(rootItem);
        treeView.setShowRoot(true);
        treeView.setStyle("-fx-font-size: 14px;");
        setSize(treeView, 600, 500);

        treeView.setCellFactory(tv -> new TreeCell<>() {
            private final Button infoButton = new Button("Подробнее..");
            private final HBox hbox = new HBox();

            {
                infoButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #2a9fd6; -fx-underline: true; -fx-padding: 0; -fx-font-weight: bold;");
                infoButton.setOnAction(e -> {
                    TreeItem<String> item = getTreeItem();
                    if (item != null && item.getParent() != rootItem) {
                        showInfoDialog(item.getValue(), (String) item.getGraphic().getUserData());
                    }
                });

                hbox.setSpacing(5);
                hbox.getChildren().add(infoButton);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    // Показываем кнопку только для подразделов (не для корня и не для разделов первого уровня)
                    TreeItem<String> currentItem = getTreeItem();
                    if (currentItem != null && currentItem.getParent() != null && currentItem.getParent() != rootItem) {
                        setGraphic(hbox);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });

        VBox root = new VBox(treeView);
        root.getStylesheets().add(PATCH_CSS + "tree-view-component.css"); //"-fx-padding: 20;" -fx-background-color: #f4f4f4;
        setSize(root, 600, 500);
        return root;
    }

    private void addSection(TreeItem<String> root, TreeViewItem item) {
        TreeItem<String> section = new TreeItem<>(item.getTitle());
        section.setExpanded(true);

        for (TreeViewItem.SubItem subItem : item.getSubItems()) {
            TreeItem<String> subsection = new TreeItem<>(subItem.getTitle());
            subsection.setGraphic(createInfoIcon());
            subsection.getGraphic().setUserData(subItem.getContent());
            section.getChildren().add(subsection);
        }

        root.getChildren().add(section);
    }

    private Label createInfoIcon() {
        Label icon = new Label("ℹ");
        icon.setStyle("-fx-text-fill: #2a9fd6; -fx-font-size: 16px;");
        return icon;
    }

    private void showInfoDialog(String title, String content) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.TRANSPARENT);

        VBox dialogVbox = new VBox(20);
        dialogVbox.setStyle("-fx-font-family: 'Bookman Old Style'; -fx-background-color: white; -fx-background-radius: 10; -fx-padding: 20; -fx-border-radius: 10; -fx-border-width: 1; -fx-border-color: #ddd; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 0);");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #333;");

        TextArea contentArea = new TextArea(content);
        contentArea.setEditable(false);
        contentArea.setWrapText(true);
        contentArea.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-font-size: 14px; -fx-text-fill: #555;");
        setSize(contentArea, 650, 380);

        Button closeButton = new Button("Закрыть");
        closeButton.setStyle("-fx-background-color: #2a9fd6; -fx-text-fill: white; -fx-background-radius: 5; -fx-padding: 8 16;");
        closeButton.setOnAction(e -> dialog.close());

        HBox buttonBox = new HBox(closeButton);
        buttonBox.setStyle("-fx-alignment: center-right;");

        dialogVbox.getChildren().addAll(titleLabel, contentArea, buttonBox);

        Scene dialogScene = new Scene(dialogVbox, 700, 530);
        dialogScene.setFill(Color.TRANSPARENT);
        dialog.setScene(dialogScene);
        dialog.showAndWait();
    }


}
