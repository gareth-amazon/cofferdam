package cofferdam.factories;

import cofferdam.generated.DgsConstants;
import cofferdam.generated.types.Project;
import cofferdam.util.JsonConverter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

public class ProjectFactory {
    private static final Logger logger = LogManager.getLogger(ProjectFactory.class);
    public static Project build(Map<String, String> arguments) {
        logger.info(new JsonConverter().toJson(arguments));
        return Project.newBuilder()
                .workspaceName(arguments.get(DgsConstants.PROJECT.WorkspaceName))
                .build();
    }
}
