//  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//  SPDX-License-Identifier: MIT-0

package cofferdam;

import cofferdam.factories.ProjectFactory;
import cofferdam.generated.types.Asset;
import cofferdam.generated.types.Project;
import cofferdam.util.JsonConverter;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.List;
import java.util.Map;

/**
 * Handler for Hello World.
 */
public class DescribeProjectHandler implements RequestHandler<Map<String, Object>, List<Asset>> {
    private static final JsonConverter jsonConverter = new JsonConverter();
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public DescribeProjectHandler() {
    }

    public List<Asset> handleRequest(final Map<String, Object> input, final Context context) {
        LambdaLogger logger = context.getLogger();
        // log execution details
        logger.log("ENVIRONMENT VARIABLES: " + gson.toJson(System.getenv()));
        logger.log("CONTEXT: " + gson.toJson(context));
        // process event
        logger.log("INPUT: " + gson.toJson(input));

        Map<String, String> arguments = (Map<String, String>) input.get("project");
        Project project = ProjectFactory.build(arguments);

        try {
            TwinMakerKnowledgeGraphQuery knowledgeGraph = new TwinMakerKnowledgeGraphQuery(logger);
            List<Asset> assets = knowledgeGraph.describeProject(project);
            logger.log("OUTPUT: " + gson.toJson(assets));
            return assets;
        } catch (Exception e) {
            logger.log(e.toString());
            throw new RuntimeException(e);
        }
    }
}