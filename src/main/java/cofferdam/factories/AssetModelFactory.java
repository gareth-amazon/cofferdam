package cofferdam.factories;

import cofferdam.generated.types.AssetModel;
import cofferdam.generated.types.AttributeValue;
import cofferdam.generated.types.DataType;
import cofferdam.generated.types.PropertyDefinition;
import graphql.com.google.common.collect.ImmutableList;
import software.amazon.awssdk.services.iotsitewise.model.AssetModelProperty;
import software.amazon.awssdk.services.iotsitewise.model.DescribeAssetModelResponse;
import software.amazon.awssdk.services.iotsitewise.model.PropertyType;
import software.amazon.awssdk.services.iottwinmaker.model.DataValue;

import java.util.List;

public class AssetModelFactory {
    public static AssetModel build(DescribeAssetModelResponse assetModelDescription) {
        return AssetModel.newBuilder()
                .arn(assetModelDescription.assetModelArn())
                .id(assetModelDescription.assetModelId())
                .name(assetModelDescription.assetModelName())
                .description(assetModelDescription.assetModelDescription())
                .properties(buildAssetModelProperties(assetModelDescription.assetModelProperties()))
                .build();
    }



    // TODO: would it have been faster to get the models from SiteWise?????
    // https://docs.aws.amazon.com/iot-sitewise/latest/APIReference/API_ListAssetModels.html
    // https://docs.aws.amazon.com/iot-sitewise/latest/APIReference/API_DescribeAssetModel.html
    private static List<PropertyDefinition> buildAssetModelProperties(List<AssetModelProperty> propertyDefinitions) {
        ImmutableList.Builder<PropertyDefinition> propDefBuilder = ImmutableList.builder();
        propertyDefinitions.forEach(def -> {
            PropertyDefinition.Builder propBuilder = PropertyDefinition.newBuilder();
            DataType dataType = DataType.valueOf(def.dataTypeAsString());
            PropertyType type = def.type();
            propBuilder.dataType(dataType)
                    .name(def.name())
                    .id(def.id())
                    .unit(def.unit());

            if (type.attribute() != null) {
                propBuilder.propertyType(cofferdam.generated.types.PropertyType.ATTRIBUTE);
                String defaultValue = def.type().attribute().defaultValue();
                AttributeValue.Builder attributeValue = AttributeValue.newBuilder();
                if (dataType == DataType.BOOLEAN) {
                    attributeValue.booleanValue(Boolean.valueOf(defaultValue));
                } else if (dataType == DataType.INTEGER) {
                    attributeValue.intValue(Integer.valueOf(defaultValue));
                } else if (dataType == DataType.DOUBLE) {
                    attributeValue.floatValue(Double.valueOf(defaultValue));
                } else if (dataType == DataType.STRING) {
                    attributeValue.stringValue(defaultValue);
                }
                propBuilder.defaultValue(attributeValue.build());
                // TODO: what to do with Struct??
            } else if (type.measurement() != null) {
                // TODO: more details
                propBuilder.propertyType(cofferdam.generated.types.PropertyType.MEASUREMENT);
            } else if (type.metric() != null) {
                // TODO: more details
                propBuilder.propertyType(cofferdam.generated.types.PropertyType.METRIC);
            } else if (type.transform() != null) {
                // TODO: more details
                propBuilder.propertyType(cofferdam.generated.types.PropertyType.TRANSFORM);
            }

            propDefBuilder.add(propBuilder.build());
        });
        return propDefBuilder.build();
    }
}
