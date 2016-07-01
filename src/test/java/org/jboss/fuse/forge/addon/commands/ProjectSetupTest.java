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

import java.io.File;
import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.ui.command.UICommand;
import org.jboss.forge.addon.ui.controller.CommandController;
import org.jboss.forge.addon.ui.result.Failed;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.test.UITestHarness;
import org.jboss.forge.arquillian.AddonDependencies;
import org.jboss.forge.arquillian.AddonDependency;
import org.jboss.forge.arquillian.archive.AddonArchive;
import org.jboss.forge.furnace.addons.AddonRegistry;
import org.jboss.forge.furnace.repositories.AddonDependencyEntry;
import org.jboss.forge.furnace.util.OperatingSystemUtils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class ProjectSetupTest {

    private static final String[] EXCLUDE_NAMES = new String[]{
        "excludeConfigNames",
        "excludeConfigTypes",
        "excludeClasses",
        "excludeMethods",
        "excludePackages"
    };

    @SuppressWarnings("unchecked")
    private static final Class<? extends UICommand>[] EXCLUDE_COMMANDS = (Class<? extends UICommand>[]) new Class<?>[]{
        ExcludeConfigNamesCommand.class,
        ExcludeConfigTypesCommand.class,
        ExcludeJavadocClassesCommand.class,
        ExcludeJavadocMethodsCommand.class,
        ExcludeJavadocPackagesCommand.class,
    };

    @Inject
    private UITestHarness testHarness;

    @Inject
    private ProjectFactory projectFactory;

    @Inject
    private AddonRegistry registry;

    @Deployment
    @AddonDependencies({
        @AddonDependency(name = "org.jboss.forge.addon:maven"),
        @AddonDependency(name = "org.jboss.forge.addon:projects"),
        @AddonDependency(name = "org.jboss.forge.addon:ui"),
        @AddonDependency(name = "org.jboss.forge.addon:ui-test-harness"),
        @AddonDependency(name = "org.jboss.forge.addon:shell-test-harness"),
        @AddonDependency(name = "org.jboss.fuse.forge.addon:fuse-forge")
    })
    public static AddonArchive getDeployment() {
        return ShrinkWrap.create(AddonArchive.class).addBeansXML().addAsAddonDependencies(
            AddonDependencyEntry.create("org.jboss.forge.furnace.container:cdi"),
            AddonDependencyEntry.create("org.jboss.forge.addon:ui"),
            AddonDependencyEntry.create("org.jboss.forge.addon:ui-test-harness"),
            AddonDependencyEntry.create("org.jboss.forge.addon:shell-test-harness"),
            AddonDependencyEntry.create("org.jboss.forge.addon:projects"),
            AddonDependencyEntry.create("org.jboss.forge.addon:maven"),
            AddonDependencyEntry.create("org.jboss.fuse.forge.addon:fuse-forge"));
    }

    @Test
    public void testAddons() throws Exception {
        File tempDir = OperatingSystemUtils.createTempDir();
        try {
            Project project = projectFactory.createTempProject();
            Assert.assertNotNull("Should have created a project", project);

            // test project setup
            testProjectSetup(project);

            // test add api
            testAddApi(project);

            // test alias
            testAddAlias(project, null);
            testAddAlias(project, "testApi");

            // test extra option
            testExtraOption(project, null);
            testExtraOption(project, "testApi");

            // test nullable option
            testNullableOption(project, null);
            testNullableOption(project, "testApi");

            // test substitution
            testSubstitution(project, null);
            testSubstitution(project, "testApi");

            // test various excludes
            for (int i = 0; i < EXCLUDE_COMMANDS.length; i++) {
                testExclude(project, null, EXCLUDE_NAMES[i], EXCLUDE_COMMANDS[i], 0);
                testExclude(project, null, EXCLUDE_NAMES[i], EXCLUDE_COMMANDS[i], 1);

                testExclude(project, "testApi", EXCLUDE_NAMES[i], EXCLUDE_COMMANDS[i], 0);
                testExclude(project, "testApi", EXCLUDE_NAMES[i], EXCLUDE_COMMANDS[i], 1);
            }

            // test includeStaticMethods for Javadoc
            testIncludeStaticMethods(project, null);
            testIncludeStaticMethods(project, "testApi");

            // test fromJavadoc
            testAddFromJavadoc(project, null);
            testAddFromJavadoc(project, "testApi");

            // test fromSignatureFile
            testAddFromSignatureFile(project);

            // test fromSignatureFile
            testDeleteApi(project);

            System.out.print("Success!");
        } finally {
            tempDir.delete();
        }
    }

    private void testDeleteApi(Project project) throws Exception {
        CommandController command = testHarness.createCommandController(DeleteApiCommand.class, project.getRoot());
        command.initialize();
        command.setValueFor("apiName", "testApi");

        Result result = command.execute();
        Assert.assertFalse("Should delete API config from the project", result instanceof Failed);

        String message = result.getMessage();
        System.out.println(message);
    }

    private void testAddFromSignatureFile(Project project) throws Exception {
        CommandController command = testHarness.createCommandController(AddFromSignatureFileCommand.class, project.getRoot());
        command.initialize();
        command.setValueFor("apiName", "testApi");
        command.setValueFor("fromSignatureFile", "signatures/file-sig-api.txt");

        Result result = command.execute();
        Assert.assertFalse("Should add fromSignatureFile to the project", result instanceof Failed);

        String message = result.getMessage();
        System.out.println(message);
    }

    private void testAddFromJavadoc(Project project, String apiName) throws Exception {
        CommandController command = testHarness.createCommandController(AddFromJavadocCommand.class, project.getRoot());
        command.initialize();
        command.setValueFor("apiName", apiName);
        command.setValueFor("excludePackages", "excludePackagesPattern");
        command.setValueFor("excludeClasses", "excludeClassesPattern");
        command.setValueFor("excludeMethods", "excludeMethodsPattern");
        command.setValueFor("includeStaticMethods", "true");

        Result result = command.execute();
        Assert.assertFalse("Should add fromJavadoc to the project", result instanceof Failed);

        String message = result.getMessage();
        System.out.println(message);
    }

    private void testIncludeStaticMethods(Project project, String apiName) throws Exception {
        CommandController command = testHarness.createCommandController(IncludeJavadocStaticMethodsCommand.class, project.getRoot());
        command.initialize();
        command.setValueFor("apiName", apiName);
        command.setValueFor("includeStaticMethods", "true");

        Result result = command.execute();
        Assert.assertFalse("Should add includeStaticMethods to the project", result instanceof Failed);

        String message = result.getMessage();
        System.out.println(message);
    }

    private void testExclude(Project project, String apiName, String excludeConfig, Class<? extends UICommand> excludeCommand, int callCount) throws Exception {
        CommandController command = testHarness.createCommandController(excludeCommand, project.getRoot());
        command.initialize();
        command.setValueFor("apiName", apiName);
        command.setValueFor(excludeConfig, excludeConfig + "Value" + callCount);

        Result result = command.execute();
        Assert.assertFalse("Should add " + excludeConfig + " to the project", result instanceof Failed);

        String message = result.getMessage();
        System.out.println(message);
    }

    private void testSubstitution(Project project, String apiName) throws Exception {
        CommandController command = testHarness.createCommandController(AddSubstitutionCommand.class, project.getRoot());
        command.initialize();
        command.setValueFor("apiName", apiName);
        command.setValueFor("argName", "^.+$");
        command.setValueFor("argType", "java.lang.String");
        command.setValueFor("replacement", "$1Param");

        Result result = command.execute();
        Assert.assertFalse("Should add substitution to the project", result instanceof Failed);

        String message = result.getMessage();
        System.out.println(message);
    }

    private void testExtraOption(Project project, String apiName) throws Exception {
        CommandController command = testHarness.createCommandController(AddExtraOptionCommand.class, project.getRoot());
        command.initialize();
        command.setValueFor("apiName", apiName);
        command.setValueFor("name", "customOption");
        // TODO fix support for parameterized types
        // might have to to a global configuration re-escape after loading/before writing
//        command.setValueFor("type", "java.util.List<String>");
        command.setValueFor("type", "java.lang.String");

        Result result = command.execute();
        Assert.assertFalse("Should add extra option to the project", result instanceof Failed);

        String message = result.getMessage();
        System.out.println(message);
    }

    private void testNullableOption(Project project, String apiName) throws Exception {
        CommandController command = testHarness.createCommandController(AddNullableOptionCommand.class, project.getRoot());
        command.initialize();
        command.setValueFor("apiName", apiName);
        command.setValueFor("nullableOption", "nullableOptionName");

        Result result = command.execute();
        Assert.assertFalse("Should add nullable option to the project", result instanceof Failed);

        String message = result.getMessage();
        System.out.println(message);
    }

    private void testAddAlias(Project project, String apiName) throws Exception {
        CommandController command = testHarness.createCommandController(AddAliasCommand.class, project.getRoot());
        command.initialize();
        command.setValueFor("apiName", apiName);
        command.setValueFor("methodPattern", "[gs]et(.+)");
        command.setValueFor("methodAlias", "$1");

        Result result = command.execute();
        Assert.assertFalse("Should add API alias to the project", result instanceof Failed);

        String message = result.getMessage();
        System.out.println(message);
    }

    private void testAddApi(Project project) throws Exception {
        CommandController command = testHarness.createCommandController(AddApiCommand.class, project.getRoot());
        command.initialize();
        command.setValueFor("apiName", "testApi");
        command.setValueFor("proxyClass", "org.test.api.MyProxy");

        Result result = command.execute();
        Assert.assertFalse("Should add API config to the project", result instanceof Failed);

        String message = result.getMessage();
        System.out.println(message);
    }

    private void testProjectSetup(Project project) throws Exception {
        CommandController command = testHarness.createCommandController(ProjectSetupCommand.class, project.getRoot());
        command.initialize();
        command.setValueFor("name", "ApiTest");
        command.setValueFor("scheme", "apitest");
        command.setValueFor("packageName", "org.apache.camel.component.apitest");

        Result result = command.execute();
        Assert.assertFalse("Should setup Camel API Component plugins in the project", result instanceof Failed);

        String message = result.getMessage();
        System.out.println(message);
    }
}