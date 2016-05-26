/*
 * Copyright © 2015 George and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package odl.example.impl;

import com.google.common.util.concurrent.Futures;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odlexample.rev150105.*;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Future;


/**
 * Created by geopet on 26/5/2016.
 */
public class ExampleImpl implements OdlexampleService {

    private static final Logger LOG = LoggerFactory.getLogger(ExampleProvider.class);
    private DataBroker db;

    public ExampleImpl(DataBroker db) {
        this.db = db;
    }

    @Override
    public Future<RpcResult<Void>> addApplication(AddApplicationInput input) {
        LOG.info("Adding application {}", input);

        ApplicationRegistryUtils.getInstance().writeToApplicationRegistry(input);

        SwitchConfigurator.getInstance().send("openflow:1", "openflow:1:2");

        NodeMonitor monitor = new NodeMonitor(db);
        monitor.getNodeFromIpAddress("10.0.0.1");
        monitor.measureNodeStatistics("openflow:1", "openflow:1:2");

        return Futures.immediateFuture(RpcResultBuilder.<Void>success().build());
    }
}
