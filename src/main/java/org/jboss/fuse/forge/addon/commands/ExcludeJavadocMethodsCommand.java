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

import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;

public class ExcludeJavadocMethodsCommand extends AbstractExcludeJavadocCommand {

    @Inject
    @WithAttributes(label = "Exclude Javadoc Methods RegEx", required = true, description = "Regular expression for methods to exclude when processing Javadoc")
    private UIInput<String> excludeMethods;

    @Override
	public UICommandMetadata getMetadata(UIContext context) {
		return Metadata
				.forCommand(ExcludeJavadocMethodsCommand.class)
				.name("Api Component: Exclude Javadoc Methods")
				.category(Categories.create("Camel"));
	}

	@Override
	public void initializeUI(UIBuilder builder) throws Exception {
		super.initializeUI(builder);
        builder.add(excludeMethods);
	}

    @Override
    protected CommandConfig getCommandConfig() {
        return new CommandConfig(excludeMethods, "excludeMethods", "Command 'Api Component: Exclude Config Names' successfully executed!");
    }
}