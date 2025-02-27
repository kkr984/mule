/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

import static org.mule.runtime.deployment.model.api.application.ApplicationStatus.CREATED;
import static org.mule.runtime.deployment.model.api.application.ApplicationStatus.STARTED;
import static org.mule.runtime.deployment.model.api.application.ApplicationStatus.STOPPED;
import static org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor.DEFAULT_DOMAIN_NAME;
import static org.mule.runtime.module.deployment.internal.util.DeploymentServiceTestUtils.redeployDomain;
import static org.mule.test.allure.AllureConstants.ArtifactDeploymentFeature.DOMAIN_DEPLOYMENT;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.application.ApplicationStatus;
import org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.deployment.api.DeploymentListener;
import org.mule.runtime.module.deployment.impl.internal.builder.ApplicationFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.DomainFileBuilder;

import io.qameta.allure.Feature;
import org.junit.Test;

@Feature(DOMAIN_DEPLOYMENT)
public class ApplicationDependingOnDomainDeploymentTestCase extends AbstractDeploymentTestCase {

  private final DomainFileBuilder emptyDomain100FileBuilder =
      new DomainFileBuilder("empty-domain").definedBy("empty-domain-config.xml").withVersion("1.0.0");
  private final DomainFileBuilder emptyDomain101FileBuilder =
      new DomainFileBuilder("empty-domain").definedBy("empty-domain-config.xml").withVersion("1.0.1");

  private final ApplicationFileBuilder appDependingOnDomain100FileBuilder = new ApplicationFileBuilder("dummy-domain100-app")
      .definedBy("empty-config.xml").dependingOn(emptyDomain100FileBuilder);
  private final ApplicationFileBuilder appDependingOnDomain101FileBuilder = new ApplicationFileBuilder("dummy-domain101-app")
      .definedBy("empty-config.xml").dependingOn(emptyDomain101FileBuilder);

  private final ApplicationFileBuilder appReferencingDomain101FileBuilder = new ApplicationFileBuilder("dummy-domain101-app-ref")
      .definedBy("empty-config.xml").dependingOn(emptyDomain100FileBuilder)
      .deployedWith("domain", "empty-domain-1.0.1-mule-domain");
  private final ApplicationFileBuilder appReferencingDomain100FileBuilder = new ApplicationFileBuilder("dummy-domain100-app-ref")
      .definedBy("empty-config.xml").dependingOn(emptyDomain100FileBuilder)
      .deployedWith("domain", "empty-domain-1.0.0-mule-domain");

  private final ApplicationFileBuilder appReferencingDefaultDomainFileBuilder = new ApplicationFileBuilder("app-with-default-ref")
      .definedBy("empty-config.xml").deployedWith("domain", "default");

  private final ApplicationFileBuilder defaultDomainAppFileBuilder = new ApplicationFileBuilder("default-domain-app")
      .definedBy("empty-config.xml");

  private final ApplicationFileBuilder incompatibleDomainNameAppFileBuilder = new ApplicationFileBuilder("bad-domain-app-ref")
      .definedBy("empty-config.xml").dependingOn(emptyDomain101FileBuilder)
      .deployedWith("domain", "empty-domain-1.0.0-mule-domain");

  private final ApplicationFileBuilder appWithDomainNameButMissingBundleDescriptor =
      new ApplicationFileBuilder("dummy-domain101-app-ref")
          .definedBy("empty-config.xml").deployedWith("domain", "empty-domain-1.0.1-mule-domain");

  public ApplicationDependingOnDomainDeploymentTestCase(boolean parallelDeployment) {
    super(parallelDeployment);
  }

  @Test
  public void domainNotFound() throws Exception {
    startDeployment();

    // By GAV
    addExplodedAppFromBuilder(appDependingOnDomain100FileBuilder, appDependingOnDomain100FileBuilder.getId());
    assertDeploymentFailure(applicationDeploymentListener, appDependingOnDomain100FileBuilder.getId());

    // By name
    addExplodedAppFromBuilder(appReferencingDomain101FileBuilder, appReferencingDomain101FileBuilder.getId());
    assertDeploymentFailure(applicationDeploymentListener, appReferencingDomain101FileBuilder.getId());
  }

  @Test
  public void referenceToDomainByGAV() throws Exception {
    startDeployment();

    // Add domain 1.0.0
    addExplodedDomainFromBuilder(emptyDomain100FileBuilder, emptyDomain100FileBuilder.getId());
    assertDeploymentSuccess(domainDeploymentListener, emptyDomain100FileBuilder.getId());

    // Add app pointing to 1.0.0
    addExplodedAppFromBuilder(appDependingOnDomain100FileBuilder, appDependingOnDomain100FileBuilder.getId());
    assertDeploymentSuccess(applicationDeploymentListener, appDependingOnDomain100FileBuilder.getId());
  }

  @Test
  public void referenceToCompatibleDomainByGAV() throws Exception {
    startDeployment();

    // Add domain with version upgraded (1.0.1)
    addExplodedDomainFromBuilder(emptyDomain101FileBuilder, emptyDomain101FileBuilder.getId());
    assertDeploymentSuccess(domainDeploymentListener, emptyDomain101FileBuilder.getId());

    // Add app pointing to 1.0.0 or compatible
    addExplodedAppFromBuilder(appDependingOnDomain100FileBuilder, appDependingOnDomain100FileBuilder.getId());
    assertDeploymentSuccess(applicationDeploymentListener, appDependingOnDomain100FileBuilder.getId());
  }

  @Test
  public void referenceDomainByName() throws Exception {
    startDeployment();

    addExplodedDomainFromBuilder(emptyDomain101FileBuilder, emptyDomain101FileBuilder.getId());
    assertDeploymentSuccess(domainDeploymentListener, emptyDomain101FileBuilder.getId());

    reset(applicationDeploymentListener);

    addExplodedAppFromBuilder(appReferencingDomain101FileBuilder, appReferencingDomain101FileBuilder.getId());
    assertDeploymentSuccess(applicationDeploymentListener, appReferencingDomain101FileBuilder.getId());
  }

  @Test
  public void referenceDefaultDomainByName() throws Exception {
    startDeployment();

    addExplodedAppFromBuilder(appReferencingDefaultDomainFileBuilder, appReferencingDefaultDomainFileBuilder.getId());
    assertDeploymentSuccess(applicationDeploymentListener, appReferencingDefaultDomainFileBuilder.getId());
  }

  @Test
  public void failsWhenSpecifiedNameIsNotFoundEvenWhenCompatibleIsPresent() throws Exception {
    startDeployment();

    // Add domain with version upgraded (1.0.1)
    addExplodedDomainFromBuilder(emptyDomain101FileBuilder, emptyDomain101FileBuilder.getId());
    assertDeploymentSuccess(domainDeploymentListener, emptyDomain101FileBuilder.getId());

    // Add app pointing to 1.0.0 by name
    addExplodedAppFromBuilder(appReferencingDomain100FileBuilder, appReferencingDomain100FileBuilder.getId());
    assertDeploymentFailure(applicationDeploymentListener, appReferencingDomain100FileBuilder.getId());
  }

  @Test
  public void appDeploymentFailsIfMultipleCompatibleDomainsAreDeployed() throws Exception {
    startDeployment();

    // Deploy two compatible domains (1.0.0 and 1.0.1)
    addExplodedDomainFromBuilder(emptyDomain100FileBuilder, emptyDomain100FileBuilder.getId());
    assertDeploymentSuccess(domainDeploymentListener, emptyDomain100FileBuilder.getId());
    addExplodedDomainFromBuilder(emptyDomain101FileBuilder, emptyDomain101FileBuilder.getId());
    assertDeploymentSuccess(domainDeploymentListener, emptyDomain101FileBuilder.getId());

    // Following application depends on domain 1.0.0, and both domains 1.0.0 and 1.0.1 are deployed, so
    // the deployment fails because the domain reference is ambiguous
    addExplodedAppFromBuilder(appDependingOnDomain100FileBuilder, appDependingOnDomain100FileBuilder.getId());
    assertDeploymentFailure(applicationDeploymentListener, appDependingOnDomain100FileBuilder.getId());

    reset(applicationDeploymentListener);

    // This application depends on domain 1.0.1, which is not considered compatible with 1.0.1, so this deployment is ok
    addExplodedAppFromBuilder(appDependingOnDomain101FileBuilder, appDependingOnDomain101FileBuilder.getId());
    assertDeploymentSuccess(applicationDeploymentListener, appDependingOnDomain101FileBuilder.getId());
  }

  @Test
  public void appPointingToIncompatibleDomain() throws Exception {
    startDeployment();

    // Deploy both versions to ensure that there is one compatible domain and a name-matching domain
    addExplodedDomainFromBuilder(emptyDomain100FileBuilder, emptyDomain100FileBuilder.getId());
    assertDeploymentSuccess(domainDeploymentListener, emptyDomain100FileBuilder.getId());
    addExplodedDomainFromBuilder(emptyDomain101FileBuilder, emptyDomain101FileBuilder.getId());
    assertDeploymentSuccess(domainDeploymentListener, emptyDomain101FileBuilder.getId());

    // The app depends on 1.0.1 but references the domain 1.0.0 by name, so it must fail
    addExplodedAppFromBuilder(incompatibleDomainNameAppFileBuilder, incompatibleDomainNameAppFileBuilder.getId());
    assertDeploymentFailure(applicationDeploymentListener, incompatibleDomainNameAppFileBuilder.getId());
  }

  @Test
  public void failToDeployAppWithDomainNameButMissingBundleDescriptor() throws Exception {
    startDeployment();

    addExplodedDomainFromBuilder(emptyDomain101FileBuilder, emptyDomain101FileBuilder.getId());
    assertDeploymentSuccess(domainDeploymentListener, emptyDomain101FileBuilder.getId());

    addExplodedAppFromBuilder(appWithDomainNameButMissingBundleDescriptor, appWithDomainNameButMissingBundleDescriptor.getId());
    assertDeploymentFailure(applicationDeploymentListener, appWithDomainNameButMissingBundleDescriptor.getId());
  }

  @Test
  public void defaultDomainNameIsSetAfterDeployment() throws Exception {
    startDeployment();

    addExplodedAppFromBuilder(defaultDomainAppFileBuilder, defaultDomainAppFileBuilder.getId());
    assertDeploymentSuccess(applicationDeploymentListener, defaultDomainAppFileBuilder.getId());

    Application application = findApp(defaultDomainAppFileBuilder.getId(), 1);
    ApplicationDescriptor applicationDescriptor = application.getDescriptor();
    assertThat(applicationDescriptor.getDomainName(), is(DEFAULT_DOMAIN_NAME));
  }

  @Test
  public void domainNameIsSetAfterDeployment() throws Exception {
    startDeployment();

    addExplodedDomainFromBuilder(emptyDomain101FileBuilder, emptyDomain101FileBuilder.getId());
    assertDeploymentSuccess(domainDeploymentListener, emptyDomain101FileBuilder.getId());

    addExplodedAppFromBuilder(appDependingOnDomain101FileBuilder, appDependingOnDomain101FileBuilder.getId());
    assertDeploymentSuccess(applicationDeploymentListener, appDependingOnDomain101FileBuilder.getId());

    Application application = findApp(appDependingOnDomain101FileBuilder.getId(), 1);
    ApplicationDescriptor applicationDescriptor = application.getDescriptor();
    assertThat(applicationDescriptor.getDomainName(), is(emptyDomain101FileBuilder.getId()));
  }

  @Test
  public void compatibleDomainNameIsSetAfterDeployment() throws Exception {
    startDeployment();

    addExplodedDomainFromBuilder(emptyDomain101FileBuilder, emptyDomain101FileBuilder.getId());
    assertDeploymentSuccess(domainDeploymentListener, emptyDomain101FileBuilder.getId());

    addExplodedAppFromBuilder(appDependingOnDomain100FileBuilder, appDependingOnDomain100FileBuilder.getId());
    assertDeploymentSuccess(applicationDeploymentListener, appDependingOnDomain100FileBuilder.getId());

    Application application = findApp(appDependingOnDomain100FileBuilder.getId(), 1);
    ApplicationDescriptor applicationDescriptor = application.getDescriptor();
    assertThat(applicationDescriptor.getDomainName(), is(emptyDomain101FileBuilder.getId()));
  }

  @Test
  public void stoppedApplicationsAreNotStartedWhenDomainIsRedeployed() throws Exception {
    DeploymentListener mockDeploymentListener = spy(new DeploymentStatusTracker());
    deploymentService.addDeploymentListener(mockDeploymentListener);
    deployDomainAndApplication(emptyDomain100FileBuilder, appDependingOnDomain100FileBuilder);

    // Stop application and check status
    assertApplicationStatus(appDependingOnDomain100FileBuilder.getId(), STARTED);
    deploymentService.findApplication(appDependingOnDomain100FileBuilder.getId()).stop();
    assertApplicationStatus(appDependingOnDomain100FileBuilder.getId(), STOPPED);

    // Redeploy domain
    redeployDomain(deploymentService, emptyDomain100FileBuilder.getId());

    // Application was redeployed but it is not started
    verify(mockDeploymentListener, times(1)).onRedeploymentSuccess(appDependingOnDomain100FileBuilder.getId());
    assertApplicationStatus(appDependingOnDomain100FileBuilder.getId(), CREATED);

    // Redeploy domain again
    redeployDomain(deploymentService, emptyDomain100FileBuilder.getId());

    // Application was redeployed twice but it is not started
    verify(mockDeploymentListener, times(2)).onRedeploymentSuccess(appDependingOnDomain100FileBuilder.getId());
    assertApplicationStatus(appDependingOnDomain100FileBuilder.getId(), CREATED);
  }

  @Test
  public void startedApplicationsAreStartedWhenDomainIsRedeployed() throws Exception {
    DeploymentListener mockDeploymentListener = spy(new DeploymentStatusTracker());
    deploymentService.addDeploymentListener(mockDeploymentListener);
    deployDomainAndApplication(emptyDomain100FileBuilder, appDependingOnDomain100FileBuilder);

    // Check status
    assertApplicationStatus(appDependingOnDomain100FileBuilder.getId(), STARTED);

    // Redeploy domain
    redeployDomain(deploymentService, emptyDomain100FileBuilder.getId());

    // Application was redeployed and started
    verify(mockDeploymentListener, times(1)).onRedeploymentSuccess(appDependingOnDomain100FileBuilder.getId());
    assertApplicationStatus(appDependingOnDomain100FileBuilder.getId(), STARTED);
  }

  private void assertApplicationStatus(String appName, ApplicationStatus expectedStatus) {
    Application application = deploymentService.findApplication(appName);
    assertThat(application.getStatus(), is(expectedStatus));
  }

  private void deployDomainAndApplication(DomainFileBuilder domainFileBuilder,
                                          ApplicationFileBuilder applicationFileBuilder)
      throws Exception {
    assertThat("Application should depend on domain",
               applicationFileBuilder.getDependencies().contains(domainFileBuilder), is(true));

    // Add domain
    addExplodedDomainFromBuilder(domainFileBuilder, domainFileBuilder.getId());

    // Deploy an application (exploded)
    addExplodedAppFromBuilder(applicationFileBuilder);
    startDeployment();

    // Application was deployed
    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());
  }
}
