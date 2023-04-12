package cofferdam.factories;

import cofferdam.cleints.SiteWiseModels;
import cofferdam.generated.types.Asset;

import java.util.List;
import java.util.Map;

public class AssetFactory {
    private final Map<String, Object> assetData;

    public AssetFactory(Map<String, Object> assetData) {
        this.assetData = assetData;
    }

    public Asset buildAsset() {
        List<Object> components = (List<Object>) assetData.get("components");

        PropertiesFactory propertiesFactory = new PropertiesFactory(components);
        return Asset.newBuilder()
                .id(propertiesFactory.getAssetId())
                .arn((String) assetData.get("arn"))
                .name((String) assetData.get("entityName"))
                .description((String) assetData.get("description"))
                .modelId(propertiesFactory.getModelId())
                .properties(propertiesFactory.getProperties())
                .build();
    }
}
