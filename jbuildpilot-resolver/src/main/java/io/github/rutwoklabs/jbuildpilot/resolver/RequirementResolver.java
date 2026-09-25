package io.github.rutwoklabs.jbuildpilot.resolver;

import io.github.rutwoklabs.jbuildpilot.core.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves a ProjectModel into a structured RequirementGraph safely without execution.
 */
public class RequirementResolver {

    public ResolutionResult resolve(ProjectModel model) {
        RequirementGraph graph = new RequirementGraph();
        boolean successful = true;

        String projectId = model.getMetadata().getName() != null ? model.getMetadata().getName() : "root-project";
        RequirementNode root = new RequirementNode(projectId, null, RequirementStatus.AVAILABLE);
        graph.addNode(root);

        boolean hasJava = false;
        List<Requirement> requirements = model.getRequirements();
        
        // Group by identity to find intersections
        Map<String, List<Requirement>> grouped = new HashMap<>();
        for (Requirement req : requirements) {
            grouped.computeIfAbsent(req.getIdentity(), k -> new ArrayList<>()).add(req);
        }

        for (Map.Entry<String, List<Requirement>> entry : grouped.entrySet()) {
            List<Requirement> reqsForIdentity = entry.getValue();
            
            // Compute intersection
            RequirementStatus groupStatus = RequirementStatus.REQUIRED;
            VersionConstraint intersection = VersionConstraint.any();
            
            try {
                for (Requirement req : reqsForIdentity) {
                    VersionConstraint constraint = req.getVersionConstraint();
                    intersection = intersection.intersect(constraint);
                    if (intersection == null) {
                        groupStatus = RequirementStatus.INCOMPATIBLE;
                        successful = false;
                        break; // Conflict found
                    }
                }
            } catch (IllegalArgumentException e) {
                groupStatus = RequirementStatus.UNKNOWN;
                successful = false;
            }

            // Add nodes for all requirements in this identity group
            for (Requirement req : reqsForIdentity) {
                String nodeId = req.getType().name() + "-" + req.getIdentity();
                RequirementNode node = new RequirementNode(nodeId, req, groupStatus);

                if (req.getType() == RequirementType.JAVA) {
                    hasJava = true;
                }

                graph.addNode(node);
                
                // Build edges
                String edgeType = "DEPENDS_ON";
                if (req.getType() == RequirementType.MAVEN || req.getType() == RequirementType.GRADLE) {
                    edgeType = "BUILT_WITH";
                } else if (req.getType() == RequirementType.JAVA) {
                    edgeType = "RUNS_ON";
                } else if (req.getType() == RequirementType.PLUGIN) {
                    edgeType = "USES_PLUGIN";
                }

                graph.addEdge(new RequirementEdge(root, node, edgeType));
            }
        }

        // Phase 3 requirement: Detect missing Java requirement based purely on model analysis
        if (!hasJava) {
            RequirementNode missingJava = new RequirementNode("MISSING-JAVA", null, RequirementStatus.MISSING);
            graph.addNode(missingJava);
            graph.addEdge(new RequirementEdge(root, missingJava, "RUNS_ON"));
            successful = false; // A project without Java cannot be successfully resolved in our engine
        }

        return new ResolutionResult(graph, successful);
    }
}
