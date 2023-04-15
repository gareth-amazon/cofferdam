package cofferdam.util;

import cofferdam.generated.types.Asset;
import cofferdam.generated.types.ResourceIdentifier;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ResourceIdentifierDistinctFilter {
    public static <T extends ResourceIdentifier> List<T> apply(List<T> assets) {
        return assets.stream().filter(distinctByKey(ResourceIdentifier::getId)).collect(Collectors.toList());
    }

    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }
}
