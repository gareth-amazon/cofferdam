package cofferdam;

import cofferdam.generated.types.Asset;
import cofferdam.generated.types.Project;
import cofferdam.util.DocumentConverter;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import graphql.com.google.common.collect.ImmutableList;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iottwinmaker.IoTTwinMakerClient;
import software.amazon.awssdk.services.iottwinmaker.model.ExecuteQueryRequest;
import software.amazon.awssdk.services.iottwinmaker.model.ExecuteQueryResponse;
import software.amazon.awssdk.services.iottwinmaker.model.Row;

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

    public Asset toAsset(Document doc) {
        Map<String, Object> assetData = new DocumentConverter().mapMapDocument(doc);
        logger.log("ASSET (maybe?): " + gson.toJson(assetData));
        return Asset.newBuilder()
                .id((String) assetData.get("arn")) // TODO: get from properties
                .arn((String) assetData.get("arn"))
                .name((String) assetData.get("entityName"))
                .description((String) assetData.get("description"))
                .modelId((String) assetData.get("arn")) // TODO: get from properties
                .build();
    }

    public List<Asset> describeProject(Project project) {
        ImmutableList.Builder<Asset> assets = ImmutableList.builder();
        ExecuteQueryResponse response = queryKnowledgeGraph(project);
        for (Row row : response.rows()) {
            if (row.hasRowData()) {
                for (Document doc : row.rowData()) {
                    if (doc.isMap()) {
                        assets.add(toAsset(doc));
                    }
                }
            }
        }
        return assets.build();
    }

    public ExecuteQueryResponse queryKnowledgeGraph(Project project) {
        ExecuteQueryResponse results = queryKnowledgeGraph(project.getWorkspaceName(), "" +
                "SELECT entity\n" +
                "FROM EntityGraph MATCH (entity)");

        return results;
    }

    public ExecuteQueryResponse queryKnowledgeGraph(String workspaceName, String partiqlQuery) {
        IoTTwinMakerClient client = getClient();
        ExecuteQueryRequest request = ExecuteQueryRequest.builder()
                .maxResults(MAX_RESULTS)
                .queryStatement(partiqlQuery)
                .workspaceId(workspaceName)
                .build();
        return client.executeQuery(request);
    }
}
