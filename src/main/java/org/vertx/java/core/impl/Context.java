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

import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public abstract class Context {

  private static final Logger log = LoggerFactory.getLogger(Context.class);

  private static final ThreadLocal<Context> contextTL = new ThreadLocal<>();

  private DeploymentHandle deploymentContext;

  private List<Runnable> closeHooks;

  public static void setContext(Context context) {
    contextTL.set(context);
  }

  public static Context getContext() {
    return contextTL.get();
  }

  public void setDeploymentHandle(DeploymentHandle deploymentHandle) {
    this.deploymentContext = deploymentHandle;
  }

  public DeploymentHandle getDeploymentHandle() {
    return deploymentContext;
  }

  public void reportException(Throwable t) {
    if (deploymentContext != null) {
      deploymentContext.reportException(t);
    } else {
      log.error("Unhandled exception", t);
    }
  }

  public void addCloseHook(Runnable hook) {
    if (closeHooks == null) {
      closeHooks = new ArrayList<>();
    }
    closeHooks.add(hook);
  }

  public void runCloseHooks() {
    if (closeHooks != null) {
      for (Runnable hook: closeHooks) {
        try {
          hook.run();
        } catch (Throwable t) {
          reportException(t);
        }
      }
    }
  }

  public abstract void execute(Runnable handler);

  protected Runnable wrapTask(final Runnable task) {
    return new Runnable() {
      public void run() {
        try {
          setContext(Context.this);
          task.run();
        } catch (Throwable t) {
          reportException(t);
        }
      }
    };
  }
}
