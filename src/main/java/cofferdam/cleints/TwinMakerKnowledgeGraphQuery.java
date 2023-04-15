package cofferdam.cleints;

import cofferdam.factories.AssetFactory;
import cofferdam.generated.types.Asset;
import cofferdam.generated.types.Project;
import cofferdam.generated.types.ProjectDescription;
import cofferdam.generated.types.ProjectFacet;
import cofferdam.util.ArgumentUtils;
import cofferdam.util.DocumentConverter;
import cofferdam.util.KnowledgeGraphQueryBuilder;
import cofferdam.util.ResourceIdentifierDistinctFilter;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iottwinmaker.IoTTwinMakerClient;
import software.amazon.awssdk.services.iottwinmaker.model.ExecuteQueryRequest;
import software.amazon.awssdk.services.iottwinmaker.model.ExecuteQueryResponse;
import software.amazon.awssdk.services.iottwinmaker.model.Row;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class TwinMakerKnowledgeGraphQuery {
    private final int MAX_RESULTS = 100; // docs say 250, bot3 checks for 100, who knows ¯\_(ツ)_/¯
    private final LambdaLogger logger;
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public TwinMakerKnowledgeGraphQuery(LambdaLogger logger) {
        this.logger = logger;
    }

    private IoTTwinMakerClient getClient() {
        Region region = Region.US_EAST_1;
        return IoTTwinMakerClient.builder()
                .region(region)
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .build();
    }

    public ProjectDescription describeProject(Project project) {
        SiteWiseModels models = new SiteWiseModels(this.logger, project.getWorkspaceName());
        models.loadModelCache();
        SiteWiseAttributeValues attributeValues = new SiteWiseAttributeValues(logger);

        List<Asset> assets = new ArrayList<>();
        final String query = buildQuery(project, models);
        logger.log("KG QUERY: " + query);
        ExecuteQueryResponse response = queryKnowledgeGraph(query, project);
        for (Row row : response.rows()) {
            if (row.hasRowData()) {
                Document targetDoc = row.rowData().get(KnowledgeGraphQueryBuilder.TARGET_COLUMN_INDEX);
                if (targetDoc.isMap()) {
                    assets.add(toAsset(targetDoc));
                }
            }
        }

        //TODO: remove when DISTINCT is implemented, some result types generate duplicates right now
        assets = ResourceIdentifierDistinctFilter.apply(assets);

        // decorate with current attribute values
        attributeValues.applyAttributeValues(assets);
        return ProjectDescription.newBuilder()
                .knowledgeGraphQuery(query)
                .assets(assets)
                .models(models.getModels(assets))
                .build();
    }

    private Asset toAsset(Document doc) {
        Map<String, Object> assetData = new DocumentConverter().mapMapDocument(doc);
        //logger.log("ASSET (maybe?): " + gson.toJson(assetData));
        AssetFactory assetFactory = new AssetFactory(assetData);
        return assetFactory.buildAsset();
    }

    private String buildQuery(Project project, SiteWiseModels models) {
        ProjectFacet targets = project.getTargets();
        KnowledgeGraphQueryBuilder.KnowledgeGraphQueryBuilderBuilder builder = KnowledgeGraphQueryBuilder.builder();
        if (targets == null) {
            throw new IllegalArgumentException("targets is required");
        }
        if (ArgumentUtils.isNotEmpty(targets.getAssetIds())) {
            // TODO: when OR is supported, add all IDs
            builder.targetAssetId(targets.getAssetIds().get(0));
        } else if (ArgumentUtils.isNotEmpty(targets.getModelNames())) {
            // TODO: when OR is supported, add all model IDs
            String modelName = targets.getModelNames().get(0);
            builder.targetAssetModelId(models.findAssetModelByName(modelName).getId());
        }
        ProjectFacet ancestors = project.getAncestors();
        if (ancestors != null) {
            if (ArgumentUtils.isNotEmpty(ancestors.getAssetIds())) {
                // TODO: when OR is supported, add all IDs
                builder.ancestorAssetId(ancestors.getAssetIds().get(0));
            } else if (ArgumentUtils.isNotEmpty(ancestors.getModelNames())) {
                // TODO: when OR is supported, add all model IDs
                String modelName = ancestors.getModelNames().get(0);
                builder.ancestorAssetModelId(models.findAssetModelByName(modelName).getId());
            }
        }

        return builder.build().buildProjectRootsQuery();
    }

    private ExecuteQueryResponse queryKnowledgeGraph(String query, Project project) {
        return queryKnowledgeGraph(project.getWorkspaceName(), query);
    }

    private ExecuteQueryResponse queryKnowledgeGraph(String workspaceName, String partiqlQuery) {
        IoTTwinMakerClient client = getClient();
        ExecuteQueryRequest request = ExecuteQueryRequest.builder()
                .maxResults(MAX_RESULTS)
                .queryStatement(partiqlQuery)
                .workspaceId(workspaceName)
                .build();
        return client.executeQuery(request);
    }
}
