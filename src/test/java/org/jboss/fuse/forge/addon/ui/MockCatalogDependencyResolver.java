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
package org.jboss.fuse.forge.addon.ui;

import org.jboss.forge.addon.dependencies.Coordinate;
import org.jboss.forge.addon.dependencies.Dependency;
import org.jboss.forge.addon.dependencies.DependencyException;
import org.jboss.forge.addon.dependencies.DependencyMetadata;
import org.jboss.forge.addon.dependencies.DependencyNode;
import org.jboss.forge.addon.dependencies.DependencyQuery;
import org.jboss.forge.addon.dependencies.DependencyResolver;
import org.jboss.forge.addon.facets.Facet;
import org.jboss.forge.addon.resource.AbstractFileResource;
import org.jboss.forge.addon.resource.FileResource;
import org.jboss.forge.addon.resource.Resource;
import org.jboss.forge.addon.resource.ResourceFactory;
import org.jboss.fuse.forge.addon.util.MavenUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.jboss.fuse.forge.addon.ui.FuseProjectSetupStep.ARCHETYPE_CATALOG_ARTIFACT_ID;
import static org.jboss.fuse.forge.addon.ui.FuseProjectSetupStep.ARCHETYPE_CATALOG_GROUP_ID;

@Singleton
public class MockCatalogDependencyResolver implements DependencyResolver {

    private Coordinate catalogCoordinate = MavenUtils.createCoordinate(ARCHETYPE_CATALOG_GROUP_ID,
            ARCHETYPE_CATALOG_ARTIFACT_ID, "1.0.0", "jar");

    @Inject
    ResourceFactory resourceFactory;

    @Override
    public Dependency resolveArtifact(DependencyQuery dependencyQuery) {
        return createMockCatalogDependency();
    }

    @Override
    public Set<Dependency> resolveDependencies(DependencyQuery dependencyQuery) {
        return Collections.emptySet();
    }

    @Override
    public DependencyNode resolveDependencyHierarchy(DependencyQuery dependencyQuery) {
        return null;
    }

    @Override
    public List<Coordinate> resolveVersions(DependencyQuery dependencyQuery) {
        List<Coordinate> versions = new ArrayList<>();
        versions.add(catalogCoordinate);
        return versions;
    }

    @Override
    public DependencyMetadata resolveDependencyMetadata(DependencyQuery dependencyQuery) {
        return null;
    }

    private Dependency createMockCatalogDependency() {
        return new Dependency() {
            @Override
            public Coordinate getCoordinate() {
                return catalogCoordinate;
            }

            @Override
            public String getScopeType() {
                return "compile";
            }

            @Override
            public FileResource<?> getArtifact() throws DependencyException {
                return new AbstractFileResource(resourceFactory, new File("target/archetypes-catalog.jar")) {

                    @Override
                    public boolean supports(Facet facet) {
                        return false;
                    }

                    @Override
                    public Resource createFrom(Object o) {
                        return null;
                    }

                    @Override
                    protected List<Resource<?>> doListResources() {
                        return null;
                    }

                    @Override
                    public Resource<File> createFrom(File file) {
                        return null;
                    }
                };
            }

            @Override
            public boolean isOptional() {
                return false;
            }

            @Override
            public List<Coordinate> getExcludedCoordinates() {
                return Collections.emptyList();
            }
        };
    }
}
