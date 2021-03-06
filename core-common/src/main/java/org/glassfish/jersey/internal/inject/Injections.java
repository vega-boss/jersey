/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * http://glassfish.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package org.glassfish.jersey.internal.inject;

import java.util.Iterator;
import java.util.ServiceLoader;

import javax.ws.rs.WebApplicationException;

import org.glassfish.jersey.hk2.Hk2InjectionManagerFactory;

/**
 * Injection binding utility methods.
 *
 * @author Tom Beerbower
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class Injections {

    /**
     * Creates a {@link InjectionManager} without parent and initial binder.
     *
     * @return a injection manager with all the bindings.
     */
    public static InjectionManager createInjectionManager() {
        return lookupInjectionManagerFactory().create();
    }

    /**
     * Creates a {@link InjectionManager} with initial binder that is immediately registered.
     *
     * @param binder custom the {@link Binder binder}.
     * @return a injection manager with all the bindings.
     */
    public static InjectionManager createInjectionManager(Binder binder) {
        InjectionManager injectionManager = lookupInjectionManagerFactory().create();
        injectionManager.register(binder);
        return injectionManager;
    }

    /**
     * Creates an unnamed, parented {@link InjectionManager}. In case the {@code parent} injection manager is not specified, the
     * locator will not be parented.
     *
     * @param parent  The parent of this injection manager. Services can be found in the parent (and all grand-parents). May be
     *                {@code null}. An underlying DI provider checks whether the parent is in a proper type.
     * @return an injection manager with all the bindings.
     */
    public static InjectionManager createInjectionManager(Object parent) {
        return lookupInjectionManagerFactory().create(parent);
    }

    private static InjectionManagerFactory lookupInjectionManagerFactory() {
        Iterator<InjectionManagerFactory> iterator = ServiceLoader.load(InjectionManagerFactory.class).iterator();
        if (iterator.hasNext()) {
            return iterator.next();
        } else {
            // TODO: Log that there is no explicitly configured InjectionManager, default is used.
            return new Hk2InjectionManagerFactory();
        }
    }

    /**
     * Get the class by contract or create and inject a new instance.
     *
     * @param <T>              instance type.
     * @param injectionManager DI injection manager.
     * @param clazz            class of the instance to be provider.
     * @return instance of the class either provided as a service or created and injected  by HK2.
     */
    public static <T> T getOrCreate(InjectionManager injectionManager, final Class<T> clazz) {
        try {
            final T component = injectionManager.getInstance(clazz);
            return component == null ? injectionManager.createAndInitialize(clazz) : component;
        } catch (final RuntimeException e) {
            // Look for WebApplicationException and return it if found. MultiException is thrown when *Param field is
            // annotated and value cannot be provided (for example fromString(String) method can throw unchecked
            // exception.
            //
            // see InvalidParamTest
            // see JERSEY-1117
            Throwable throwable = e.getCause();
            if (throwable != null && WebApplicationException.class.isAssignableFrom(throwable.getClass())) {
                throw (WebApplicationException) throwable;
            }

            throw e;
        }
    }
}
