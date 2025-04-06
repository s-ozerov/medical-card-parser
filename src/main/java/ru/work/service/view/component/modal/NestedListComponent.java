package ru.work.service.view.component.modal;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import ru.work.service.view.component.DialogComponent;
import ru.work.service.view.component.ExceptionBox;
import ru.work.service.view.util.Constants;
import ru.work.service.view.util.ImageFactory;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static ru.work.service.view.util.ImageFactory.buildSize24;
import static ru.work.service.view.util.StyleFactory.removeBackground;
import static ru.work.service.view.util.StyleFactory.setSize;

@Slf4j
public class NestedListComponent {

    @Getter
    private boolean changed = false;
    private Item lastSelectedItem = null;
    private int lastSelectedItemCount = 0;

    private ListView<Item> mainListView;
    private ListView<String> nestedListView;

    public Map<String, List<String>> getItems() {
        Map<String, List<String>> items = new TreeMap<>();
        for (Item item : mainListView.getItems()) {
            items.put(item.getName(), new LinkedList<>(item.getElements()));
        }
        return items;
    }

    public <T, O> void display(String title, Map<T, List<O>> items) {
        Stage window = new Stage();

        this.changed = false;

        HBox top = new HBox(10);
        setSize(top, 500, 200);

        HBox bottom = new HBox(10);

        mainListView = prepareDataItems(items);
        mainListView.getSelectionModel().clearSelection();
        bottom.setVisible(false);
        setSize(mainListView, 400, 380);
        setSize(top, 500, 380);

        Button addMainButton = buildButton(ImageFactory.ImageName.ADD);
        Button removeMainButton = buildButton(ImageFactory.ImageName.REMOVE);
        Button editMainButton = buildButton(ImageFactory.ImageName.EDIT);

        nestedListView = new ListView<>(FXCollections.observableArrayList());
        setSize(nestedListView, 400, 170);

        Button addNestedButton = buildButton(ImageFactory.ImageName.ADD);
        Button removeNestedButton = buildButton(ImageFactory.ImageName.REMOVE);
        Button editNestedButton = buildButton(ImageFactory.ImageName.EDIT);

        addMainButton.setOnAction(e -> {
            DialogComponent component = new DialogComponent("Добавить осн. элемент", "Введите название осн. элемента:");
            component.show();
            String mainNewItem = component.getText();
            if (StringUtils.isNotBlank(mainNewItem)) {
                if (mainListView.getItems().stream().noneMatch(i -> i.getName().equalsIgnoreCase(mainNewItem))) {
                    var newItem = new Item(mainNewItem);
                    mainListView.getItems().add(newItem);

                    changed = true;
                } else {
                    ExceptionBox.displayWarn("Не удалось добавить осн. элемент", "Данное значение «%s» уже используется".formatted(mainNewItem));
                    log.debug("Item [{}] already exists", mainNewItem);
                }
            }
        });

        removeMainButton.setOnAction(e -> {
            Item selectedItem = mainListView.getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                mainListView.getItems().remove(selectedItem);

                changed = true;
            }
        });

        editMainButton.setOnAction(e -> {
            Item selectedMainItem = mainListView.getSelectionModel().getSelectedItem();
            if (selectedMainItem != null) {
                DialogComponent component = new DialogComponent("Изменить осн. элемент", "Изменить название «%s» на:".formatted(selectedMainItem.getName()));
                component.show();
                String newName = component.getText();
                if (StringUtils.isNotBlank(newName)) {
                    if (mainListView.getItems().stream().noneMatch(i -> i.getName().equalsIgnoreCase(newName))) {
                        log.debug("Needed main item: {}", selectedMainItem.getName());
                        int editIndex = mainListView.getItems().indexOf(selectedMainItem);

                        LinkedList<String> list = new LinkedList<>(selectedMainItem.getElements());

                        mainListView.getItems().set(editIndex, new Item(newName, list));
                        mainListView.getSelectionModel().select(editIndex);

                        changed = true;

                        log.debug("Element [{}] changed to [{}]", selectedMainItem.getName(), newName);
                    } else {
                        ExceptionBox.displayWarn("Не удалось добавить осн. элемент", "Данное значение «%s» уже используется".formatted(newName));
                        log.debug("Item [{}] already exists", newName);
                    }
                }
            } else {
                log.debug("Select main item");
            }
        });

        addNestedButton.setOnAction(e -> {
            Item selectedMainItem = mainListView.getSelectionModel().getSelectedItem();
            if (selectedMainItem != null) {
                DialogComponent component = new DialogComponent("Добавить нас. элемент", "Введите название нас. элемента для «%s»:".formatted(selectedMainItem.getName()));
                component.show();
                String nestedNewItem = component.getText();
                if (StringUtils.isNotBlank(nestedNewItem)) {

                    String notUniqueItemName = null;
                    for (Item item : mainListView.getItems()) {
                        for (String name : item.getElements()) {
                            if (name.equalsIgnoreCase(nestedNewItem)) {
                                notUniqueItemName = item.getName();
                                break;
                            }
                        }
                        if (notUniqueItemName != null) {
                            break;
                        }
                    }
                    if (StringUtils.isBlank(notUniqueItemName)) {
                        int editIndex = mainListView.getItems().indexOf(selectedMainItem);

                        LinkedList<String> list = new LinkedList<>(selectedMainItem.getElements());
                        list.add(nestedNewItem);

                        mainListView.getItems().set(editIndex, new Item(selectedMainItem.getName(), list));
                        mainListView.getItems().sort(Comparator.comparing(Item::getName));
                        mainListView.getSelectionModel().select(editIndex);

                        changed = true;
                    } else {
                        ExceptionBox.displayWarn("Не удалось добавить нас. элемент", "Данное значение «%s» уже используется в «%s»".formatted(nestedNewItem, notUniqueItemName));
                    }
                }
            }
        });

        removeNestedButton.setOnAction(e -> {
            String selectedNestedItem = nestedListView.getSelectionModel().getSelectedItem();
            if (selectedNestedItem != null) {
                Item selectedMainItem = mainListView.getSelectionModel().getSelectedItem();
                if (selectedMainItem != null) {
                    int editIndex = mainListView.getItems().indexOf(selectedMainItem);

                    LinkedList<String> list = new LinkedList<>(selectedMainItem.getElements());
                    list.remove(selectedNestedItem);

                    mainListView.getItems().set(editIndex, new Item(selectedMainItem.getName(), list));
                    mainListView.getSelectionModel().select(editIndex);

                    changed = true;
                }
            }
        });

        editNestedButton.setOnAction(e -> {
            String selectedNestedItem = nestedListView.getSelectionModel().getSelectedItem();
            if (selectedNestedItem != null) {
                DialogComponent component = new DialogComponent("Изменить нас. элемент", "Изменить название «%s» на:".formatted(selectedNestedItem));
                component.show();
                String nestedNewItem = component.getText();
                if (StringUtils.isNotBlank(nestedNewItem)) {
                    Item selectedMainItem = mainListView.getSelectionModel().getSelectedItem();
                    if (selectedMainItem != null) {
                        String notUniqueItemName = null;
                        for (Item item : mainListView.getItems()) {
                            for (String name : item.getElements()) {
                                if (name.equalsIgnoreCase(nestedNewItem)) {
                                    notUniqueItemName = item.getName();
                                    break;
                                }
                            }
                            if (notUniqueItemName != null) {
                                break;
                            }
                        }
                        if (StringUtils.isBlank(notUniqueItemName)) {
                            log.debug("Needed main item: {}", selectedMainItem);
                            log.debug("Needed nested item: {}", selectedNestedItem);
                            int editIndex = mainListView.getItems().indexOf(selectedMainItem);

                            LinkedList<String> list = new LinkedList<>(selectedMainItem.getElements());
                            int index = list.indexOf(selectedNestedItem);
                            list.set(index, nestedNewItem);

                            mainListView.getItems().set(editIndex, new Item(selectedMainItem.getName(), list));
                            mainListView.getSelectionModel().select(editIndex);

                            changed = true;
                            log.debug("Element [{}] changed to [{}]", selectedNestedItem, nestedNewItem);
                        } else {
                            ExceptionBox.displayWarn("Не удалось добавить нас. элемент", "Данное значение «%s» уже используется в «%s»".formatted(nestedNewItem, notUniqueItemName));
                        }
                    }
                } else {
                    log.debug("Needed element is empty");
                }
            }
        });

        mainListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                nestedListView.getItems().clear();
                Item item = mainListView.getSelectionModel().getSelectedItem();
                nestedListView.getItems().addAll(item.getElements());
            }
        });

        mainListView.setOnMouseClicked(event -> {
            if (event.getButton().equals(MouseButton.PRIMARY)) {
                Item selectedItem = mainListView.getSelectionModel().getSelectedItem();
                lastSelectedItemCount++;
                if (lastSelectedItem != null && selectedItem.getName().equals(lastSelectedItem.getName())) {
                    if (lastSelectedItemCount % 2 == 0) {
                        lastSelectedItemCount = 0;
                        bottom.setVisible(true);
                        setSize(mainListView, 400, 200);
                        setSize(top, 500, 200);
                    } else {
                        bottom.setVisible(false);
                        setSize(mainListView, 400, 380);
                        setSize(top, 500, 380);
                    }
                } else {
                    bottom.setVisible(true);
                    setSize(mainListView, 400, 200);
                    setSize(top, 500, 200);
                }

                lastSelectedItem = selectedItem;
            }
        });

        VBox mainControls = new VBox(10, addMainButton, removeMainButton, editMainButton);
        top.getChildren()
                .addAll(mainListView, mainControls);

        VBox nestedControls = new VBox(10);
        nestedControls.getChildren()
                .addAll(addNestedButton, removeNestedButton, editNestedButton);

        bottom.getChildren()
                .addAll(nestedListView, nestedControls);

        VBox root = new VBox(10, top, bottom);
        root.setPadding(new Insets(10));

        Scene scene = new Scene(root, 500, 400);
        window.setResizable(false);
        window.initModality(Modality.APPLICATION_MODAL);
        window.getIcons().

                add(new Image(Constants.MAIN_ICO));
        window.setTitle(StringUtils.isNotBlank(title) ? title : "Управление вложенными списками");
        window.setScene(scene);
        window.showAndWait();
    }

    private Button buildButton(ImageFactory.ImageName imageName) {
        Button button = new Button();
        button.setGraphic(buildSize24(imageName));
        removeBackground(button);
        return button;
    }

    private <T, O> ListView<Item> prepareDataItems(Map<T, List<O>> items) {
        log.debug("Init items with size: {}", items.size());
        return new ListView<>(FXCollections.observableArrayList(items.entrySet().stream()
                .map(item -> {
                    Item newItem = new Item(item.getKey().toString(), item.getValue().stream().map(Object::toString).collect(LinkedList::new, LinkedList::add, LinkedList::addAll));
                    log.trace("Added item [{}] with elements: {}", newItem.getName(), StringUtils.join(newItem.getElements(), ","));
                    return newItem;
                }).toList()));
    }

}
