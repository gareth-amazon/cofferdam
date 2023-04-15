package cofferdam.util;

import org.apache.logging.log4j.util.Strings;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class ArgumentUtils {
    public static boolean isEmpty( final Collection< ? > c ) {
        return c == null || c.isEmpty();
    }

    public static boolean isNotEmpty( final Collection< ? > c ) {
        return !isEmpty(c);
    }

    public static boolean isEmpty( final Map< ?, ? > m ) {
        return m == null || m.isEmpty();
    }

    public static boolean isNotEmpty( final Map< ?, ? > m ) {
        return !isEmpty(m);
    }
}
