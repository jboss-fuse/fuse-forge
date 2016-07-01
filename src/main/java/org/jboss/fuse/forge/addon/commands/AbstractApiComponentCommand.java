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

import java.util.Optional;
import javax.inject.Inject;

import org.jboss.forge.addon.dependencies.builder.CoordinateBuilder;
import org.jboss.forge.addon.facets.FacetFactory;
import org.jboss.forge.addon.maven.plugins.ConfigurationBuilder;
import org.jboss.forge.addon.maven.plugins.ConfigurationElement;
import org.jboss.forge.addon.maven.plugins.ConfigurationElementBuilder;
import org.jboss.forge.addon.maven.plugins.MavenPluginBuilder;
import org.jboss.forge.addon.maven.plugins.MavenPluginInstaller;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFacet;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.Projects;
import org.jboss.forge.addon.projects.ui.AbstractProjectCommand;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.input.UIInput;

/**
 * Base class for Api Component commands.
 */
public abstract class AbstractApiComponentCommand extends AbstractProjectCommand {

    @Inject
    protected MavenPluginInstaller mavenPluginInstaller;

    @Inject
    private ProjectFactory projectFactory;
    @Inject
    private FacetFactory facetFactory;

    protected boolean isCamelComponentProject(Project project) {
		return isPluginInstalled(project, CoordinateBuilder.create("org.apache.camel:camel-api-component-maven-plugin"));
	}

    @Override
    protected boolean isProjectRequired() {
        return true;
    }

    @Override
    protected ProjectFactory getProjectFactory() {
        return projectFactory;
    }

    protected Project getSelectedProjectOrNull(UIContext context) {
        return Projects.getSelectedProject(this.getProjectFactory(), context);
    }

    protected boolean isPluginInstalled(Project project, CoordinateBuilder coordinateBuilder) {
        return mavenPluginInstaller.isInstalled(project, MavenPluginBuilder.create()
            .setCoordinate(coordinateBuilder));
    }

    protected  <T extends ProjectFacet> T getOrInstallFacet(Project project, Class<T> facetClass) {
        Optional<T> facetAsOptional = project.getFacetAsOptional(facetClass);
        final T facet;
        if (facetAsOptional.isPresent()) {
            facet = facetAsOptional.get();
        } else {
            facet = facetFactory.install(project, facetClass);
        }
        return facet;
    }

    ConfigurationElementBuilder getOrCreateChildWithContent(ConfigurationElementBuilder parent, String elementName, String content) {
        return getOrCreateChild(parent, elementName).setText(content);
    }

    <T> ConfigurationElementBuilder getOrCreateChildWithContent(ConfigurationElementBuilder parent, String elementName, UIInput<T> input) {
        String content = input.getValue().toString();
        return getOrCreateChildWithContent(parent, elementName, content);
    }

    <T> ConfigurationElementBuilder getOrCreateChildIfNotNullContent(ConfigurationElementBuilder parent, String elementName, UIInput<T> input) {
        if (input.getValue() != null) {
            return getOrCreateChildWithContent(parent, elementName, input);
        }
        return null;
    }

    ConfigurationElementBuilder createFromExisting(ConfigurationElement element) {
        if (element instanceof ConfigurationElementBuilder) {
            // avoid creating an out of sync in memory wrapper
            return (ConfigurationElementBuilder) element;
        } else {
            return ConfigurationElementBuilder.createFromExisting(element);
        }
    }

    ConfigurationElementBuilder getOrCreateChild(ConfigurationElement parent, String child) {
        return parent.hasChildByName(child, true) ?
            createFromExisting(parent.getChildByName(child, true))
            : createFromExisting(parent).createConfigurationElement(child);
    }

    ConfigurationElementBuilder getOrCreateChild(ConfigurationBuilder configurationBuilder, String child) {
        ConfigurationElementBuilder result;
        if (configurationBuilder.hasConfigurationElement(child)) {
            result = createFromExisting(configurationBuilder.getConfigurationElement(child));
        } else {
            result = configurationBuilder.createConfigurationElement(child);
        }
        return result;
    }
}
