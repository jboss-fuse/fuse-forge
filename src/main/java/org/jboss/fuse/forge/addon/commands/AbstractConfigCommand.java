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

import java.util.ArrayList;
import java.util.List;

import org.jboss.forge.addon.dependencies.builder.CoordinateBuilder;
import org.jboss.forge.addon.maven.plugins.Configuration;
import org.jboss.forge.addon.maven.plugins.ConfigurationBuilder;
import org.jboss.forge.addon.maven.plugins.ConfigurationElement;
import org.jboss.forge.addon.maven.plugins.ConfigurationElementBuilder;
import org.jboss.forge.addon.maven.plugins.Execution;
import org.jboss.forge.addon.maven.plugins.MavenPlugin;
import org.jboss.forge.addon.maven.plugins.MavenPluginBuilder;
import org.jboss.forge.addon.maven.plugins.PluginElement;
import org.jboss.forge.addon.maven.projects.MavenPluginFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.input.UIInput;

/**
 * Base class for API Component configuration commands.
 */
public abstract class AbstractConfigCommand extends AbstractApiComponentCommand {

    @Override
    public boolean isEnabled(UIContext context) {
        Project project = getSelectedProjectOrNull(context);
        // only enable if we do not have Camel yet
        // must have a project and the project must have camel api component plugin already
        return project != null && isCamelComponentProject(project);
    }

    List<ConfigurationElement> getApiElements(ConfigurationElement apis) {
        List<ConfigurationElement> apiNames = new ArrayList<>();
        for (PluginElement element : apis.getChildren()) {
            ConfigurationElement child = (ConfigurationElement) element;
            if ("api".equals(child.getName()) && child.hasChildByName("apiName", true)) {
                apiNames.add(child);
            }
        }
        return apiNames;
    }

    ConfigurationElementBuilder getElementBuilder(ConfigurationBuilder configurationBuilder) {
        return getOrCreateChild(configurationBuilder, "apis");
    }

    ConfigurationBuilder getConfigurationBuilder(MavenPluginBuilder mavenPlugin) {
        // NOTE: assuming there's only one execution and config
        Configuration config = mavenPlugin.listExecutions().get(0).getConfig();
        return ConfigurationBuilder.create(config, getPluginBuilderFromPlugin(mavenPlugin));
    }

    MavenPluginBuilder getPluginBuilder(Project project) {
        MavenPluginFacet mavenPluginFacet = project.getFacet(MavenPluginFacet.class);
        MavenPlugin plugin = mavenPluginFacet.getPlugin(CoordinateBuilder.create("org.apache.camel:camel-api-component-maven-plugin"));
        return getPluginBuilderFromPlugin(plugin);
    }

    private MavenPluginBuilder getPluginBuilderFromPlugin(MavenPlugin plugin) {
        MavenPluginBuilder pluginBuilder = MavenPluginBuilder.create(plugin);
        // TODO there is a bug in the copy ctor of MavenPluginImpl that ignores executions
        List<Execution> executions = pluginBuilder.listExecutions();
        executions.clear();
        executions.addAll(plugin.listExecutions());
        return pluginBuilder;
    }

    ConfigurationElementBuilder getConfigElementBuilder(ConfigurationElement apis, String configElement, UIInput<String> apiName) {

        ConfigurationElementBuilder parentElement = null;
        List<ConfigurationElement> apiElements = getApiElements(apis);
        for (ConfigurationElement api : apiElements) {
            if (apiName.getValue().equals(api.getChildByName("apiName", true).getText())) {
                parentElement = getOrCreateChild(api, configElement);
            }
        }

        return parentElement;
    }

    void updatePlugin(Project project, MavenPluginBuilder pluginBuilder) {
        project.getFacet(MavenPluginFacet.class).updatePlugin(pluginBuilder);
    }

    protected ConfigurationElementBuilder getApiElement(String name, ConfigurationElement apis) {
        List<ConfigurationElement> apiElements = getApiElements(apis);
        for (ConfigurationElement apiElement : apiElements) {
            if (name.equals(apiElement.getChildByName("apiName").getText())) {
                return createFromExisting(apiElement);
            }
        }
        return null;
    }

    protected List<ConfigurationElement> getApiElements(Project project) {
        return getApiElements(getElementBuilder(getConfigurationBuilder(getPluginBuilder(project))));
    }
}
