/**
 *  Copyright 2005-2016 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package org.jboss.fuse.forge.addon.ui;

import org.apache.maven.archetype.catalog.Archetype;
import org.apache.maven.archetype.catalog.ArchetypeCatalog;
import org.apache.maven.archetype.catalog.io.xpp3.ArchetypeCatalogXpp3Reader;
import org.jboss.forge.addon.dependencies.Coordinate;
import org.jboss.forge.addon.dependencies.Dependency;
import org.jboss.forge.addon.dependencies.DependencyRepository;
import org.jboss.forge.addon.dependencies.DependencyResolver;
import org.jboss.forge.addon.dependencies.builder.DependencyQueryBuilder;
import org.jboss.forge.addon.maven.projects.archetype.ArchetypeHelper;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.facets.MetadataFacet;
import org.jboss.forge.addon.resource.DirectoryResource;
import org.jboss.forge.addon.resource.FileResource;
import org.jboss.forge.addon.ui.command.AbstractUICommand;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.context.UINavigationContext;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.input.UISelectOne;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.NavigationResult;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.forge.addon.ui.wizard.UIWizardStep;
import org.jboss.forge.furnace.util.Strings;
import org.jboss.fuse.forge.addon.completer.ArchetypeVersionCompleter;
import org.jboss.fuse.forge.addon.util.MavenUtils;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;

import static org.jboss.fuse.forge.addon.ui.FuseProjectCategory.KARAF;
import static org.jboss.fuse.forge.addon.ui.FuseProjectCategory.SPRING_BOOT;

public class FuseProjectSetupStep extends AbstractUICommand implements UIWizardStep {

    public static final String ARCHETYPE_CATALOG_GROUP_ID = "io.fabric8.archetypes";
    public static final String ARCHETYPE_CATALOG_ARTIFACT_ID = "archetypes-catalog";
    private static final Logger LOG = Logger.getLogger(FuseProjectSetupStep.class.getName());

    @Inject
    private DependencyResolver resolver;

    @Inject
    @WithAttributes(label = "Project type", required = false, description = "The type of project to create")
    private UISelectOne<String> fuseProjectType;

    @Inject
    @WithAttributes(label = "Archetype catalog version", required = false, description = "The archetype catalog version")
    private UIInput<String> catalogVersion;

    @Inject
    @WithAttributes(label = "Archetype", required = true, description = "The archetype to generate the new project from")
    private UISelectOne<Archetype> archetype;

    private List<String> archetypeVersions;
    private String latestCatalogVersion;

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.from(super.getMetadata(context), getClass())
            .name("JBoss Fuse: Choose Archetype")
            .description("Choose a new JBoss Fuse archetype to generate your project");
    }

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        Coordinate coordinate = MavenUtils.createCoordinate(ARCHETYPE_CATALOG_GROUP_ID, ARCHETYPE_CATALOG_ARTIFACT_ID);
        archetypeVersions = MavenUtils.resolveVersions(resolver, coordinate);
        if (archetypeVersions.isEmpty()) {
            throw new IllegalStateException(String.format("Missing archetype catalog %s:%s, please add JBoss Fuse Maven repository to your Maven configuration",
                ARCHETYPE_CATALOG_GROUP_ID, ARCHETYPE_CATALOG_ARTIFACT_ID));
        }

        configureProjectTypeInput();
        configureCatalogVersionInput();
        configureArchetypeInput();

        builder.add(catalogVersion).add(fuseProjectType).add(archetype);
    }

    @Override
    public NavigationResult next(UINavigationContext context) throws Exception {
        return null;
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        Archetype selectedArchetype = archetype.getValue();
        String repository = selectedArchetype.getRepository();
        String coordinate = String.format("%s:%s:%s", selectedArchetype.getGroupId(), selectedArchetype.getArtifactId(),
            selectedArchetype.getVersion());
        DependencyQueryBuilder depQuery = DependencyQueryBuilder.create(coordinate);

        if (!Strings.isNullOrEmpty(repository)) {
            if (repository.endsWith(".xml")) {
                int lastRepositoryPath = repository.lastIndexOf('/');
                if (lastRepositoryPath > -1) {
                    repository = repository.substring(0, lastRepositoryPath);
                }
            }
            if (!repository.isEmpty()) {
                depQuery.setRepositories(new DependencyRepository("archetype", repository));
            }
        }

        Dependency resolvedArtifact = resolver.resolveArtifact(depQuery);
        FileResource<?> artifact = resolvedArtifact.getArtifact();

        UIContext uiContext = context.getUIContext();
        Project project = (Project) uiContext.getAttributeMap().get(Project.class);
        MetadataFacet metadataFacet = project.getFacet(MetadataFacet.class);
        File fileRoot = project.getRoot().reify(DirectoryResource.class).getUnderlyingResourceObject();

        ArchetypeHelper archetypeHelper = new ArchetypeHelper(artifact.getResourceInputStream(), fileRoot, metadataFacet.getProjectGroupName(),
            metadataFacet.getProjectName(), metadataFacet.getProjectVersion()) {

            // See: OSFUSE-349, until  super.removeInvalidHeaderCommentsAndProcessVelocityMacros actually
            // does full velocity processing, patch support for the `#set( $H = '##' )` velocity macro
            // that the archetypes are using.
            @Override
            protected String removeInvalidHeaderCommentsAndProcessVelocityMacros(String text) {
                String answer = "";
                for (String line : text.split("\r?\n")) {
                    String l = line.trim();
                    if (l.startsWith("##") || l.startsWith("#set(")) {
                        continue; // skip over the preprocessor controls..
                    }
                    if (line.contains("${D}")) {
                        line = line.replaceAll("\\$\\{D\\}", "\\$");
                    }
                    if (line.contains("${H}")) {
                        line = line.replaceAll("\\$\\{H\\}", "##");
                    }
                    answer = answer + line + "\n";
                }
                return answer;
            }
        };

        JavaSourceFacet facet = project.getFacet(JavaSourceFacet.class);
        archetypeHelper.setPackageName(facet.getBasePackage());
        archetypeHelper.execute();

        return Results.success("Created new JBoss Fuse project in: " + fileRoot.getPath());
    }

    private void configureProjectTypeInput() {
        Set<String> types = Stream.of(FuseProjectCategory.values())
            .map(FuseProjectCategory::getName)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        fuseProjectType.setValueChoices(types);
        fuseProjectType.addValueChangeListener(valueChangeEvent -> {
            String newValue = String.valueOf(valueChangeEvent.getNewValue()).trim();
            archetype.setValueChoices(() -> {
                Set<Archetype> result = new LinkedHashSet<>();
                if (isValidVersion(catalogVersion.getValue())) {
                    ArchetypeCatalog archetypeCatalog = getCatalog();
                    if (archetypeCatalog != null) {
                        List<Archetype> archetypes = archetypeCatalog.getArchetypes();
                        result.addAll(archetypes.stream()
                            .filter(archetype -> isValidArchetype(archetype, newValue))
                            .collect(Collectors.toList()));
                    }
                }
                return result;
            });
        });
    }

    private void configureCatalogVersionInput() {
        catalogVersion.setRequired(false)
            .setDefaultValue(getLatestCatalogVersion())
            .setCompleter(new ArchetypeVersionCompleter(archetypeVersions));

        catalogVersion.addValueChangeListener(valueChangeEvent -> {
            String version = String.valueOf(valueChangeEvent.getNewValue());
            archetype.setValueChoices(() -> {
                Set<Archetype> result = new LinkedHashSet<>();
                if (isValidVersion(version)) {
                    ArchetypeCatalog archetypeCatalog = getCatalog();
                    if (archetypeCatalog != null) {
                        List<Archetype> archetypes = archetypeCatalog.getArchetypes();
                        result.addAll(archetypes.stream()
                            .filter(archetype -> isValidArchetype(archetype, fuseProjectType.getValue()))
                            .collect(Collectors.toList()));
                    }
                }
                return result;
            });
        });
    }

    private void configureArchetypeInput() {
        archetype.setItemLabelConverter(source -> {
            if (source == null) {
                return null;
            }
            return source.getDescription() != null ? source.getDescription() : MavenUtils.formatArchetypeGav(source);
        }).setDescription(() -> {
            Archetype value = archetype.getValue();
            return value == null ? null : MavenUtils.formatArchetypeGav(value);
        });

        ArchetypeCatalog archetypeCatalog = getCatalog();
        if (archetypeCatalog != null) {
            Set<Archetype> result = new LinkedHashSet<>();
            List<Archetype> archetypes = archetypeCatalog.getArchetypes();
            result.addAll(archetypes.stream()
                    .filter(archetype -> isValidArchetype(archetype, null))
                    .collect(Collectors.toList()));
            archetype.setValueChoices(result);
        }
    }

    private boolean isValidArchetype(Archetype archetype, String projectType) {
        String groupIdMatch = MavenUtils.isRedhatVersion(archetype.getVersion()) ? "org.jboss.fuse.fis.archetypes" : "io.fabric8.archetypes";
        if (archetype.getGroupId().equals(groupIdMatch)) {
            if (Strings.isNullOrEmpty(projectType)) {
                return true;
            }

            String artifactId = archetype.getArtifactId();
            if (projectType.equals(KARAF.getName()) && artifactId.startsWith(KARAF.getArtifactIdPrefix())) {
                return true;
            }

            if (projectType.equals(SPRING_BOOT.getName()) && artifactId.contains(SPRING_BOOT.getArtifactIdPrefix())) {
                return true;
            }
        }
        return false;
    }

    private boolean isValidVersion(String version) {
        return !Strings.isNullOrEmpty(version) && archetypeVersions.contains(version);
    }

    private ArchetypeCatalog getCatalog() {
        Coordinate coordinate = MavenUtils.createCoordinate(ARCHETYPE_CATALOG_GROUP_ID, ARCHETYPE_CATALOG_ARTIFACT_ID,
                catalogVersion.getValue(), "jar");

        Dependency dependency = resolver.resolveArtifact(DependencyQueryBuilder.create(coordinate));
        if (dependency != null) {
            try {
                String name = dependency.getArtifact().getFullyQualifiedName();
                URL url = new URL("file", null, name);
                try(URLClassLoader classLoader = new URLClassLoader(new URL[]{url})) {
                    InputStream inputStream = classLoader.getResourceAsStream("archetype-catalog.xml");
                    if (inputStream != null) {
                        return new ArchetypeCatalogXpp3Reader().read(inputStream);
                    }
                }
            } catch (Exception e) {
                LOG.warning("Unable to resolve archetype catalog: " + e.getMessage());
            }
        }
        return null;
    }

    private String getLatestCatalogVersion() {
        if (Strings.isNullOrEmpty(latestCatalogVersion)) {
            Stream<String> stream = archetypeVersions.stream()
                .filter(v -> !MavenUtils.isRedhatVersion(v));

            latestCatalogVersion = archetypeVersions.stream()
                .filter(MavenUtils::isRedhatVersion)
                .reduce((a, b) -> b)
                .orElse(stream.reduce((a,b) -> b).orElseThrow(IllegalStateException::new));
        }
        return latestCatalogVersion;
    }
}
