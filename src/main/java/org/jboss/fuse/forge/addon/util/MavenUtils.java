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
package org.jboss.fuse.forge.addon.util;

import org.apache.maven.archetype.catalog.Archetype;
import org.jboss.forge.addon.dependencies.Coordinate;
import org.jboss.forge.addon.dependencies.Dependency;
import org.jboss.forge.addon.dependencies.DependencyResolver;
import org.jboss.forge.addon.dependencies.builder.CoordinateBuilder;
import org.jboss.forge.addon.dependencies.builder.DependencyQueryBuilder;
import org.jboss.forge.addon.dependencies.util.CompositeDependencyFilter;
import org.jboss.forge.addon.dependencies.util.NonSnapshotDependencyFilter;
import org.jboss.forge.furnace.util.Predicate;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MavenUtils {

    private static final Pattern REDHAT_VERSION_PATTERN = Pattern.compile("^.*\\.(redhat|fuse)-.*$");
    private static final String[] BANNED_VERSIONS = {"2.2.0.redhat-053", "2.2.0.redhat-066", "2.2.0.redhat-073", "2.2.0.redhat-079"};

    public static List<String> resolveVersions(DependencyResolver resolver, Coordinate coordinate) {
        List<String> bannedVersions = Arrays.asList(BANNED_VERSIONS);
        List<Coordinate> versions = resolveVersions(resolver, coordinate, new NonSnapshotDependencyFilter());
        return versions.stream()
            .map(Coordinate::getVersion)
            .filter(v -> !bannedVersions.contains(v))
            .collect(Collectors.toList());
    }

    public static String resolveLatestVersion(DependencyResolver resolver, Coordinate coordinate) {
        Coordinate result = null;
        List<Coordinate> versions = resolveVersions(resolver, coordinate, new NonSnapshotDependencyFilter());
        if (!versions.isEmpty()) {
            result = versions.get(versions.size() - 1);
        }

        return result != null ? result.getVersion() : "";
    }

    public static String resolveLatestRedhatVersion(DependencyResolver resolver, Coordinate coordinate) {
        List<String> bannedVersions = Arrays.asList(BANNED_VERSIONS);
        Predicate<Dependency> predicate = dependency -> {
            String version = dependency.getCoordinate().getVersion();
            return isRedhatVersion(version) && !bannedVersions.contains(version);
        };

        List<Coordinate> versions = resolveVersions(resolver, coordinate, new NonSnapshotDependencyFilter(), predicate);
        Coordinate result = null;
        if (!versions.isEmpty()) {
            result = versions.get(versions.size() - 1);
        }

        return result != null ? result.getVersion() : "";
    }

    public static boolean isRedhatVersion(String version) {
        return REDHAT_VERSION_PATTERN.matcher(version).matches();
    }

    public static Coordinate createCoordinate(String groupId, String artifactId, String version, String packaging) {
        if (packaging == null) {
            packaging = "jar";
        }

        return CoordinateBuilder.create()
            .setGroupId(groupId)
            .setArtifactId(artifactId)
            .setVersion(version)
            .setPackaging(packaging);
    }

    private static List<Coordinate> resolveVersions(DependencyResolver resolver, Coordinate coordinate, Predicate... filters) {
        DependencyQueryBuilder query = DependencyQueryBuilder.create(coordinate).setFilter(new CompositeDependencyFilter(filters));
        return resolver.resolveVersions(query);
    }

    public static Coordinate createCoordinate(String groupId, String artifactId) {
        return createCoordinate(groupId, artifactId, null, null);
    }

    public static String formatArchetypeGav(Archetype archetype) {
        if (archetype == null) {
            return "";
        }

        return String.format("%s:%s:%s", archetype.getGroupId(), archetype.getArtifactId(), archetype.getVersion());
    }
}
