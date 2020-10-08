package ch4_composing_object;

import java.util.concurrent.atomic.AtomicInteger;

public class NumberRange {
    private final AtomicInteger lower = new AtomicInteger(0);
    private final AtomicInteger upper = new AtomicInteger(0);

    public void setLower(int i){
        if(i > upper.get()){
            throw new IllegalStateException("Cant set lower to "+i +" > Upper");
        }
        lower.set(i);
    }

    public void setUpper(int i){
        if(i < lower.get()){
            throw new IllegalStateException("Cant set Upper to "+i +" < Lower");
        }
        upper.set(i);
    }
}
