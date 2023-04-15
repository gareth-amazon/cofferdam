package cofferdam.util;

import graphql.com.google.common.collect.ImmutableList;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.ArrayList;

@Builder
@AllArgsConstructor
public class KnowledgeGraphQueryBuilder {
    public static final int TARGET_COLUMN_INDEX = 0;
    public static final int ANCESTOR_COLUMN_INDEX = 1;

    private String ancestorAssetId;
    private String ancestorAssetName;
    private String ancestorAssetModelId;

    private String targetAssetId;
    private String targetAssetName;
    private String targetAssetModelId;

    private boolean hasAncestorAssetRestriction() {
        return this.ancestorAssetId != null && !this.ancestorAssetId.isEmpty();
    }

    private boolean hasAncestorAssetNameRestriction() {
        return this.ancestorAssetName != null && !this.ancestorAssetName.isEmpty();
    }
    private boolean hasAncestorModelRestriction() {
        return this.ancestorAssetModelId != null && !this.ancestorAssetModelId.isEmpty();
    }

    public boolean hasAncestorRestriction() {
        return this.hasAncestorAssetRestriction()
                || this.hasAncestorModelRestriction()
                || hasAncestorAssetNameRestriction();
    }

    private boolean hasTargetModelRestriction() {
        return this.targetAssetModelId != null && !this.targetAssetModelId.isEmpty();
    }

    private boolean hasTargetNameRestriction() {
        return this.targetAssetName != null && !this.targetAssetName.isEmpty();
    }

    private boolean hasTargetAssetIdRestriction() {
        return this.targetAssetId != null && !this.targetAssetId.isEmpty();
    }

    private void appendSelectClause(StringBuilder sb) {
        sb.append("SELECT ");
        sb.append("target as Target");
        if (hasAncestorRestriction()) {
            sb.append(", ancestor as Ancestor");
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

        sb.append("(target),\n");

        variables.add("target.components as targetComponents");
        variables.add("targetComponents.properties as targetProperties");
        sb.append(String.join(",\n", variables));
    }

    private void appendWhereClause(StringBuilder sb) {
        sb.append("\nWHERE ");
        ArrayList<String> restrictions = new ArrayList();
        if (this.hasAncestorRestriction()) {
            if (hasAncestorModelRestriction()) {
                restrictions.add("ancestorProperties.propertyName = 'sitewiseAssetModelId'");
                restrictions.add("ancestorProperties.propertyValue = '" + this.ancestorAssetModelId +"'");
            } else if (this.hasAncestorAssetNameRestriction()) {
                restrictions.add("ancestor.entityName = '" + this.ancestorAssetName +"'");
            } else if (hasAncestorAssetRestriction()) {
                restrictions.add("ancestorProperties.propertyName = 'sitewiseAssetId'");
                restrictions.add("ancestorProperties.propertyValue = '" + this.ancestorAssetId +"'");
            }
        }

        if (this.hasTargetModelRestriction()){
            restrictions.add("targetProperties.propertyName = 'sitewiseAssetModelId'");
            restrictions.add("targetProperties.propertyValue = '" + this.targetAssetModelId +"'");
        } else if (this.hasTargetNameRestriction()) {
            restrictions.add("target.entityName = '" + this.targetAssetName +"'");
        } else if (this.hasTargetAssetIdRestriction()) {
            restrictions.add("targetProperties.propertyName = 'sitewiseAssetId'");
            restrictions.add("targetProperties.propertyValue = '" + this.targetAssetId +"'");
        }

        sb.append(String.join("\n    AND ", restrictions));
    }

    /**
     * Build a KG query for the root nodes of the Project
     * @return
     */
    public String buildProjectRootsQuery() {
        StringBuilder sb = new StringBuilder();
        appendSelectClause(sb);
        appendFromClause(sb);
        appendWhereClause(sb);
        return sb.toString();
    }

    // TODO:
    // buildAssetIsPresentQuery
    // buildAllDescendantsQuery
}
