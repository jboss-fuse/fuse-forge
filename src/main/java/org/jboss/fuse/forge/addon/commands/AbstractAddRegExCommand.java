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

import java.util.regex.Pattern;
import javax.inject.Inject;

import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.metadata.WithAttributes;

/**
 * Base class for commands to add a RegEx.
 */
public abstract class AbstractAddRegExCommand extends AbstractConfigElementCommand {

    private static final Pattern GROUPED_REGEX = Pattern.compile("\\(.*\\)");

    @Inject
    @WithAttributes(label = "Append RegEx", required = false, description = "Append regular expression as a new group to existing expression, true by default", defaultValue = "true")
    UIInput<Boolean> append;

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        super.initializeUI(builder);
        builder.add(append);
    }

    String processRegexParam(String oldRegEx, UIInput<String> uiInput) {
        String newRegex;
        // handle append
        if (shouldAppend() && oldRegEx != null) {
            StringBuilder builder = new StringBuilder();
            if (GROUPED_REGEX.matcher(oldRegEx).matches()) {
                builder.append(oldRegEx);
            } else {
                builder.append('(').append(oldRegEx).append(')');
            }
            builder.append("|(").append(uiInput.getValue()).append(")");
            newRegex = builder.toString();
        } else {
            newRegex = uiInput.getValue();
        }
        return newRegex;
    }

    private boolean shouldAppend() {
        return append.getValue() != null && append.getValue();
    }
}
