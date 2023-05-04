//  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//  SPDX-License-Identifier: MIT-0

package cofferdam;

import cofferdam.cleints.TwinMakerKnowledgeGraphQuery;
import cofferdam.factories.ProjectFactory;
import cofferdam.generated.types.Project;
import cofferdam.generated.types.ProjectAssetExpansion;
import cofferdam.generated.types.ProjectDescription;
import cofferdam.util.JsonConverter;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Map;

/**
 * Handler for Hello World.
 */
public class ProjectExpandTreeHandler implements RequestHandler<Map<String, Object>, ProjectAssetExpansion> {
    private static final JsonConverter jsonConverter = new JsonConverter();
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public ProjectExpandTreeHandler() {
    }

    public ProjectAssetExpansion handleRequest(final Map<String, Object> input, final Context context) {
        LambdaLogger logger = context.getLogger();
        // log execution details
        logger.log("ENVIRONMENT VARIABLES: " + gson.toJson(System.getenv()));
        logger.log("CONTEXT: " + gson.toJson(context));
        // process event
        logger.log("RAW INPUT: " + gson.toJson(input));

        Map<String, String> arguments = (Map<String, String>) input.get("project");
        Project project = ProjectFactory.build(arguments, logger);
        logger.log("PROJECT INPUT: " + gson.toJson(project));
        String parentAssetId = (String) input.get("assetId");

        try {
            TwinMakerKnowledgeGraphQuery knowledgeGraph = new TwinMakerKnowledgeGraphQuery(logger);
            // 1) Does this asset even exist? Is this asset in the project?
            if (!knowledgeGraph.isAssetInProject(project, parentAssetId)) {
                throw new RuntimeException("Action Not Authorized 403");
            }
            // 2) Load the assets immediate children:
            ProjectAssetExpansion output = knowledgeGraph.describeChildren(parentAssetId, project);
            logger.log("OUTPUT: " + gson.toJson(output));
            return output;
        } catch (Exception e) {
            logger.log(e.toString());
            throw new RuntimeException(e);
        }
    }
}