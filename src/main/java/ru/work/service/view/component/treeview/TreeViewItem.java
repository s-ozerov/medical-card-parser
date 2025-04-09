package ru.work.service.view.component.treeview;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
@AllArgsConstructor
public class TreeViewItem {

    private String title;
    private List<SubItem> subItems = new ArrayList<>();

    @Getter
    @AllArgsConstructor
    public static class SubItem {
        private String title;
        private String content;
    }

}
