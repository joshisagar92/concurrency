package ch4_composing_object;

import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ThreadSafe
public class DelegatingVehicleTracker {
    private final Map<String,Point> locations;
    private final Map<String, Point> unmodifiableMap;

    public DelegatingVehicleTracker(Map<String,Point> point) {
        locations = new ConcurrentHashMap<>(point);
        unmodifiableMap = Collections.unmodifiableMap(locations);
    }

    public Map<String,Point> getLocations(){
        return unmodifiableMap;
    }

    public Point getLocation(String key){
        return locations.get(key);
    }
    public void setLocation(String key,int x,int y){
        locations.replace(key,new Point(x,y));
    }
}

@Immutable
class Point{
    private final int x,y;
    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }
}
