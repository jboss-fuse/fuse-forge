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

public class AddNullableOptionCommand extends AbstractConfigElementCommand {

	@Inject
	@WithAttributes(label = "Nullable Option", required = true, description = "Name of nullable endpoint URI option")
	private UIInput<String> nullableOption;

    @Override
	public UICommandMetadata getMetadata(UIContext context) {
		return Metadata
				.forCommand(AddNullableOptionCommand.class)
				.name("Api Component: Add Nullable  Option")
				.category(Categories.create("Camel"));
	}

	@Override
	public void initializeUI(UIBuilder builder) throws Exception {
		super.initializeUI(builder);
		builder.add(nullableOption);
	}

	@Override
	protected String getConfigElementName() {
		return "nullableOptions";
	}

	@Override
	protected Result doConfigure(ConfigurationElementBuilder configElement) {
		// add nullable option
		getOrCreateChildWithContent(configElement, "nullableOption", nullableOption);
		return Results.success("Command 'Api Component: Add Nullable Option' successfully executed!");
	}
}