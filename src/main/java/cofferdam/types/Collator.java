package cofferdam.types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Type that groups inputs by a key
 * @param <K> Key Type
 * @param <V> Value Type
 */
public class Collator<K, V> {
    private final Map<K, List<V>> filterContainer = new HashMap<>();

    public void put(K key, V value) {
        if (!filterContainer.containsKey(key)) {
            filterContainer.put(key, new ArrayList<>());
        }

        filterContainer.get(key).add(value);
    }

    public Map<K, List<V>> contents() {
        return this.filterContainer;
    }
}
