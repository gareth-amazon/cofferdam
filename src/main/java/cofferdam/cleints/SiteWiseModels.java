package cofferdam.cleints;

import cofferdam.factories.AssetModelFactory;
import cofferdam.generated.types.Asset;
import cofferdam.generated.types.AssetModel;
import cofferdam.util.ModelTreeNode;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import graphql.com.google.common.collect.ImmutableMap;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iotsitewise.IoTSiteWiseClient;
import software.amazon.awssdk.services.iotsitewise.model.DescribeAssetModelResponse;
import software.amazon.awssdk.services.iotsitewise.model.ListAssetModelsResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This class gets every model and builds a cache of the model definitions
 */
public class SiteWiseModels {
    private static final String COMPONENT_TYPE_ID_PREFIX = "iotsitewise.assetmodel:";
    private final int MAX_RESULTS = 200;
    private final LambdaLogger logger;
    private String workspaceName;
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private Map<String, AssetModel> assetModelsById;
    private Map<String, AssetModel> assetModelsByName;
    private ImmutableMap<String, DescribeAssetModelResponse> models;
    private List<ModelTreeNode> roots;

    public SiteWiseModels(LambdaLogger logger, String workspaceName) {
        this.logger = logger;
        this.workspaceName = workspaceName;
    }

    private IoTSiteWiseClient getClient() {
        Region region = Region.US_EAST_1;
        return IoTSiteWiseClient.builder()
                .region(region)
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .build();
    }

    public void loadModelCache() {
        // TODO: pagination. In the test we don't have more than 200 types
        ListAssetModelsResponse results = this.listAssetModels();
        ImmutableMap.Builder<String, AssetModel> idBuilder = ImmutableMap.builder();
        ImmutableMap.Builder<String, AssetModel> nameBuilder = ImmutableMap.builder();
        ImmutableMap.Builder<String, DescribeAssetModelResponse> modelsBuilder = new ImmutableMap.Builder<>();

        results.assetModelSummaries().stream().parallel().forEach(summary -> {
            DescribeAssetModelResponse assetModelDescription = describeAssetModels(summary.id());
            modelsBuilder.put(assetModelDescription.assetModelId(), assetModelDescription);
            AssetModel assetModel = AssetModelFactory.build(assetModelDescription);
            idBuilder.put(assetModel.getId(), assetModel);
            // TODO: many assets might share the same name in the future, use a list:
            nameBuilder.put(assetModel.getName(), assetModel);
        });

        this.models = modelsBuilder.build();
        this.assetModelsById = idBuilder.build();
        this.assetModelsByName = nameBuilder.build();
        this.roots = ModelTreeNode.buildTrees(this.models);
        logger.log("FOUND COMPONENTS:");
        logger.log(gson.toJson(this.assetModelsByName));
    }

    public List<AssetModel> getModels(List<Asset> assets) {
        HashMap<String, AssetModel> models = new HashMap<>();
        return assets.stream()
                .map(asset -> asset.getModelId())
                .distinct()
                .map(modelId -> this.findAssetModelById(modelId))
                .collect(Collectors.toList());
    }

    /**
     * Find a model by either its GUID ID or its TwinMaker Component Type Id
     * @param modelId
     * @return the AssetModel
     */
    public AssetModel findAssetModelById(String modelId) {
        if (modelId.startsWith(COMPONENT_TYPE_ID_PREFIX)) {
            return this.assetModelsById.get(modelId.substring(COMPONENT_TYPE_ID_PREFIX.length() - 1));
        } else {
            return this.assetModelsById.get(modelId);
        }
    }

    public AssetModel findAssetModelByName(String assetModelName) {
        return this.assetModelsByName.get(assetModelName);
    }

    private ListAssetModelsResponse listAssetModels() {
        return getClient().listAssetModels(builder -> builder.maxResults(MAX_RESULTS));
    }

    private DescribeAssetModelResponse describeAssetModels(String assetModelId) {
        return getClient().describeAssetModel(builder -> builder.assetModelId(assetModelId));
    }


}
