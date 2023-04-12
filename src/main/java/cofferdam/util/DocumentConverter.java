package cofferdam.util;
import graphql.com.google.common.collect.ImmutableList;
import graphql.com.google.common.collect.ImmutableMap;
import software.amazon.awssdk.core.document.Document;

import java.math.BigInteger;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Map;

public class DocumentConverter {

    BigInteger MAX_LONG = BigInteger.valueOf(Long.MAX_VALUE);
    BigInteger MIN_LONG = BigInteger.valueOf(Long.MIN_VALUE);

    BigInteger MAX_INT = BigInteger.valueOf(Integer.MAX_VALUE);
    BigInteger MIN_INT = BigInteger.valueOf(Integer.MIN_VALUE);
    private boolean isScalarType(Document doc) {
        return !doc.isMap() && !doc.isList();
    }

    /*
    public Object mapNumberDocument(Document doc) {
        SdkNumber awsNumber = doc.asNumber();
        BigDecimal big = awsNumber.bigDecimalValue();
        BigDecimal[] result = big.divideAndRemainder(BigDecimal.ONE);
        if (result[1].equals(BigDecimal.ZERO)) {
            BigInteger bigIntVal = big.toBigIntegerExact();
            if (bigIntVal.compareTo(MIN_LONG) < 0 || bigIntVal.compareTo(MAX_LONG) > 0) {
                return bigIntVal;
            } else if (bigIntVal.compareTo(MIN_INT) < 0 || bigIntVal.compareTo(MAX_INT) > 0) {
                return bigIntVal.longValueExact();
            } else {
                return bigIntVal.intValueExact();
            }

        } else {
            // try floating point conversion
            // try integer conversion
            double doubleVal = big.doubleValue();
            float floatVal = big.floatValue();

            // If it cant be contained in a Double, return BigNumber
            if (doubleVal == Double.NEGATIVE_INFINITY || doubleVal == Double.POSITIVE_INFINITY) {
                return big;
            }

            // If it cant be contained in a Float return Double
            if (floatVal == Float.NEGATIVE_INFINITY || floatVal == Float.POSITIVE_INFINITY) {
                return doubleVal;
            }

            return floatVal;
        }
    }
    */
    private Object mapField(Document field) {
        if (isScalarType(field)) {
            return mapScalarType(field);
        } else if (field.isMap()) {
            return mapMapDocument(field);
        } else if (field.isList()) {
            return mapListDocument(field);
        } else {
            throw new IllegalStateException("Unexpected field type??!!");
        }
    }

    public Object mapScalarType(Document doc) {
        if (doc.isNumber()) {
            return doc.asNumber();
        } else if (doc.isString()) {
            return doc.asString();
        } else if (doc.isBoolean()) {
            return doc.asBoolean();
        } else if (doc.isNull()) {
            return null;
        } else {
            throw new IllegalStateException("Document of undeclared type, expected the Null type");
        }
    }

    public Map<String, Object> mapMapDocument(Document document) {
        if (!document.isMap()) {
            throw new InputMismatchException("That document is not a map!");
        }

        ImmutableMap.Builder<String, Object> mapBuilder = new ImmutableMap.Builder<>();
        for (Map.Entry<String, Document> entry : document.asMap().entrySet()) {
            mapBuilder.put(entry.getKey(), mapField(entry.getValue()));
        }
        return mapBuilder.build();
    }

    public List<Object> mapListDocument(Document document) {
        if (!document.isList()) {
            throw new InputMismatchException("That document is not a list!");
        }
        List<Document> list = document.asList();
        ImmutableList.Builder<Object> listBuilder = new ImmutableList.Builder<>();
        for (Document item : document.asList()) {
           listBuilder.add(mapField(item));
        }
        return listBuilder.build();
    }
}
