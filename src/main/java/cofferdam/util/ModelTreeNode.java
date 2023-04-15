package cofferdam.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import software.amazon.awssdk.services.iotsitewise.model.AssetModelHierarchy;
import software.amazon.awssdk.services.iotsitewise.model.DescribeAssetModelResponse;

public class ModelTreeNode {
    private int referenceCount = 0;
    private final String modelId;
    private final Map<AssetModelHierarchy, ModelTreeNode> hierarchyRelationships = new HashMap<>();

    public ModelTreeNode(String model) {
        this.modelId = model;
    }

    public void attach(AssetModelHierarchy key, ModelTreeNode modelTreeNode) {
        modelTreeNode.referenceCount += 1;
        this.hierarchyRelationships.put(key, modelTreeNode);
    }

    public String getModelId() {
        return this.modelId;
    }

    public boolean hasChildren() {
        return this.hierarchyRelationships.size() > 0;
    }

    public boolean isRoot() {
        return this.referenceCount == 0;
    }

    public static List<ModelTreeNode> buildTrees(Map<String, DescribeAssetModelResponse> modelsLookup) {
        // first pass, make a node for every model:
        List<ModelTreeNode> allNodes = new ArrayList<>(modelsLookup.size());
        Map<String, ModelTreeNode> idToNodeMap = new HashMap<>(modelsLookup.size());
        modelsLookup.values().stream().forEach(model -> {
            ModelTreeNode node = new ModelTreeNode(model.assetModelId());
            allNodes.add(node);
            idToNodeMap.put(model.assetModelId(), node);
        });

        // for every node with hierarchies
        allNodes.forEach(node -> {
            DescribeAssetModelResponse nodeModel = modelsLookup.get(node.getModelId());
            nodeModel.assetModelHierarchies().forEach(hierarchy -> {
                // attach the child node to the parent
                String childModelId = hierarchy.childAssetModelId();
                node.attach(hierarchy, idToNodeMap.get(childModelId));
            });
        });

        // filter to only nodes with 0 references, meaning they were never attached to anything
        return allNodes.stream().filter(node -> node.isRoot()).collect(Collectors.toList());
    }
}
