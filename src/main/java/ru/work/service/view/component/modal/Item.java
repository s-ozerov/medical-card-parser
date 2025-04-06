package ru.work.service.view.component.modal;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.LinkedList;
import java.util.List;

@Getter
public class Item {

    private final String name;
    private final LinkedList<String> elements;

    public Item(String name) {
        this.name = name;
        this.elements = new LinkedList<>();
    }

    public Item(String name, LinkedList<String> elements) {
        this.name = name;
        this.elements = elements;
    }

    @Override
    public String toString() {
        return name;
    }

}
