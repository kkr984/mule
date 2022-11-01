/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.plugin;

import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfiguration;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfigurationLoader;

import java.util.Map;

/**
 * Allows to extends the attributes defined for a {@link ClassLoaderConfiguration} when it is being loaded by
 * {@link ClassLoaderConfigurationLoader} for plugins in order to define in which deployable artifact the plugin is declared.
 *
 * @since 4.2.0
 */
// TODO - W-11098291: remove
@Deprecated
public class PluginExtendedClassLoaderModelAttributes extends PluginExtendedClassLoaderConfigurationAttributes {

  /**
   * Creates an instance of this extended attributes for the given descriptor.
   *
   * @param originalAttributes           the original {@link Map} of attributes. No null.
   * @param deployableArtifactDescriptor {@link ArtifactDescriptor} which declares the plugin dependency. Not null.
   */
  public PluginExtendedClassLoaderModelAttributes(Map originalAttributes, ArtifactDescriptor deployableArtifactDescriptor) {
    super(originalAttributes, deployableArtifactDescriptor);
  }

}
