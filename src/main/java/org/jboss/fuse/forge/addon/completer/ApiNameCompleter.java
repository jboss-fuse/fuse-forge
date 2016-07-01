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
package org.jboss.fuse.forge.addon.completer;

import java.util.ArrayList;
import java.util.List;

import org.jboss.forge.addon.maven.plugins.ConfigurationElement;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.input.InputComponent;
import org.jboss.forge.addon.ui.input.UICompleter;

/**
 * Completer for API Names.
 */
public class ApiNameCompleter implements UICompleter<String> {
    private final List<String> apiNames;

    public ApiNameCompleter(List<ConfigurationElement> apis) {
        apiNames = new ArrayList<>();
        for (ConfigurationElement api : apis) {
            apiNames.add(api.getChildByName("apiName").getText());
        }
    }

    @Override
    public Iterable<String> getCompletionProposals(UIContext context, InputComponent<?, String> input, String value) {
        List<String> answer = new ArrayList<>();
        for (String name : apiNames) {
            if (value == null || name.startsWith(value)) {
                answer.add(name);
            }
        }
        return answer;
    }
}
