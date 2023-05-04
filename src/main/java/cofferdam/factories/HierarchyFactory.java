package cofferdam.factories;

import cofferdam.generated.types.HierarchyDefinition;

import java.util.Map;

public class HierarchyFactory {
    private final Map<String, Object> relationshipData;

    public HierarchyFactory(Map<String, Object> relationshipData) {
        this.relationshipData = relationshipData;
    }

    public HierarchyDefinition buildHierarchy() {
        /* Sample for reference
        { "relationshipName": "Hierarchy_268d0da5-a0ef-4b1c-9a9a-035f19dd91ba",
         "targetEntityId": "04e65f1e-221c-41da-b5c9-49f224846ac0",
          "sourceComponentName": "sitewiseBase",
           "sourceEntityId": "01fa6033-e7a8-4844-acec-8965516325ed",
            "displayName": "motors", "sourceComponentTypeId": "iotsitewise.assetmodel:7fb76f2f-ff04-4517-9ed3-b08e96899056" }
         */

        return HierarchyDefinition.newBuilder()
                .id(((String) relationshipData.get("displayName")).substring("Hierarchy_".length()))
                .name((String) relationshipData.get("displayName"))
                .modelId("") // TK: maybe remove this?
                .build();
    }
}
