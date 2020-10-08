package ch4_composing_object;


import net.jcip.annotations.ThreadSafe;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@ThreadSafe
public class MonitorVehicleTracker {
    //This is our state variable and we will take care of this for thread safety.
    private final Map<String, MutablePoint> locations;

    //One minor improvement we can do to this class is to use a static initializer
    public MonitorVehicleTracker(Map<String, MutablePoint> locations) {
        //We will do a deepCopy over here  because we dont want that our location is changed from outside world.
        this.locations = deepCopy(locations);
    }

    public synchronized Map<String, MutablePoint> getLocations(){
        //We will do a deepcopy over here as well because we dont want that our location is published to out side word with mutability.
        return deepCopy(locations);
    }

    public synchronized MutablePoint getLocation(String key){
        return new MutablePoint(locations.get(key));
    }

    public synchronized void setLocations(String key,int x, int y){
        MutablePoint mutablePoint = locations.get(key);
        // we are mutating the mutable MutationPoint Object over here, but it is fine as we are changing it withing the intrinsic Lock.
        mutablePoint.x = x;
        mutablePoint.y = y;
    }

    private Map<String, MutablePoint> deepCopy(Map<String, MutablePoint> locations) {
        Map<String, MutablePoint> result = new HashMap<>();
        for (Map.Entry<String, MutablePoint> entry : locations.entrySet()) {
                //we have added new object because location is passed from client and we dont want that effect to be here.
                result.put(entry.getKey(),new MutablePoint(entry.getValue()));
        }
        return Collections.unmodifiableMap(result); // You can not add anything in the location.
    }
}
class MutablePoint {
public int x,y;
    public MutablePoint() {
    }
    public MutablePoint(MutablePoint point) {
        this.x = point.x;
        this.y = point.y;
    }
}
