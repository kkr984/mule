/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static java.util.Arrays.asList;

import org.mule.runtime.ast.api.validation.ArtifactValidation;
import org.mule.runtime.ast.api.validation.Validation;
import org.mule.runtime.ast.api.validation.ValidationsProvider;

import java.util.List;

public class CoreValidationsProvider implements ValidationsProvider {

  private ClassLoader artifactRegionClassLoader;

  @Override
  public List<Validation> get() {
    return asList(new SingletonsAreNotRepeated(),
                  new SingletonsPerFileAreNotRepeated(),
                  new NamedTopLevelElementsHaveName(),
                  new NameHasValidCharacters(),
                  new NameIsNotRepeated(),
                  // make this general for all references via stereotypes
                  new FlowRefPointsToExistingFlow(),
                  new SourceErrorMappingAnyNotRepeated(),
                  new SourceErrorMappingAnyLast(),
                  new SourceErrorMappingTypeNotRepeated(),
                  new ErrorHandlerRefOrOnErrorExclusiveness(),
                  new ErrorHandlerOnErrorHasTypeOrWhen(),
                  new RaiseErrorTypeReferencesPresent(),
                  new RaiseErrorTypeReferencesExist(),
                  new ErrorMappingTargetTypeReferencesExist(),
                  new ErrorMappingSourceTypeReferencesExist(),
                  new ErrorHandlerOnErrorTypeExists(),
                  new RequiredParametersPresent(),
                  new ParameterGroupExclusiveness(),
                  new ExpressionsInRequiredExpressionsParams(),
                  new PollingSourceHasSchedulingStrategy(),
                  new RoundRobinRoutes(),
                  new FirstSuccessfulRoutes(),
                  new ScatterGatherRoutes(),
                  new ParseTemplateResourceExist(artifactRegionClassLoader),
                  new SourcePositiveMaxItemsPerPoll()
    // Commented out because this causes failures because of a lying extension model for munit, in the 'ignore' parameter
    // new NoExpressionsInNoExpressionsSupportedParams()
    // validate expressions!
    );
  }

  @Override
  public List<ArtifactValidation> getArtifactValidations() {
    return asList(new ImportValidTarget());
  }

  @Override
  public void setArtifactRegionClassLoader(ClassLoader artifactRegionClassLoader) {
    this.artifactRegionClassLoader = artifactRegionClassLoader;
  }
}
