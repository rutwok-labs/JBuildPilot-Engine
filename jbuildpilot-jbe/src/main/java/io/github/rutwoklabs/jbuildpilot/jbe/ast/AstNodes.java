package io.github.rutwoklabs.jbuildpilot.jbe.ast;

import java.util.List;

public class AstNodes {
    
    public static class ProgramNode implements Node {
        public final List<Node> declarations;
        public ProgramNode(List<Node> declarations) { this.declarations = declarations; }
    }

    public static class ProjectNode implements Node {
        public final String name;
        public ProjectNode(String name) { this.name = name; }
    }

    public static class JavaNode implements Node {
        public final String version;
        public JavaNode(String version) { this.version = version; }
    }

    public static class BuildNode implements Node {
        public final String system;
        public BuildNode(String system) { this.system = system; }
    }

    public static class RunNode implements Node {
        public final String mainClass;
        public RunNode(String mainClass) { this.mainClass = mainClass; }
    }

    public static class DependenciesNode implements Node {
        public final List<DependencyNode> deps;
        public DependenciesNode(List<DependencyNode> deps) { this.deps = deps; }
    }

    public static class DependencyNode implements Node {
        public final String type;
        public final String coordinate;
        public DependencyNode(String type, String coordinate) { 
            this.type = type; 
            this.coordinate = coordinate; 
        }
    }
}
