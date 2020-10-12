package ch5_building_blocks;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.Vector;

public class Simple {

    public static void main(String[] args) {

        Vector<Object> vector = new Vector<>();
        for (int i = 0; i < vector.size(); i++) {
          //  doSomeThing(vector.get(i));
            
        }
        
    }


    
}


class HiddenIterator{
    private final Set<Integer> set = new HashSet<>();

    public synchronized void add(Integer i){
        set.add(i);
    }

    public synchronized void remove(Integer i){
        set.remove(i);
    }

    public void addThings(){
        Random r = new Random();
        for (int i = 0; i < 10; i++) {
            add(r.nextInt());
        }

        System.out.println("DEBUG : Added 10 elements to set " + set);

    }
}