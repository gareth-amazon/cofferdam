package cofferdam.cleints;

import cofferdam.generated.types.Asset;
import cofferdam.generated.types.AttributeValue;
import cofferdam.generated.types.Property;
import cofferdam.generated.types.PropertyType;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import graphql.com.google.common.collect.ImmutableList;
import graphql.com.google.common.collect.ImmutableMap;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iotsitewise.IoTSiteWiseClient;
import software.amazon.awssdk.services.iotsitewise.model.BatchGetAssetPropertyValueEntry;
import software.amazon.awssdk.services.iotsitewise.model.BatchGetAssetPropertyValueRequest;
import software.amazon.awssdk.services.iotsitewise.model.BatchGetAssetPropertyValueResponse;
import software.amazon.awssdk.services.iotsitewise.model.Variant;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class SiteWiseAttributeValues {
    private final int MAX_RESULTS = 250;
    private final LambdaLogger logger;
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public SiteWiseAttributeValues(LambdaLogger logger) {
        this.logger = logger;
    }

    private IoTSiteWiseClient getClient() {
        Region region = Region.US_EAST_1;
        return IoTSiteWiseClient.builder()
                .region(region)
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .build();
    }

    public void applyAttributeValues(List<Asset> assets) {
        // TODO: Pagination & limits. Right now its not an issue with the limited size of the data
        IoTSiteWiseClient client = getClient();
        BatchGetAssetPropertyValueRequest.Builder requestBuilder = BatchGetAssetPropertyValueRequest.builder();
        ImmutableList.Builder<BatchGetAssetPropertyValueEntry> entries = ImmutableList.builder();

        ImmutableMap.Builder<String, Property> propLookupBuilder = ImmutableMap.builder();
        AtomicInteger entryId = new AtomicInteger(0);

        assets.stream().forEach(asset -> {
            asset.getProperties().stream()
                    //.filter(property -> property.getType() == PropertyType.ATTRIBUTE)
                    .forEach(property -> {
                        String key = "entry_id_" + entryId.getAndIncrement();
                        entries.add(BatchGetAssetPropertyValueEntry.builder()
                                        .assetId(asset.getId())
                                        .propertyId(property.getId())
                                        .entryId(key)
                                .build());
                        propLookupBuilder.put(key, property);
            });
        });
        requestBuilder.entries(entries.build());
        ImmutableMap<String, Property> propLookup = propLookupBuilder.build();

        BatchGetAssetPropertyValueResponse results = client.batchGetAssetPropertyValue(requestBuilder.build());
        results.successEntries().forEach(entry -> {
            Property prop = propLookup.get(entry.entryId());
            // Note: we don't need to know the type here as all incorrect values will be null
            Variant value = entry.assetPropertyValue().value();
            prop.setValue(AttributeValue.newBuilder().booleanValue(value.booleanValue())
            .intValue(value.integerValue())
            .floatValue(value.doubleValue())
            .stringValue(value.stringValue()).build());
        });
    }
}
