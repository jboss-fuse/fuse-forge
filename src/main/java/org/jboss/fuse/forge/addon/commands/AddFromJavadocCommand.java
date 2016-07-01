/**
 * Copyright 2005-2016 Red Hat, Inc.
 * <p>
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
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

public class AddFromJavadocCommand extends AbstractConfigElementCommand {

    @Inject
    @WithAttributes(label = "Exclude Packages RegEx", required = false, description = "Regular expression for Packages to exclude when processing Javadoc")
    private UIInput<String> excludePackages;

    @Inject
    @WithAttributes(label = "Exclude Classes RegEx", required = false, description = "Regular expression for Classes to exclude when processing Javadoc")
    private UIInput<String> excludeClasses;

    @Inject
    @WithAttributes(label = "Exclude Methods RegEx", required = false, description = "Regular expression for Methods to exclude when processing Javadoc")
    private UIInput<String> excludeMethods;

    @Inject
    @WithAttributes(label = "Include Static Methods", required = false, description = "Include static methods when processing Javadoc")
    private UIInput<Boolean> includeStaticMethods;

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(AddFromJavadocCommand.class)
            .name("Api Component: Add Config to Generate From Javadoc")
            .category(Categories.create("Camel"));
    }

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        super.initializeUI(builder);
        builder.add(excludePackages)
            .add(excludeClasses)
            .add(excludeMethods)
            .add(includeStaticMethods);
    }

    @Override
    protected String getConfigElementName() {
        return "fromJavadoc";
    }

    @Override
    protected Result doConfigure(ConfigurationElementBuilder configElement) {
        // add fromJavadoc config
        getOrCreateChildIfNotNullContent(configElement, "excludePackages", excludePackages);
        getOrCreateChildIfNotNullContent(configElement, "excludeClasses", excludeClasses);
        getOrCreateChildIfNotNullContent(configElement, "excludeMethods", excludeMethods);
        getOrCreateChildIfNotNullContent(configElement, "includeStaticMethods", includeStaticMethods);

        return Results.success("Command 'Api Component: Add Config to Generate From Javadoc' successfully executed!");
    }
}