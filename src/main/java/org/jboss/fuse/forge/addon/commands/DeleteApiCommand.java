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

import javax.inject.Inject;

import org.jboss.forge.addon.maven.plugins.ConfigurationBuilder;
import org.jboss.forge.addon.maven.plugins.ConfigurationElementBuilder;
import org.jboss.forge.addon.maven.plugins.MavenPluginBuilder;
import org.jboss.forge.addon.projects.Project;
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

public class DeleteApiCommand extends AbstractConfigCommand {

	@Inject
	@WithAttributes(label = "API Name", required = true, description = "Endpoint URI Prefix for API")
	private UIInput<String> apiName;

	@Override
	public UICommandMetadata getMetadata(UIContext context) {
		return Metadata.forCommand(DeleteApiCommand.class)
				.name("Api Component: Add Api")
				.category(Categories.create("Camel"));
	}

	@Override
	public void initializeUI(UIBuilder builder) throws Exception {
        getSelectedProject(builder.getUIContext());
        builder.add(apiName);
	}

    @Override
	public Result execute(UIExecutionContext context) throws Exception {

        String name = apiName.getValue();

        Project project = getSelectedProject(context);
        MavenPluginBuilder pluginBuilder = getPluginBuilder(project);
        ConfigurationBuilder configurationBuilder = getConfigurationBuilder(pluginBuilder);
        ConfigurationElementBuilder apis = getElementBuilder(configurationBuilder);

        ConfigurationElementBuilder apiElement;
        if ((apiElement = getApiElement(name, apis)) == null) {
            return Results.fail("No API found with name " + name);
        }

        // TODO should have user confirm before destroying the whole API sub-tree
        ConfigurationElementBuilder parentElement = apiElement.getParentElement();
        parentElement.getChildren().remove(apiElement);

        updatePlugin(project, pluginBuilder);

        return Results.success("Command 'Api Component: Add Api' successfully executed!");
	}

}