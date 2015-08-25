/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.executor;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.when;
import static org.mule.module.extension.internal.util.ExtensionsTestUtils.getParameter;
import static org.mule.module.extension.internal.util.ExtensionsTestUtils.getResolver;
import org.mule.api.MuleEvent;
import org.mule.extension.introspection.ConfigurationModel;
import org.mule.extension.introspection.ExtensionModel;
import org.mule.extension.introspection.ParameterModel;
import org.mule.module.extension.internal.capability.metadata.ParameterGroupCapability;
import org.mule.module.extension.internal.runtime.config.ConfigurationObjectBuilder;
import org.mule.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class ConfigurationObjectBuilderTestCase extends AbstractMuleTestCase
{

    private static final String CONFIG_NAME = "configName";
    private static final String NAME_VALUE = "name";
    private static final String DESCRIPTION_VALUE = "description";

    @Mock
    private ExtensionModel extensionModel;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private ConfigurationModel configurationModel;

    @Mock
    private MuleEvent event;

    private TestConfig configuration;

    private ConfigurationObjectBuilder configurationObjectBuilder;
    private ResolverSet resolverSet;
    private ParameterModel nameParameterModel = getParameter("name", String.class);
    private ParameterModel descriptionParameterModel = getParameter("description", String.class);

    @Before
    public void before() throws Exception
    {
        configuration = new TestConfig();

        when(configurationModel.getParameterModels()).thenReturn(Arrays.asList(nameParameterModel, descriptionParameterModel));
        when(configurationModel.getInstantiator().newInstance()).thenReturn(configuration);
        when(configurationModel.getInstantiator().getObjectType()).thenAnswer(invocation -> TestConfig.class);
        when(configurationModel.getCapabilities(ParameterGroupCapability.class)).thenReturn(null);

        resolverSet = new ResolverSet();
        resolverSet.add(nameParameterModel, getResolver(NAME_VALUE));
        resolverSet.add(descriptionParameterModel, getResolver(DESCRIPTION_VALUE));

        configurationObjectBuilder = new ConfigurationObjectBuilder(configurationModel, resolverSet);
    }

    @Test
    public void build() throws Exception
    {
        TestConfig testConfig = (TestConfig) configurationObjectBuilder.build(event);
        assertThat(testConfig.getName(), is(NAME_VALUE));
        assertThat(testConfig.getDescription(), is(DESCRIPTION_VALUE));
    }

    public static class TestConfig
    {

        private String name;
        private String description;

        public String getName()
        {
            return name;
        }

        public String getDescription()
        {
            return description;
        }
    }
}
