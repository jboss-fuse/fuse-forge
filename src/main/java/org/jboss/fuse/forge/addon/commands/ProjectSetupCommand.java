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
package org.jboss.fuse.forge.addon.commands;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

import org.apache.maven.model.Model;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.model.Reporting;
import org.jboss.forge.addon.dependencies.Coordinate;
import org.jboss.forge.addon.dependencies.Dependency;
import org.jboss.forge.addon.dependencies.builder.CoordinateBuilder;
import org.jboss.forge.addon.dependencies.builder.DependencyBuilder;
import org.jboss.forge.addon.maven.plugins.ConfigurationBuilder;
import org.jboss.forge.addon.maven.plugins.ConfigurationElementBuilder;
import org.jboss.forge.addon.maven.plugins.ExecutionBuilder;
import org.jboss.forge.addon.maven.plugins.MavenPluginAdapter;
import org.jboss.forge.addon.maven.plugins.MavenPluginBuilder;
import org.jboss.forge.addon.maven.projects.MavenFacet;
import org.jboss.forge.addon.maven.projects.MavenPluginFacet;
import org.jboss.forge.addon.maven.resources.MavenModelResource;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.dependencies.DependencyInstaller;
import org.jboss.forge.addon.projects.facets.DependencyFacet;
import org.jboss.forge.addon.projects.facets.ResourcesFacet;
import org.jboss.forge.addon.resource.FileResource;
import org.jboss.forge.addon.resource.Resource;
import org.jboss.forge.addon.resource.ResourceFactory;
import org.jboss.forge.addon.resource.URLResource;
import org.jboss.forge.addon.templates.Template;
import org.jboss.forge.addon.templates.TemplateFactory;
import org.jboss.forge.addon.templates.freemarker.FreemarkerTemplate;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.JavaSource;

public class ProjectSetupCommand extends AbstractApiComponentCommand {

    // TODO determine appropriate log4j version instead of hardcoded value
    private static final String LOG4J_VERSION = "1.2.17";
    private static final String JAVA_VERSION = "1.7";
    private static final String UTF_8 = "UTF-8";
    private static final String COMPONENT_RESOURCE_PATH = "META-INF/services/org/apache/camel/component".replace('/', File.separatorChar);

    @Inject
	@WithAttributes(label = "Camel Version", required = false, description = "Camel version to use. If none provided then the latest version will be used.")
	private UIInput<String> version;

	@Inject
	@WithAttributes(label = "Component Name", required = true, description = "Component name, e.g. 'LinkedIn'.")
	private UIInput<String> name;

	@Inject
	@WithAttributes(label = "Component Scheme", required = true, description = "Component scheme name, .e.g 'linkedin'.")
	private UIInput<String> scheme;

	@Inject
	@WithAttributes(label = "Component Package", required = true, description = "Component Java package, e.g. 'org.apache.camel.component.linkedin'.")
	private UIInput<String> packageName;

	@Inject
	private DependencyInstaller dependencyInstaller;

    @Inject
    private ResourceFactory resourceFactory;

    @Inject
    private TemplateFactory templateFactory;

    private String camelVersion;
    private String slf4jVersion;

    @Override
	public boolean isEnabled(UIContext context) {
        Project project = getSelectedProjectOrNull(context);
        // only enable if we do not have Camel yet
        // must have a project
        // and the project must not have camel api component plugin already
        return project != null && !isCamelComponentProject(project);
    }

	@Override
	public UICommandMetadata getMetadata(UIContext context) {
		return Metadata.forCommand(ProjectSetupCommand.class)
				.name("Api Component: Setup")
				.category(Categories.create("Camel"));
	}

	@Override
	public void initializeUI(UIBuilder builder) throws Exception {
		builder.add(version)
            .add(name)
            .add(scheme)
            .add(packageName);
	}

	@Override
	public Result execute(UIExecutionContext context) throws Exception {
        Project project = getSelectedProject(context);
        if (isCamelComponentProject(project)) {
            return Results.success("Api Component is already setup!");
        }

        // configure maven project
        configureProject(project);

        // copy resource templates
        createResources(project);

        return Results.success("Command 'Api Component: Setup' successfully executed!");
	}

    private void configureProject(Project project) {
        // set component properties
        setProjectProperties(project);

        // add dependencies
        addDependencies(project);

        // add managed plugin
        addManagedPlugin(project);

        // add plugins
        addPlugins(project);

        // add reporting
        addReporting(project);
    }

    private void setProjectProperties(Project project) {
        MavenModelResource modelResource = project.getFacet(MavenFacet.class).getModelResource();
        Model model = modelResource.getCurrentModel();

        if (model.getName() == null) {
            model.setName("Camel " + name.getValue() + " Component");
        }
        if (model.getDescription() == null) {
            model.setDescription("Camel Component for " + name.getValue());
        }
        model.setPackaging("bundle");

        model.addProperty("schemeName", scheme.getValue());
        model.addProperty("componentName", name.getValue());
        model.addProperty("componentPackage", packageName.getValue());
        model.addProperty("outPackage", packageName.getValue() + ".internal");
        model.addProperty("project.build.sourceEncoding", UTF_8);
        model.addProperty("project.build.outputEncoding", UTF_8);

        modelResource.setCurrentModel(model);
    }

    private void addDependencies(Project project) {
        // version set?
        String value = version.getValue();
        if (value == null) {
            value = "";
        } else {
            value = ":" + value;
        }
        dependencyInstaller.install(project, DependencyBuilder.create("org.apache.camel:camel-core" + value));

        // get camel and slf4j versions
        List<Dependency> effectiveDependencies = project.getFacet(DependencyFacet.class).getEffectiveDependencies();
        for (Dependency dependency : effectiveDependencies) {
            Coordinate coordinate = dependency.getCoordinate();
            if ("org.apache.camel".equals(coordinate.getGroupId()) && "camel-core".equals(coordinate.getArtifactId())) {
                camelVersion = coordinate.getVersion();
            } else if ("org.slf4j".equals(coordinate.getGroupId()) && "slf4j-api".equals(coordinate.getArtifactId())) {
                slf4jVersion = coordinate.getVersion();
            }
        }

        // install dependencies
        dependencyInstaller.install(project, DependencyBuilder.create("org.apache.camel:apt:" + camelVersion));
        dependencyInstaller.install(project, DependencyBuilder.create("org.apache.camel:spi-annotations:" + camelVersion + ":provided"));
        dependencyInstaller.install(project, DependencyBuilder.create("org.slf4j:slf4j-api:" + slf4jVersion));
        dependencyInstaller.install(project, DependencyBuilder.create("org.slf4j:slf4j-log4j12:" + slf4jVersion + ":test"));
        dependencyInstaller.install(project, DependencyBuilder.create("log4j:log4j:" + LOG4J_VERSION + ":test"));
        dependencyInstaller.install(project, DependencyBuilder.create("org.apache.camel:camel-test:" + camelVersion + ":test"));
    }

    private void addManagedPlugin(Project project) {
        // api component maven plugin
        MavenPluginBuilder plugin = MavenPluginBuilder.create()
            .setCoordinate(CoordinateBuilder.create("org.apache.camel:camel-api-component-maven-plugin:" + camelVersion));
        ConfigurationBuilder configuration = plugin.createConfiguration();
        configuration.createConfigurationElement("scheme").setText("${schemeName}");
        configuration.createConfigurationElement("componentName").setText("${componentName}");
        configuration.createConfigurationElement("componentPackage").setText("${componentPackage}");
        configuration.createConfigurationElement("outPackage").setText("${outPackage}");

        mavenPluginInstaller.installManaged(project, plugin);
    }

    private void addPlugins(Project project) {

        // compiler plugin
        if (!isPluginInstalled(project, CoordinateBuilder.create("org.apache.maven.plugins:maven-compiler-plugin"))) {

            MavenPluginBuilder compilerPlugin = MavenPluginBuilder.create()
                .setCoordinate(CoordinateBuilder.create("org.apache.maven.plugins:maven-compiler-plugin:3.5.1"));
            ConfigurationBuilder configuration = compilerPlugin.createConfiguration();
            configuration.createConfigurationElement("source").setText(JAVA_VERSION);
            configuration.createConfigurationElement("target").setText(JAVA_VERSION);
            mavenPluginInstaller.install(project, compilerPlugin);
        }

        // resources plugin
        if (!isPluginInstalled(project, CoordinateBuilder.create("org.apache.maven.plugins:maven-resources-plugin"))) {

            MavenPluginBuilder resourcesPlugin = MavenPluginBuilder.create()
                .setCoordinate(CoordinateBuilder.create("org.apache.maven.plugins:maven-resources-plugin:2.6"));
            ConfigurationBuilder configuration = resourcesPlugin.createConfiguration();
            configuration.createConfigurationElement("encoding").setText(UTF_8);
            mavenPluginInstaller.install(project, resourcesPlugin);
        }

        // bundle plugin
        if (!isPluginInstalled(project, CoordinateBuilder.create("org.apache.felix:maven-bundle-plugin"))) {

            MavenPluginBuilder bundlePlugin = MavenPluginBuilder.create()
                .setCoordinate(CoordinateBuilder.create("org.apache.felix:maven-bundle-plugin:2.3.7"))
                .setExtensions(true);
            ConfigurationBuilder configuration = bundlePlugin.createConfiguration();
            ConfigurationElementBuilder instructions = configuration.createConfigurationElement("instructions");
            instructions.addChild("Bundle-Name").setText("Camel Component for ${componentName}");
            instructions.addChild("Bundle-SymbolicName").setText("${project.groupId}.${project.artifactId}");
            instructions.addChild("Export-Service").setText("org.apache.camel.spi.ComponentResolver;component=${schemeName}");
            instructions.addChild("Export-Package").setText("${componentPackage};version=${project.version}");
            instructions.addChild("Import-Package").setText("${componentPackage}.api;version=${project.version}," +
                "${componentPackage};version=${project.version}," +
                "org.apache.camel.*;version=${camel-version}");
            instructions.addChild("Private-Package").setText("${outPackage}");
            instructions.addChild("Implementation-Title").setText("Apache Camel");
            instructions.addChild("Implementation-Version").setText("${project.version}");
            instructions.addChild("Karaf-Info").setText("Camel;${project.artifactId}=${project.version}");
            instructions.addChild("_versionpolicy").setText("[$(version;==;$(@)),$(version;+;$(@)))");
            instructions.addChild("_failok").setText("false");
            mavenPluginInstaller.install(project, bundlePlugin);
        }

        // camel-api-component plugin
        MavenPluginBuilder camelApiPlugin = MavenPluginBuilder.create()
            .setCoordinate(CoordinateBuilder.create("org.apache.camel:camel-api-component-maven-plugin:" + camelVersion));
        ConfigurationBuilder configuration = ConfigurationBuilder.create(camelApiPlugin);
        configuration.createConfigurationElement("apis");
        camelApiPlugin.addExecution(ExecutionBuilder.create()
            .setId("generate-component-classes")
            .addGoal("fromApis")
            .setConfig(configuration));
        mavenPluginInstaller.install(project, camelApiPlugin);

        // build-helper plugin
        MavenPluginBuilder buildHelperPlugin = MavenPluginBuilder.create()
            .setCoordinate(CoordinateBuilder.create("org.codehaus.mojo:build-helper-maven-plugin:1.10"));
        // add generated component sources
        configuration = ConfigurationBuilder.create(buildHelperPlugin);
        configuration.createConfigurationElement("sources")
            .addChild("source").setText("${project.build.directory}/generated-sources/camel-component");
        buildHelperPlugin.addExecution(ExecutionBuilder.create()
            .setId("add-generated-sources")
            .addGoal("add-source")
            .setConfig(configuration));
        // add generated component test sources
        configuration = ConfigurationBuilder.create(buildHelperPlugin);
        configuration.createConfigurationElement("sources")
            .addChild("source").setText("${project.build.directory}/generated-test-sources/camel-component");
        buildHelperPlugin.addExecution(ExecutionBuilder.create()
            .setId("add-generated-test-sources")
            .addGoal("add-test-source")
            .setConfig(configuration));
        mavenPluginInstaller.install(project, buildHelperPlugin);

        // camel-package plugin
        MavenPluginBuilder camelPackagePlugin = MavenPluginBuilder.create()
            .setCoordinate(CoordinateBuilder.create("org.apache.camel:camel-package-maven-plugin:" + camelVersion));
        camelPackagePlugin.addExecution(ExecutionBuilder.create()
            .setId("prepare")
            .addGoal("prepare-components")
            .setPhase("generate-resources"));
        camelPackagePlugin.addExecution(ExecutionBuilder.create()
            .setId("validate")
            .addGoal("validate-components")
            .setPhase("prepare-package"));
        mavenPluginInstaller.install(project, camelPackagePlugin);
    }

    private void addReporting(Project project) {

        // add camel-api-component plugin for reporting
        MavenModelResource modelResource = project.getFacet(MavenFacet.class).getModelResource();
        Model currentModel = modelResource.getCurrentModel();
        // copy managed plugin configuration
        MavenPluginAdapter mavenPluginAdapter = new MavenPluginAdapter(project.getFacet(MavenPluginFacet.class).getManagedPlugin(
            CoordinateBuilder.create("org.apache.camel:camel-api-component-maven-plugin:" + camelVersion)));
        ReportPlugin reportPlugin = new ReportPlugin();
        reportPlugin.setGroupId(mavenPluginAdapter.getGroupId());
        reportPlugin.setArtifactId(mavenPluginAdapter.getArtifactId());
        reportPlugin.setVersion(mavenPluginAdapter.getVersion());
        reportPlugin.setConfiguration(mavenPluginAdapter.getConfiguration());

        Reporting reporting = new Reporting();
        reporting.addPlugin(reportPlugin);
        currentModel.setReporting(reporting);
        modelResource.setCurrentModel(currentModel);
    }

    private void createResources(Project project) throws IOException {

        // java sources
        reifyJavaResource(project, "/templates/Abstract__name__TestSupport.ftl", true);

        reifyJavaResource(project, "/templates/__name__Component.ftl", false);
        reifyJavaResource(project, "/templates/__name__Configuration.ftl", false);
        reifyJavaResource(project, "/templates/__name__Consumer.ftl", false);
        reifyJavaResource(project, "/templates/__name__Endpoint.ftl", false);
        reifyJavaResource(project, "/templates/__name__Producer.ftl", false);

        reifyJavaResource(project, "/templates/__name__Constants.ftl", false);
        reifyJavaResource(project, "/templates/__name__PropertiesHelper.ftl", false);

        // resources
        reifyResource(project, "/templates/__scheme__", COMPONENT_RESOURCE_PATH + File.separator + scheme.getValue(), false);
        reifyResource(project, "/templates/log4j.properties", "/log4j.properties", true);
        reifyResource(project, "/templates/test-options.properties", "/test-options.properties", true);
    }

    private void reifyJavaResource(Project project, String resourceTemplate, boolean testResource) throws IOException {

        JavaSourceFacet javaSourceFacet = getOrInstallFacet(project, JavaSourceFacet.class);
        String output = readResource(resourceTemplate);

        JavaSource<?> javaSource = Roaster.parse(JavaSource.class, output);
        if (!testResource) {
            javaSourceFacet.saveJavaSourceUnformatted(javaSource);
        } else {
            javaSourceFacet.saveTestJavaSourceUnformatted(javaSource);
        }
    }

    private void reifyResource(Project project, String resourceTemplate, String fileName, boolean testResource) throws IOException {

        ResourcesFacet resourcesFacet = getOrInstallFacet(project, ResourcesFacet.class);
        String output = readResource(resourceTemplate);

        FileResource<?> fileResource;
        if (!testResource) {
            fileResource = resourcesFacet.getResource(fileName);
        } else {
            fileResource = resourcesFacet.getTestResource(fileName);
        }
        fileResource.createNewFile();
        fileResource.setContents(output);
    }

    private String readResource(String resourceTemplate) throws IOException {
        Resource<URL> urlResource = resourceFactory.create(getClass().getResource(resourceTemplate)).reify(URLResource.class);
        Template template = templateFactory.create(urlResource, FreemarkerTemplate.class);

        // any dynamic options goes into the params map
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("name", name.getValue());
        params.put("scheme", scheme.getValue());
        params.put("package", packageName.getValue());
        return template.process(params);
    }

}