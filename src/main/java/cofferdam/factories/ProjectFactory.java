package cofferdam.factories;

import cofferdam.generated.types.Project;
import cofferdam.util.JsonConverter;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

public class ProjectFactory {
    public static Project build(Map<String, String> arguments, LambdaLogger logger) {
        String projectAsJson = new JsonConverter().toJson(arguments);
        logger.log("PROJECT AS JSON:  " + projectAsJson);
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(projectAsJson, Project.class);

        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
