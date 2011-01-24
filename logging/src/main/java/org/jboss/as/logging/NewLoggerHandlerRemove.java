/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.logging;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.ModelRemoveOperationHandler;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.server.NewRuntimeOperationContext;
import org.jboss.as.server.RuntimeOperationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceRegistry;

/**
 * @author Emanuel Muckenhuber
 */
class NewLoggerHandlerRemove implements ModelRemoveOperationHandler, RuntimeOperationHandler {

    static final NewLoggerHandlerRemove INSTANCE = new NewLoggerHandlerRemove();

    /** {@inheritDoc} */
    public Cancellable execute(final NewOperationContext context, final ModelNode operation, final ResultHandler resultHandler) {

        final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        final String name = address.getLastElement().getValue();

        final ModelNode subModel = context.getSubModel();
        final ModelNode compensatingOperation = new ModelNode();
        compensatingOperation.get(OP_ADDR).set(operation.require(OP_ADDR));
        compensatingOperation.get(OP).set("set-root-logger");
        for(final Property property : subModel.asPropertyList()) {
            compensatingOperation.get(property.getName()).set(property.getValue());
        }

        if(context instanceof NewRuntimeOperationContext) {
            final NewRuntimeOperationContext runtimeContext = (NewRuntimeOperationContext) context;
            final ServiceRegistry registry = runtimeContext.getServiceRegistry();
            try {
                final ServiceController<?> controller = registry.getService(LogServices.handlerName(name));
                if(controller != null) {
                    controller.setMode(ServiceController.Mode.REMOVE);
                }
            } catch (Throwable t) {
                resultHandler.handleFailed(new ModelNode().set(t.getLocalizedMessage()));
            }
        }

        resultHandler.handleResultComplete(compensatingOperation);

        return Cancellable.NULL;
    }

}
