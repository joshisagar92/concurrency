package ch4_composing_object;

import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class VisualComponent {
    private final List<KeyListener> keyEvents = new CopyOnWriteArrayList();
    private final List<MouseListener> mouseEvents = new CopyOnWriteArrayList();

    public void addKeyListener(KeyListener listener){
        keyEvents.add(listener);
    }
    public void removeKeyListener(KeyListener listener){
        keyEvents.remove(listener);
    }
    public void removeMouseListener(MouseListener listener){
        mouseEvents.remove(listener);
    }
    public void addMouseListener(MouseListener listener){
        mouseEvents.add(listener);
    }

}
