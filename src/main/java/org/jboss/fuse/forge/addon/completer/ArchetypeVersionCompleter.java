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

import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.input.InputComponent;
import org.jboss.forge.addon.ui.input.UICompleter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ArchetypeVersionCompleter implements UICompleter<String> {

    private List<String> archetypeVersions = new ArrayList<>();

    public ArchetypeVersionCompleter(List<String> archetypeVersions) {
        this.archetypeVersions = archetypeVersions;
    }

    @Override
    public Iterable<String> getCompletionProposals(UIContext context, InputComponent<?, String> input, String value) {
        return archetypeVersions.stream()
            .filter(a -> a.startsWith(value))
            .sorted((o1, o2) -> o1.compareTo(o2))
            .collect(Collectors.toList());
    }
}
