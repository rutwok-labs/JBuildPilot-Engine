package io.github.rutwoklabs.jbuildpilot.analyzer;

import io.github.rutwoklabs.jbuildpilot.core.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class MavenAnalyzer implements JavaDetector {

    public ProjectModel analyze(Path projectDir) {
        ProjectMetadata metadata = extractMetadata(projectDir);
        List<Requirement> requirements = new ArrayList<>();

        // 1. Record Build System
        requirements.add(new MavenRequirement(VersionConstraint.any(), RequirementStatus.UNKNOWN));

        // 2. Detect Java Version
        detectJavaVersion(projectDir).ifPresent(requirements::add);

        // 3. Parse dependencies and plugins
        Path pomFile = projectDir.resolve("pom.xml");
        if (Files.exists(pomFile)) {
            try {
                Document doc = parseXml(pomFile);
                requirements.addAll(extractDependencies(doc));
                requirements.addAll(extractPlugins(doc));
            } catch (Exception ignored) {}
        }

        return new ProjectModel(metadata, requirements);
    }

    private ProjectMetadata extractMetadata(Path projectDir) {
        Path pomFile = projectDir.resolve("pom.xml");
        if (!Files.exists(pomFile)) {
            return new ProjectMetadata(null, null, null, null);
        }

        try {
            Document doc = parseXml(pomFile);
            Element root = doc.getDocumentElement();
            String groupId    = getDirectChildTagValue(root, "groupId").orElse(null);
            String artifactId = getDirectChildTagValue(root, "artifactId").orElse(null);
            String version    = getDirectChildTagValue(root, "version").orElse(null);
            String name       = getDirectChildTagValue(root, "name").orElse(artifactId);
            String packaging  = getDirectChildTagValue(root, "packaging").orElse("jar");
            String description= getDirectChildTagValue(root, "description").orElse(null);

            // Multi-module detection
            NodeList modules = doc.getElementsByTagName("module");
            int moduleCount = (modules != null) ? modules.getLength() : 0;

            // Source file count
            long sourceFiles = countSourceFiles(projectDir);

            String descriptionText = buildDescription(description, packaging, moduleCount, sourceFiles);
            return new ProjectMetadata(name != null ? name : artifactId, groupId, version, descriptionText);
        } catch (Exception e) {
            return new ProjectMetadata(null, null, null, null);
        }
    }

    private String buildDescription(String desc, String packaging, int modules, long sourceFiles) {
        StringBuilder sb = new StringBuilder();
        if (desc != null && !desc.isEmpty()) sb.append(desc).append(" | ");
        sb.append("packaging=").append(packaging);
        if (modules > 0) sb.append(" | modules=").append(modules);
        sb.append(" | sources=").append(sourceFiles);
        return sb.toString();
    }

    private long countSourceFiles(Path projectDir) {
        Path srcMain = projectDir.resolve("src/main/java");
        if (!Files.exists(srcMain)) return 0;
        try (var stream = Files.walk(srcMain)) {
            return stream
                    .filter(p -> p.toString().endsWith(".java"))
                    .count();
        } catch (Exception e) {
            return 0;
        }
    }

    private List<DependencyRequirement> extractDependencies(Document doc) {
        List<DependencyRequirement> result = new ArrayList<>();
        NodeList deps = doc.getElementsByTagName("dependency");
        if (deps == null) return result;

        for (int i = 0; i < deps.getLength(); i++) {
            org.w3c.dom.Node node = deps.item(i);
            if (!(node instanceof Element el)) continue;
            if (isInsidePlugin(el)) continue;

            String gId  = getDirectChildTagValue(el, "groupId").orElse("");
            String aId  = getDirectChildTagValue(el, "artifactId").orElse("");
            String ver  = getDirectChildTagValue(el, "version").orElse("*");

            if (gId.isEmpty() || aId.isEmpty()) continue;
            VersionConstraint vc = "*".equals(ver) ? VersionConstraint.any() : VersionConstraint.exact(ver);
            result.add(new DependencyRequirement(gId, aId, vc, RequirementStatus.UNKNOWN));
        }
        return result;
    }

    private boolean isInsidePlugin(Element el) {
        org.w3c.dom.Node parent = el.getParentNode();
        while (parent != null) {
            if (parent instanceof Element pe && pe.getTagName().equals("plugin")) return true;
            parent = parent.getParentNode();
        }
        return false;
    }

    private List<PluginRequirement> extractPlugins(Document doc) {
        List<PluginRequirement> result = new ArrayList<>();
        NodeList plugins = doc.getElementsByTagName("plugin");
        if (plugins == null) return result;

        for (int i = 0; i < plugins.getLength(); i++) {
            org.w3c.dom.Node node = plugins.item(i);
            if (!(node instanceof Element el)) continue;
            String gId = getDirectChildTagValue(el, "groupId").orElse("org.apache.maven.plugins");
            String aId = getDirectChildTagValue(el, "artifactId").orElse("");
            String ver = getDirectChildTagValue(el, "version").orElse("*");
            if (aId.isEmpty()) continue;
            VersionConstraint vc = "*".equals(ver) ? VersionConstraint.any() : VersionConstraint.exact(ver);
            result.add(new PluginRequirement(gId + ":" + aId, vc, RequirementStatus.UNKNOWN));
        }
        return result;
    }

    @Override
    public Optional<JavaRequirement> detectJavaVersion(Path projectDir) {
        Path pomFile = projectDir.resolve("pom.xml");
        if (!Files.exists(pomFile)) return Optional.empty();

        try {
            Document doc = parseXml(pomFile);

            Optional<String> release = getTagValue(doc, "maven.compiler.release");
            if (release.isPresent()) return Optional.of(new JavaRequirement(release.get()));

            Optional<String> source = getTagValue(doc, "maven.compiler.source");
            if (source.isPresent()) return Optional.of(new JavaRequirement(source.get()));

            Optional<String> target = getTagValue(doc, "maven.compiler.target");
            if (target.isPresent()) return Optional.of(new JavaRequirement(target.get()));

        } catch (Exception ignored) {}
        return Optional.empty();
    }

    private Document parseXml(Path xmlFile) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(xmlFile.toFile());
    }

    private Optional<String> getTagValue(Document doc, String tagName) {
        NodeList elements = doc.getElementsByTagName(tagName);
        if (elements != null && elements.getLength() > 0) {
            return Optional.ofNullable(elements.item(0).getTextContent().trim());
        }
        return Optional.empty();
    }

    private Optional<String> getDirectChildTagValue(Element parent, String tagName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            org.w3c.dom.Node node = children.item(i);
            if (node instanceof Element el && el.getTagName().equals(tagName)) {
                return Optional.of(el.getTextContent().trim());
            }
        }
        return Optional.empty();
    }
}
