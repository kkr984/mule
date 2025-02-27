/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.span.command;

/**
 * A void command with two parameters.
 *
 * @param <A> the type of the first parameter.
 * @param <B> the type of the second parameter
 */
public interface VoidBiCommand<A, B> {

  /**
   * executes the command
   *
   * @param firstParameter  the first parameter
   * @param secondParameter the second parameter.
   */
  void execute(A firstParameter, B secondParameter);

}
