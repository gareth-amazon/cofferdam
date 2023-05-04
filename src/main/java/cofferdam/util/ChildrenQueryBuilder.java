package cofferdam.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Builder
@AllArgsConstructor
public class ChildrenQueryBuilder {
    private String parentAssetId;
    @Getter
    private int relationshipColumnIndex = 0;
    @Getter
    private int childColumnIndex = 1;

    /**
     * Build a Knowledge Graph query for the immediate children of the parent asset and the relationship AKA hierarchy
     * @return String query
     */
    public String buildQuery() {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT relationship as Relationship, child as Child\n");
        sb.append("FROM EntityGraph MATCH (parent)-[relationship]->(child), \n");
        sb.append("    parent.components AS parentComponents, \n");
        sb.append("    parentComponents.properties AS parentProperties\n");
        sb.append("WHERE\n");
        sb.append("    relationship.relationshipName != 'isChildOf'\n");
        sb.append("    AND parentProperties.propertyName = 'sitewiseAssetId'\n");
        sb.append("    AND parentProperties.propertyValue = '8de882a8-5fc8-4dea-854b-68ec2220dcad'\n");
        return sb.toString();
    }
}
