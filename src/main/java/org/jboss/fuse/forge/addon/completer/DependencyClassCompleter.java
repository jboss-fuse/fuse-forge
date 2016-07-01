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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jboss.forge.addon.dependencies.Coordinate;
import org.jboss.forge.addon.dependencies.Dependency;
import org.jboss.forge.addon.dependencies.DependencyResolver;
import org.jboss.forge.addon.dependencies.builder.DependencyQueryBuilder;
import org.jboss.forge.addon.maven.projects.facets.MavenDependencyFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.input.InputComponent;
import org.jboss.forge.addon.ui.input.UICompleter;

/**
 * Completer for API Proxy class.
 */
public class DependencyClassCompleter implements UICompleter<String> {

    private static final Pattern EXCLUDED_GROUPS = Pattern.compile("(org.apache.camel)|(org.slf4j)|(org.apache.log4j)");

    private final List<String> classNames;

    public DependencyClassCompleter(DependencyResolver dependencyResolver, Project project) throws IOException {

        classNames = new ArrayList<>();

        List<Dependency> dependencies = project.getFacet(MavenDependencyFacet.class).getManagedDependencies();
        for (Dependency dependency : dependencies) {
            Coordinate coordinate = dependency.getCoordinate();
            // exclude camel, slf4j and log4j dependencies
            if (!EXCLUDED_GROUPS.matcher(coordinate.getGroupId()).find()) {
                DependencyQueryBuilder queryBuilder = DependencyQueryBuilder.create(coordinate);
                JarFile jarFile = new JarFile(dependencyResolver.resolveArtifact(queryBuilder).getArtifact().getFullyQualifiedName());
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                        classNames.add(entry.getName().replace("/", ".").replace(".class", ""));
                    }
                }
            }
        }

/*
        URL[] urLs = urlClassLoader.getURLs();
        for (URL urL : urLs) {
            String file = urL.getFile();
            if (file != null && file.endsWith(".jar")) {
                // open the jar and add list of classes
                if (!"jar".equals(urL.getProtocol())) {
                    urL = new URL("jar:" + urL.toString());
                }

                JarURLConnection jarURLConnection = (JarURLConnection) urL.openConnection();
                Enumeration<JarEntry> entries = jarURLConnection.getJarFile().entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                        classNames.add(entry.getName().replace("/", ".").replace(".class", ""));
                    }
                }
            }
        }
*/
    }

    @Override
    public Iterable<String> getCompletionProposals(UIContext context, InputComponent<?, String> input, String value) {
        return classNames.stream().filter(name -> value == null || name.startsWith(value)).collect(Collectors.toList());
    }
}
