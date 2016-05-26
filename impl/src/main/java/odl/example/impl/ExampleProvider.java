/*
 * Copyright © 2015 George and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package odl.example.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odlexample.rev150105.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExampleProvider implements BindingAwareProvider, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(ExampleProvider.class);
    private BindingAwareBroker.RpcRegistration<OdlexampleService> exampleService;

    @Override
    public void onSessionInitiated(ProviderContext session) {
        LOG.info("ExampleProvider Session Initiated");
        DataBroker db = session.getSALService(DataBroker.class);
        exampleService = session.addRpcImplementation(OdlexampleService.class, new ExampleImpl(db));

        ApplicationRegistryUtils.getInstance().setDb(db);
        ApplicationRegistryUtils.getInstance().initializeDataTree();

        SalFlowService salFlowService = session.getRpcService(SalFlowService.class);
        SwitchConfigurator.getInstance().setDb(db);
        SwitchConfigurator.getInstance().setSalFlowService(salFlowService);

        NetworkGraphImpl.getInstance().setDb(db);
        NetworkGraphImpl.getInstance().init();

    }

    @Override
    public void close() throws Exception {
        LOG.info("ExampleProvider Closed");
    }

}
