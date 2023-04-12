package cofferdam.factories;

import cofferdam.cleints.SiteWiseModels;
import cofferdam.generated.types.AssetModel;
import cofferdam.generated.types.Property;
import cofferdam.generated.types.PropertyType;
import graphql.com.google.common.collect.ImmutableList;
import software.amazon.awssdk.services.iottwinmaker.model.GetComponentTypeResponse;

import java.util.List;
import java.util.Map;

public class PropertiesFactory {
    private final List<Object> components;
    private String modelId;
    private final ImmutableList.Builder<Property> props = new ImmutableList.Builder<>();
    private List<Property> properties;
    private String assetId;

    public PropertiesFactory(List<Object> components) {
        this.components = components;
        this.transform();
    }

    private void transform() {
        for (Object component : this.components) {
            this.transformComponent((Map<String, Object>) component);
        }
        this.properties = props.build();
    }

    private void transformComponent(Map<String, Object> component) {
        this.transformProperties((List<Map<String, Object>>) component.get("properties"));
    }

    private void transformProperties(List<Map<String, Object>> properties) {
        for (Map<String, Object> property : properties) {
            String propertyName = (String) property.get("propertyName");
            // this is a field of the Asset
            if (propertyName.equalsIgnoreCase("sitewiseAssetModelId")) {
                this.modelId = (String) property.get("propertyValue");
            } else if (propertyName.equalsIgnoreCase("sitewiseAssetId")) {
                this.assetId = (String) property.get("propertyValue");
            }
        }

        for (Map<String, Object> property : properties) {
            if (!property.containsKey("definition")) {
                continue;
            }
            // unpack definition:
            Map<String, Object> definition = (Map<String, Object>) property.get("definition");
            Map<String, Object> configuration = (Map<String, Object>) definition.get("configuration");
            PropertyType type = PropertyType.valueOf((String) configuration.get("sitewisePropertyType"));

            // only include attributes, everything else can be deduced from the model
            if (type != PropertyType.ATTRIBUTE) {
                continue;
            }

            // this is a "real" SiteWise Property
            Property.Builder propBuilder = Property.newBuilder();
            propBuilder.name((String)definition.get("displayName"));
            propBuilder.id((String)configuration.get("sitewisePropertyId"));
            props.add(propBuilder.build());
        }
    }

    public List<Property> getProperties() {
        return this.properties;
    }

    public String getModelId() {
        return this.modelId;
    }

    public String getAssetId() {
        return this.assetId;
    }
}
