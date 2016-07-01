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

public class AddSubstitutionCommand extends AbstractConfigElementCommand {

    @Inject
    @WithAttributes(label = "Method RegEx", required = true, description = "Regular expression for method name", defaultValue = "^.+$")
    private UIInput<String> method;

    @Inject
    @WithAttributes(label = "Argument Name RegEx", required = true, description = "Regular expression for argument name")
    private UIInput<String> argName;

    @Inject
    @WithAttributes(label = "Argument Type", required = false, description = "Argument Java type")
    private UIInput<String> argType;

    @Inject
    @WithAttributes(label = "Replacement Expression", required = true, description = "Expression to replace matching argument name")
    private UIInput<String> replacement;

    @Inject
    @WithAttributes(label = "Replace With Type", required = false, description = "Replace matching argument using type name expression, default is false")
    private UIInput<Boolean> replaceWithType;
    
    @Override
	public UICommandMetadata getMetadata(UIContext context) {
		return Metadata
				.forCommand(AddSubstitutionCommand.class)
				.name("Api Component: Add Substitution")
				.category(Categories.create("Camel"));
	}

	@Override
	public void initializeUI(UIBuilder builder) throws Exception {
		super.initializeUI(builder);
        builder.add(method)
            .add(argName)
            .add(argType)
            .add(replacement)
            .add(replaceWithType);
	}

	@Override
	protected String getConfigElementName() {
		return "substitutions";
	}

	@Override
	protected Result doConfigure(ConfigurationElementBuilder configElement) {
        // add substitution
        ConfigurationElementBuilder substitution = configElement.createConfigurationElement("substitution");
        getOrCreateChildWithContent(substitution, "method", method);
        getOrCreateChildWithContent(substitution, "argName", argName);
        getOrCreateChildIfNotNullContent(substitution, "argType", argType);
        getOrCreateChildWithContent(substitution, "replacement", replacement);
        getOrCreateChildIfNotNullContent(substitution, "replaceWithType", replaceWithType);

        return Results.success("Command 'Api Component: Add Substitution' successfully executed!");
	}

}