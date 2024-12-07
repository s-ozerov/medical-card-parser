package ru.work.service.view.component;

import javafx.scene.control.Button;
import lombok.SneakyThrows;
import ru.work.service.view.util.Constants;

public class BlockButton implements Runnable {

    private final Button button;
    private final Integer second;

    public BlockButton(Button button, Integer second) {
        this.button = button;
        this.second = second;
    }

    @Override
    @SneakyThrows(value = InterruptedException.class)
    public void run() {
        button.setDisable(true);
        Thread.sleep(second * Constants.SECOND);
        button.setDisable(false);
    }

}
