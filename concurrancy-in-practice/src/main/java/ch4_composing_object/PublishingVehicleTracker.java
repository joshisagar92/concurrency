package ch4_composing_object;

import net.jcip.annotations.Immutable;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PublishingVehicleTracker {
    private final Map<String,SafePoint> locations;
    private final Map<String, SafePoint> unmodifiableMap;

    public PublishingVehicleTracker(Map<String,SafePoint> point) {
        locations = new ConcurrentHashMap<>(point);
        unmodifiableMap = Collections.unmodifiableMap(locations);
    }

    public Map<String,SafePoint> getLocations(){
        return unmodifiableMap;
    }

    public SafePoint getLocation(String key){
        return unmodifiableMap.get(key);
    }
    public void setLocation(String key,int x,int y){
        if(locations.containsKey(key)){
            locations.get(key).set(x,y);
        }
    }

}

@Immutable
class SafePoint{
    private int x;
    private int y;

    public SafePoint(SafePoint p) {
        this(p.get());
    }

    private SafePoint(int[] ints) {
        this(ints[0],ints[1]);
    }

    public SafePoint(int x, int y) {
        this.x = x;
        this.y = y;
    }
    public synchronized int[] get() {
        return new int[] {x ,y};
    }
    public synchronized void set(int x,int y){
        this.x = x;
        this.y = y;
    }


}
