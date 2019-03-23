package implementations;

import interfaces.SimpleStringMap;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class DistributedMap implements SimpleStringMap, Serializable {

    public Map<String, Integer> stringMap;

    public DistributedMap() {
        stringMap = new HashMap<>();
    }

    public DistributedMap(Map<String, Integer> stringMap) {
        this.stringMap = stringMap;
    }

    public boolean containsKey(String key){
        return stringMap.containsKey(key);
    }

    public Integer get(String key){
        return stringMap.get(key);
    }

    public void put(String key, Integer value){
        stringMap.put(key, value);
    }

    public Integer remove(String key){
        return stringMap.remove(key);
    }
}
