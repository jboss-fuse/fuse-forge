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

import org.jboss.forge.addon.maven.plugins.ConfigurationElementBuilder;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;

/**
 * Base class for Javadoc exclude commands.
 */
public abstract class AbstractExcludeJavadocCommand extends AbstractAddRegExCommand {

    @Override
    protected String getConfigElementName() {
        return "fromJavadoc";
    }

    @Override
    protected Result doConfigure(ConfigurationElementBuilder configElement) {
        CommandConfig commandConfig = getCommandConfig();
        processExcludeElement(configElement, commandConfig);
        return Results.success(commandConfig.successMessage);
    }

    private void processExcludeElement(ConfigurationElementBuilder configElement, CommandConfig commandConfig) {
        ConfigurationElementBuilder elementBuilder = getOrCreateChild(configElement, commandConfig.elementName);
        elementBuilder.setText(processRegexParam(elementBuilder.getText(), commandConfig.excludeInput));
    }

    protected abstract CommandConfig getCommandConfig();

    // command parameters required by this base class
    class CommandConfig {
        UIInput<String> excludeInput;
        String elementName;
        String successMessage;

        CommandConfig(UIInput<String> excludeInput, String elementName, String successMessage) {
            this.excludeInput = excludeInput;
            this.elementName = elementName;
            this.successMessage = successMessage;
        }
    }
}
