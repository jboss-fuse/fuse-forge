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

import org.jboss.forge.addon.dependencies.DependencyResolver;
import org.jboss.forge.addon.maven.plugins.ConfigurationElementBuilder;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.fuse.forge.addon.completer.DependencyClassCompleter;

public class AddExtraOptionCommand extends AbstractConfigElementCommand {

	@Inject
	@WithAttributes(label = "Extra Option Name", required = true, description = "Extra endpoint URI option name")
	private UIInput<String> name;

	@Inject
	@WithAttributes(label = "Extra Option Type", required = true, description = "Extra endpoint URI option Java type")
	private UIInput<String> type;

    @Inject
    private DependencyResolver dependencyResolver;

    @Override
	public UICommandMetadata getMetadata(UIContext context) {
		return Metadata
				.forCommand(AddExtraOptionCommand.class)
				.name("Api Component: Add Extra Option")
				.category(Categories.create("Camel"));
	}

	@Override
	public void initializeUI(UIBuilder builder) throws Exception {
		super.initializeUI(builder);
        type.setCompleter(new DependencyClassCompleter(dependencyResolver, getSelectedProject(builder)));
		builder.add(name)
			.add(type);
	}

	@Override
	protected String getConfigElementName() {
		return "extraOptions";
	}

	@Override
	protected Result doConfigure(ConfigurationElementBuilder configElement) {
		// add extra option
		ConfigurationElementBuilder extraOption = configElement.createConfigurationElement("extraOption");
        // type could have '<>' for generic types, so wrap with CDATA if needed
        String typeValue = type.getValue();
        // TODO this attempt to handle escaped XML characters fails, since re-parsing decodes it and barfs when re-writing
        if (typeValue.indexOf('<') != -1) {
            typeValue = "<![CDATA[" + typeValue + "]]>";
        }
        extraOption.createConfigurationElement("type").setText(typeValue);
		extraOption.createConfigurationElement("name").setText(name.getValue());

		return Results.success("Command 'Api Component: Add Extra Option' successfully executed!");
	}
}