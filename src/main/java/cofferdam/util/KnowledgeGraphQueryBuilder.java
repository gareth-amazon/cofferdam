package cofferdam.util;

import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.ArrayList;
import java.util.List;

@Builder
@AllArgsConstructor
public class KnowledgeGraphQueryBuilder {
    public static final int TARGET_COLUMN_INDEX = 0;
    public static final int ANCESTOR_COLUMN_INDEX = 1;

    private List<String> ancestorAssetIds;
    private List<String> ancestorAssetNames;
    private List<String> ancestorAssetModelIds;

    private List<String> targetAssetIds;
    private List<String> targetAssetNames;
    private List<String> targetAssetModelIds;

    // ID of an asset that may be in the Project, the goal of the query is to test if this is true
    private String testAssetId;

    private boolean hasAncestorAssetRestriction() {
        return this.ancestorAssetIds != null && !this.ancestorAssetIds.isEmpty();
    }

    private boolean hasAncestorAssetNameRestriction() {
        return this.ancestorAssetNames != null && !this.ancestorAssetNames.isEmpty();
    }
    private boolean hasAncestorModelRestriction() {
        return this.ancestorAssetModelIds != null && !this.ancestorAssetModelIds.isEmpty();
    }

    public boolean hasAncestorRestriction() {
        return this.hasAncestorAssetRestriction()
                || this.hasAncestorModelRestriction()
                || hasAncestorAssetNameRestriction();
    }

    private boolean hasTargetModelRestriction() {
        return this.targetAssetModelIds != null && !this.targetAssetModelIds.isEmpty();
    }

    private boolean hasTargetNameRestriction() {
        return this.targetAssetNames != null && !this.targetAssetNames.isEmpty();
    }

    private boolean hasTargetAssetIdRestriction() {
        return this.targetAssetIds != null && !this.targetAssetIds.isEmpty();
    }

    private boolean hasTestAssetId() {
        return this.testAssetId != null && !this.testAssetId.isEmpty();
    }

    private void appendSelectClause(StringBuilder sb) {
        sb.append("SELECT ");
        sb.append("target as Target");
        if (hasAncestorRestriction()) {
            sb.append(", ancestor as Ancestor");
        }
        if (hasTestAssetId()) {
            sb.append(", test as Test");
        }
        sb.append("\n");
    }

    private void appendFromClause(StringBuilder sb) {
        sb.append("FROM EntityGraph MATCH ");

        ArrayList<String> variables = new ArrayList<>();

        if (this.hasAncestorRestriction()) {
            sb.append("(ancestor)-[]->{1,5}");
            variables.add("ancestor.components as ancestorComponents");
            variables.add("ancestorComponents.properties as ancestorProperties");
        }

        sb.append("(target)");
        variables.add("target.components as targetComponents");
        variables.add("targetComponents.properties as targetProperties");

        if (this.hasTestAssetId()) {
            sb.append("-[]->{1,5}(test)");
            variables.add("test.components as testComponents");
            variables.add("testComponents.properties as testProperties");
        }

        sb.append(",\n");

        sb.append(String.join(",\n", variables));
    }

    private String toInClause(List<String> matches) {
        StringBuilder sb = new StringBuilder();
        sb.append("['");
        String.join("', '", matches);
        sb.append("']");
        return sb.toString();
    }

    private void appendWhereClause(StringBuilder sb) {
        sb.append("\nWHERE ");
        ArrayList<String> restrictions = new ArrayList();
        if (this.hasAncestorRestriction()) {
            if (hasAncestorModelRestriction()) {
                restrictions.add("ancestorProperties.propertyName = 'sitewiseAssetModelId'");
                restrictions.add("ancestorProperties.propertyValue IN " + toInClause(this.ancestorAssetModelIds));
            } else if (this.hasAncestorAssetNameRestriction()) {
                restrictions.add("ancestor.entityName IN " + toInClause(this.ancestorAssetNames));
            } else if (hasAncestorAssetRestriction()) {
                restrictions.add("ancestorProperties.propertyName = 'sitewiseAssetId'");
                restrictions.add("ancestorProperties.propertyValue IN " + toInClause(this.ancestorAssetIds));
            }
        }

        if (this.hasTargetModelRestriction()){
            restrictions.add("targetProperties.propertyName = 'sitewiseAssetModelId'");
            restrictions.add("targetProperties.propertyValue IN " + toInClause(this.targetAssetModelIds));
        } else if (this.hasTargetNameRestriction()) {
            restrictions.add("target.entityName IN " + toInClause(this.targetAssetNames));
        } else if (this.hasTargetAssetIdRestriction()) {
            restrictions.add("targetProperties.propertyName = 'sitewiseAssetId'");
            restrictions.add("targetProperties.propertyValue IN " + toInClause(this.targetAssetIds));
        }

        if (this.hasTestAssetId()) {
            restrictions.add("testProperties.propertyName = 'sitewiseAssetId'");
            restrictions.add("testProperties.propertyValue IN ['" + this.testAssetId + "']");
        }

        sb.append(String.join("\n    AND ", restrictions));
    }

    /**
     * Build a Knowledge Graph query for the root nodes of the Project
     * @return
     */
    public String buildQuery() {
        StringBuilder sb = new StringBuilder();
        appendSelectClause(sb);
        appendFromClause(sb);
        appendWhereClause(sb);
        return sb.toString();
    }
}
