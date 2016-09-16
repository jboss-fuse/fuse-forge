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

import org.apache.maven.archetype.catalog.Archetype;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.addon.dependencies.DependencyResolver;
import org.jboss.forge.addon.projects.ui.NewProjectWizard;
import org.jboss.forge.addon.ui.controller.WizardCommandController;
import org.jboss.forge.addon.ui.input.UISelectOne;
import org.jboss.forge.addon.ui.test.UITestHarness;
import org.jboss.forge.arquillian.AddonDependencies;
import org.jboss.forge.arquillian.AddonDependency;
import org.jboss.forge.arquillian.archive.AddonArchive;
import org.jboss.forge.furnace.repositories.AddonDependencyEntry;
import org.jboss.fuse.forge.addon.project.FuseProjectType;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import java.io.File;

@RunWith(Arquillian.class)
public class FuseProjectSetupStepTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Inject
    private UITestHarness testHarness;

    @Inject
    private DependencyResolver resolver;

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
        return ShrinkWrap.create(AddonArchive.class)
            .addBeansXML()
            .addAsAddonDependencies(
                AddonDependencyEntry.create("org.jboss.forge.furnace.container:cdi"),
                AddonDependencyEntry.create("org.jboss.forge.addon:ui"),
                AddonDependencyEntry.create("org.jboss.forge.addon:ui-test-harness"),
                AddonDependencyEntry.create("org.jboss.forge.addon:shell-test-harness"),
                AddonDependencyEntry.create("org.jboss.forge.addon:projects"),
                AddonDependencyEntry.create("org.jboss.forge.addon:maven"),
                AddonDependencyEntry.create("org.jboss.fuse.forge.addon:fuse-forge")
            );
    }

    @Test
    public void testNewFuseSpringBootProject() throws Exception {
        testNewFuseProject(FuseProjectCategory.SPRING_BOOT);
    }

    @Test
    public void testNewFuseKarafProject() throws Exception {
        testNewFuseProject(FuseProjectCategory.KARAF);
    }

    @SuppressWarnings("unchecked")
    private void testNewFuseProject(FuseProjectCategory category) throws Exception {
        temporaryFolder.create();
        File tempDir = temporaryFolder.newFolder();

        try (WizardCommandController wizard = testHarness.createWizardController(NewProjectWizard.class)) {
            wizard.initialize();
            Assert.assertFalse(wizard.canMoveToNextStep());
            wizard.setValueFor("named", "fuse-project-test");
            wizard.setValueFor("targetLocation", tempDir);
            wizard.setValueFor("topLevelPackage", "org.fuse.example");
            wizard.setValueFor("type", new FuseProjectType());

            Assert.assertTrue(wizard.canMoveToNextStep());
            WizardCommandController archetypeSelection = wizard.next();

            archetypeSelection.setValueFor("fuseProjectType", category.getName());

            UISelectOne<Archetype> archetypeSelectionInput = (UISelectOne) archetypeSelection.getInput("archetype");
            Iterable<Archetype> archetypes = archetypeSelectionInput.getValueChoices();

            String expectedArchetype = category.equals(FuseProjectCategory.SPRING_BOOT) ? "spring-boot" : "jboss-fuse";

            for (Archetype archetype : archetypes) {
                Assert.assertTrue(archetype.getArtifactId().contains(expectedArchetype));
            }

            archetypeSelectionInput.setValue(archetypes.iterator().next());

            File targetDirectory = new File(tempDir, "fuse-project-test");

            wizard.execute();
            Assert.assertTrue(targetDirectory.exists());
        } finally {
            tempDir.delete();
        }
    }
}
