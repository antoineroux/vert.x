/*
 * Copyright 2011-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.vertx.java.core.impl;

/**
 * <p>An Implementation of {@link org.vertx.java.core.impl.Deferred} which can be used to implement custom synchronous actions.</p>
 *
 * <p>If you wish to create an instance of Deferred which does an operation without returning sa result, without blocking than
 * this class can be subclassed and the act method implemented.</p>
 *
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public abstract class SimpleAction extends SynchronousAction<Void> {

  @Override
  public Void action() throws Exception {
    act();
    return null;
  }

   /**
   * Implement this method in a subclass to implement the non blocking synchronous action .
   * <b>Do not</b> use this method to implement long running, blocking operations. Use {@link org.vertx.java.core.impl.BlockingAction} for that.
   */
  protected abstract void act();
}
