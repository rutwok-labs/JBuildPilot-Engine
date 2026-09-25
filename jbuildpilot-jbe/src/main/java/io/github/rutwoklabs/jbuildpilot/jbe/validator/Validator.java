package io.github.rutwoklabs.jbuildpilot.jbe.validator;

import io.github.rutwoklabs.jbuildpilot.jbe.ast.AstNodes.*;
import io.github.rutwoklabs.jbuildpilot.jbe.ast.Node;
import io.github.rutwoklabs.jbuildpilot.jbe.model.JbeProgram;

import java.util.ArrayList;
import java.util.List;

public class Validator {

    public JbeProgram validate(ProgramNode ast) {
        String projectName = null;
        String javaVersion = null;
        String buildSystem = null;
        String mainClass = null;
        List<String> dependencies = new ArrayList<>();

        for (Node decl : ast.declarations) {
            if (decl instanceof ProjectNode) {
                if (projectName != null) throw new RuntimeException("Duplicate project declaration.");
                projectName = ((ProjectNode) decl).name;
                if (projectName.isBlank()) throw new RuntimeException("Project name cannot be blank.");
            } else if (decl instanceof JavaNode) {
                if (javaVersion != null) throw new RuntimeException("Duplicate java block.");
                javaVersion = ((JavaNode) decl).version;
                if (javaVersion.isBlank()) throw new RuntimeException("Java version cannot be blank.");
            } else if (decl instanceof BuildNode) {
                if (buildSystem != null) throw new RuntimeException("Duplicate build block.");
                buildSystem = ((BuildNode) decl).system;
                if (buildSystem == null || buildSystem.isBlank()) throw new RuntimeException("Build system cannot be blank.");
            } else if (decl instanceof RunNode) {
                if (mainClass != null) throw new RuntimeException("Duplicate run block.");
                mainClass = ((RunNode) decl).mainClass;
                if (mainClass.isBlank()) throw new RuntimeException("Main class cannot be blank.");
            } else if (decl instanceof DependenciesNode) {
                for (DependencyNode dep : ((DependenciesNode) decl).deps) {
                    if (dep.coordinate.isBlank()) throw new RuntimeException("Dependency coordinate cannot be blank.");
                    dependencies.add(dep.type + ":" + dep.coordinate);
                }
            }
        }

        if (projectName == null) {
            throw new RuntimeException("A 'project' declaration is required.");
        }
        if (buildSystem == null) {
            throw new RuntimeException("A 'build' block with a build system is required.");
        }

        return new JbeProgram(projectName, javaVersion, buildSystem, mainClass, dependencies);
    }
}
