package cofferdam.cleints;

import cofferdam.factories.AssetFactory;
import cofferdam.factories.HierarchyFactory;
import cofferdam.generated.types.Asset;
import cofferdam.generated.types.HierarchyContainer;
import cofferdam.generated.types.HierarchyDefinition;
import cofferdam.generated.types.Project;
import cofferdam.generated.types.ProjectAssetExpansion;
import cofferdam.generated.types.ProjectDescription;
import cofferdam.generated.types.ProjectFacet;
import cofferdam.types.Collator;
import cofferdam.util.ArgumentUtils;
import cofferdam.util.ChildrenQueryBuilder;
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
import java.util.stream.Collectors;


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
        final String query = buildQuery(project, models).build().buildQuery();
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

    public boolean isAssetInProject(Project project, String testAssetId) {
        SiteWiseModels models = new SiteWiseModels(this.logger, project.getWorkspaceName());
        models.loadModelCache();
        SiteWiseAttributeValues attributeValues = new SiteWiseAttributeValues(logger);

        List<Asset> assets = new ArrayList<>();
        final String query = buildQuery(project, models)
                .testAssetId(testAssetId)
                .build().buildQuery();
        logger.log("KG EXISTS QUERY: " + query);
        ExecuteQueryResponse response = queryKnowledgeGraph(query, project);
        for (Row row : response.rows()) {
            if (row.hasRowData()) {
                Document targetDoc = row.rowData().get(KnowledgeGraphQueryBuilder.TARGET_COLUMN_INDEX);
                if (targetDoc.isMap()) {
                    assets.add(toAsset(targetDoc));
                }
            }
        }


        return !assets.isEmpty();
    }

    /**
     * Describe the children of an Asset node
     * @param parentAssetId
     * @return
     */
    public ProjectAssetExpansion describeChildren(String parentAssetId, Project project) {
        ChildrenQueryBuilder queryBuilder = ChildrenQueryBuilder.builder().parentAssetId(parentAssetId).build();
        String query = queryBuilder.buildQuery();
        ExecuteQueryResponse response = this.queryKnowledgeGraph(query, project);

        Collator<HierarchyDefinition, Asset> hierarchyCollator = new Collator<>();
        response.rows().stream().filter(row -> row.hasRowData())
                .forEach(row -> {
                    Document relationshipDoc = row.rowData().get(queryBuilder.getRelationshipColumnIndex());
                    HierarchyDefinition hierarchy = toHierarchy(relationshipDoc);
                    Document childAssetDoc = row.rowData().get(queryBuilder.getChildColumnIndex());
                    Asset child = toAsset(childAssetDoc);
                    hierarchyCollator.put(hierarchy, child);
                });

        SiteWiseAttributeValues attributeValues = new SiteWiseAttributeValues(logger);
        List<Asset> allAssets = hierarchyCollator.contents().values().stream().flatMap(list -> list.stream()).collect(Collectors.toList());
        attributeValues.applyAttributeValues(allAssets);

        List<HierarchyContainer> children = hierarchyCollator.contents().entrySet().stream().map(entry -> HierarchyContainer.newBuilder()
                .hierarchy(entry.getKey())
                .assets(entry.getValue())
                .build()).collect(Collectors.toList());

        return ProjectAssetExpansion.newBuilder().children(children).build();
    }

    private HierarchyDefinition toHierarchy(Document relationshipDoc) {
        Map<String, Object> hierachyData = new DocumentConverter().mapMapDocument(relationshipDoc);
        HierarchyFactory hierarchyFactory = new HierarchyFactory(hierachyData);
        return hierarchyFactory.buildHierarchy();
    }

    private Asset toAsset(Document doc) {
        Map<String, Object> assetData = new DocumentConverter().mapMapDocument(doc);
        AssetFactory assetFactory = new AssetFactory(assetData);
        return assetFactory.buildAsset();
    }

    private KnowledgeGraphQueryBuilder.KnowledgeGraphQueryBuilderBuilder buildQuery(Project project, SiteWiseModels models) {
        ProjectFacet targets = project.getTargets();
        KnowledgeGraphQueryBuilder.KnowledgeGraphQueryBuilderBuilder builder = KnowledgeGraphQueryBuilder.builder();
        if (targets == null) {
            throw new IllegalArgumentException("targets is required");
        }
        if (ArgumentUtils.isNotEmpty(targets.getAssetIds())) {
            builder.targetAssetIds(targets.getAssetIds());
        } else if (ArgumentUtils.isNotEmpty(targets.getModelNames())) {
            List<String> modelIds = targets.getModelNames().stream()
                    .map(modelName -> models.findAssetModelByName(modelName).getId()).collect(Collectors.toList());
            builder.targetAssetModelIds(modelIds);
        }
        ProjectFacet ancestors = project.getAncestors();
        if (ancestors != null) {
            if (ArgumentUtils.isNotEmpty(ancestors.getAssetIds())) {

                builder.ancestorAssetIds(ancestors.getAssetIds());
            } else if (ArgumentUtils.isNotEmpty(ancestors.getModelNames())) {
                List<String> modelIds = ancestors.getModelNames().stream()
                        .map(modelName -> models.findAssetModelByName(modelName).getId()).collect(Collectors.toList());
                builder.ancestorAssetModelIds(modelIds);
            }
        }

        return builder;
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
