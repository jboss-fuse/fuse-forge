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

import org.jboss.forge.addon.maven.plugins.ConfigurationBuilder;
import org.jboss.forge.addon.maven.plugins.ConfigurationElement;
import org.jboss.forge.addon.maven.plugins.ConfigurationElementBuilder;
import org.jboss.forge.addon.maven.plugins.ExecutionBuilder;
import org.jboss.forge.addon.maven.plugins.MavenPluginBuilder;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.Failed;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.fuse.forge.addon.completer.ApiNameCompleter;

/**
 * Base class for commands based on a configuraion element that can be set globally.
 */
public abstract class AbstractConfigElementCommand extends AbstractConfigCommand {

    @Inject
    @WithAttributes(label = "API Name", description = "API name where configuration will be added, and will apply to ALL APIs if left empty")
    protected UIInput<String> apiName;

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        Project project = getSelectedProject(builder.getUIContext());
        apiName.setCompleter(new ApiNameCompleter(getApiElements(project)));

        builder.add(apiName);
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {

        Project project = getSelectedProject(context);

        // is apiName provided for non-global settings

        // get location to insert based on global flag
        MavenPluginBuilder pluginBuilder = getPluginBuilder(project);
        ConfigurationBuilder configurationBuilder = getConfigurationBuilder(pluginBuilder);
        ConfigurationElementBuilder parentElement = getConfigElement(getConfigElementName(), pluginBuilder, configurationBuilder);
        if (parentElement == null) {
            return Results.fail("Missing API name " + apiName.getValue());
        }

        Result result = doConfigure(parentElement);
        if (!(result instanceof Failed)) {
            // update plugin config
            updatePlugin(project, pluginBuilder);
        }
        return result;
    }

    protected abstract String getConfigElementName();

    protected abstract Result doConfigure(ConfigurationElementBuilder configElement);

    private ConfigurationElementBuilder getConfigElement(String elementName, MavenPluginBuilder pluginBuilder, ConfigurationBuilder configurationBuilder) {
        ConfigurationElement apis = getElementBuilder(configurationBuilder);

        ConfigurationElementBuilder parentElement;
        if (isGlobal()) {
            parentElement = getOrCreateChild(configurationBuilder, elementName);
            // NOTE: this is needed to make sure the configuration builder updates the execution
            // since configuration builders don't handle updating execution configurations correctly
            ((ExecutionBuilder)pluginBuilder.listExecutions().get(0)).setConfig(configurationBuilder);
        } else {
            parentElement = getConfigElementBuilder(apis, elementName, apiName);
        }
        return parentElement;
    }

    private boolean isGlobal() {
        return apiName.getValue() == null;
    }
}
