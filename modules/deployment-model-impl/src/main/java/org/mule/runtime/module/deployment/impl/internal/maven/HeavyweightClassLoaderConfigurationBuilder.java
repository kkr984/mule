/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.maven;

import static com.vdurmont.semver4j.Semver.SemverType.LOOSE;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;

import static org.mule.runtime.module.artifact.activation.internal.plugin.PluginLocalDependenciesDenylist.isDenylisted;
import static org.mule.runtime.module.deployment.impl.internal.maven.AbstractMavenClassLoaderConfigurationLoader.CLASS_LOADER_MODEL_VERSION_120;

import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleScope;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfiguration;
import org.mule.tools.api.classloader.model.AppClassLoaderModel;
import org.mule.tools.api.classloader.model.Artifact;
import org.mule.tools.api.classloader.model.ClassLoaderModel;

import java.io.File;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.vdurmont.semver4j.Semver;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Plugin;

/**
 * Builder for a {@link ClassLoaderConfiguration} with information from a {@link ClassLoaderModel} included when packaging the
 * artifact in a heavyweight manner.
 *
 * @since 4.2.0
 */
public class HeavyweightClassLoaderConfigurationBuilder extends ArtifactClassLoaderConfigurationBuilder {

  public static final Semver CLASS_LOADER_MODEL_VERSION_110 = new Semver("1.1.0", LOOSE);

  private final org.mule.tools.api.classloader.model.ClassLoaderModel packagerClassLoaderModel;

  private Semver classLoaderModelVersion;

  public HeavyweightClassLoaderConfigurationBuilder(File applicationFolder, BundleDescriptor artifactBundleDescriptor,
                                                    org.mule.tools.api.classloader.model.ClassLoaderModel packagerClassLoaderModel) {
    super(applicationFolder, artifactBundleDescriptor);
    this.packagerClassLoaderModel = packagerClassLoaderModel;
    this.classLoaderModelVersion = new Semver(packagerClassLoaderModel.getVersion(), LOOSE);
  }

  /**
   * Exports the shared libraries resources and packages.
   */
  @Override
  protected void doExportSharedLibrariesResourcesAndPackages(Plugin packagingPlugin) {
    if (new Semver(packagerClassLoaderModel.getVersion(), LOOSE).isLowerThan(CLASS_LOADER_MODEL_VERSION_110)) {
      super.doExportSharedLibrariesResourcesAndPackages(packagingPlugin);
    } else {
      exportSharedLibrariesResourcesAndPackages();
    }
  }

  @Override
  protected Map<BundleDescriptor, List<BundleDescriptor>> doProcessAdditionalPluginLibraries(Plugin packagingPlugin) {
    if (packagerClassLoaderModel instanceof AppClassLoaderModel) {
      AppClassLoaderModel appClassLoaderModel = (AppClassLoaderModel) packagerClassLoaderModel;
      appClassLoaderModel.getAdditionalPluginDependencies()
          .ifPresent(additionalDeps -> additionalDeps.forEach(this::updateDependency));
    }
    return emptyMap();
  }

  @Override
  protected List<URI> processPluginAdditionalDependenciesURIs(BundleDependency bundleDependency) {
    return bundleDependency.getAdditionalDependenciesList().stream().map(additionalDependency -> {
      if (isSupportingPackagesResourcesInformation() && !isDenylisted(additionalDependency.getDescriptor())) {
        withLocalPackages(additionalDependency.getPackages());
        withLocalResources(additionalDependency.getResources());
      }
      return additionalDependency.getBundleUri();
    }).collect(toList());
  }

  private BundleDependency createExtendedBundleDependency(BundleDependency original,
                                                          List<BundleDependency> additionalPluginDependencies) {
    return new BundleDependency.Builder(original).setAdditionalDependencies(additionalPluginDependencies).build();
  }

  private void updateDependency(org.mule.tools.api.classloader.model.Plugin plugin) {
    dependencies.stream()
        .filter(dep -> areSameDependency(plugin, dep))
        .findFirst()
        .ifPresent(
                   pluginDependency -> replaceBundleDependency(
                                                               pluginDependency,
                                                               createExtendedBundleDependency(
                                                                                              pluginDependency,
                                                                                              plugin.getAdditionalDependencies()
                                                                                                  .stream()
                                                                                                  .map(this::toBundleDependency)
                                                                                                  .collect(toList()))));
  }

  private boolean areSameDependency(org.mule.tools.api.classloader.model.Plugin plugin, BundleDependency dependency) {
    return StringUtils.equals(dependency.getDescriptor().getGroupId(), plugin.getGroupId())
        && StringUtils.equals(dependency.getDescriptor().getArtifactId(), plugin.getArtifactId());
  }

  private BundleDependency toBundleDependency(Artifact artifact) {
    BundleDependency.Builder builder = new BundleDependency.Builder();
    if (artifact.getArtifactCoordinates().getScope() != null) {
      builder.setScope(BundleScope.valueOf(artifact.getArtifactCoordinates().getScope().toUpperCase()));
    }

    BundleDependency.Builder bundleDependencyBuilder = builder
        .setBundleUri(artifact.getUri().isAbsolute()
            ? artifact.getUri()
            : new File(artifactFolder, artifact.getUri().toString()).toURI())
        .setDescriptor(new BundleDescriptor.Builder()
            .setArtifactId(artifact.getArtifactCoordinates().getArtifactId())
            .setGroupId(artifact.getArtifactCoordinates().getGroupId())
            .setVersion(artifact.getArtifactCoordinates().getVersion())
            .setClassifier(artifact.getArtifactCoordinates().getClassifier())
            .setType(artifact.getArtifactCoordinates().getType())
            .build());
    if (isSupportingPackagesResourcesInformation()) {
      bundleDependencyBuilder
          .setPackages(artifact.getPackages() == null ? emptySet() : new HashSet<>(asList(artifact.getPackages())));
      bundleDependencyBuilder
          .setResources(artifact.getResources() == null ? emptySet() : new HashSet<>(asList(artifact.getResources())));
    }
    return bundleDependencyBuilder.build();
  }

  private boolean isSupportingPackagesResourcesInformation() {
    return !classLoaderModelVersion.isLowerThan(CLASS_LOADER_MODEL_VERSION_120);
  }

  /**
   * Exports shared libraries resources and packages getting the information from the packager {@link ClassLoaderModel}.
   */
  private void exportSharedLibrariesResourcesAndPackages() {
    packagerClassLoaderModel.getDependencies().stream()
        .filter(Artifact::isShared)
        .filter(sharedDep -> !validateMuleRuntimeSharedLibrary(sharedDep.getArtifactCoordinates().getGroupId(),
                                                               sharedDep.getArtifactCoordinates().getArtifactId()))
        .forEach(sharedDep -> {
          if (isSupportingPackagesResourcesInformation()) {
            this.exportingPackages(sharedDep.getPackages() == null ? emptySet() : new HashSet<>(asList(sharedDep.getPackages())));
            this.exportingResources(sharedDep.getResources() == null ? emptySet()
                : new HashSet<>(asList(sharedDep.getResources())));
          } else {
            findAndExportSharedLibrary(sharedDep.getArtifactCoordinates().getGroupId(),
                                       sharedDep.getArtifactCoordinates().getArtifactId());
          }
        });
  }

}
