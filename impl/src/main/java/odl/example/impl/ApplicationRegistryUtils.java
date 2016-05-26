/*
 * Copyright © 2015 George and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package odl.example.impl;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odlexample.rev150105.AddApplicationInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odlexample.rev150105.AddApplicationInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odlexample.rev150105.ApplicationRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odlexample.rev150105.ApplicationRegistryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odlexample.rev150105.application.registry.ApplicationRegistryEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odlexample.rev150105.application.registry.ApplicationRegistryEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.odlexample.rev150105.application.registry.ApplicationRegistryEntryKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Created by geopet on 26/5/2016.
 */
public class ApplicationRegistryUtils {

    private static final Logger LOG = LoggerFactory.getLogger(ApplicationRegistryUtils.class);
    private static ApplicationRegistryUtils instance = null;
    private DataBroker db;
    protected ApplicationRegistryUtils() {

    }

    public static ApplicationRegistryUtils getInstance() {
        if(instance == null) {
            instance = new ApplicationRegistryUtils();
        }
        return instance;
    }

    public void setDb(DataBroker db) {
        this.db = db;
    }

    public void initializeDataTree() {
        LOG.info("Preparing to initialize the application registry");
        WriteTransaction transaction = db.newWriteOnlyTransaction();
        InstanceIdentifier<ApplicationRegistry> iid = InstanceIdentifier.create(ApplicationRegistry.class);
        ApplicationRegistry greetingRegistry = new ApplicationRegistryBuilder()
                .build();
        transaction.put(LogicalDatastoreType.OPERATIONAL, iid, greetingRegistry);
        CheckedFuture<Void, TransactionCommitFailedException> future = transaction.submit();
        Futures.addCallback(future, new LoggingFuturesCallBack<>("Failed to create application registry", LOG));
    }

    public void writeToApplicationRegistry(AddApplicationInput input) {
        LOG.info("Writing to application registry input {}.", input);
        WriteTransaction transaction = db.newWriteOnlyTransaction();
        InstanceIdentifier<ApplicationRegistryEntry> iid = toInstanceIdentifier(input);
        ApplicationRegistryEntry application = new ApplicationRegistryEntryBuilder()
                .setAppId(input.getAppId())
                .setJitter(input.getJitter())
                .setPacketLoss(input.getPacketLoss())
                .setPacketDelay(input.getPacketDelay())
                .setBandwidth(input.getBandwidth())
                .build();
        transaction.put(LogicalDatastoreType.OPERATIONAL, iid, application);
        CheckedFuture<Void, TransactionCommitFailedException> future = transaction.submit();
        Futures.addCallback(future, new LoggingFuturesCallBack<Void>("Failed to write to application registry", LOG));
    }

    private InstanceIdentifier<ApplicationRegistryEntry> toInstanceIdentifier(AddApplicationInput input) {
        InstanceIdentifier<ApplicationRegistryEntry> iid = InstanceIdentifier.create(ApplicationRegistry.class)
                .child(ApplicationRegistryEntry.class,
                        new ApplicationRegistryEntryKey(input.getAppId()));
        return iid;
    }

    private InstanceIdentifier<ApplicationRegistryEntry> toInstanceIdentifier(int input) {
        InstanceIdentifier<ApplicationRegistryEntry> iid = InstanceIdentifier.create(ApplicationRegistry.class)
                .child(ApplicationRegistryEntry.class,
                        new ApplicationRegistryEntryKey(input));
        return iid;
    }


    public AddApplicationInput readFromApplicationRegistry(int appId) {
        LOG.info("Reading from application registry for appID {}.", appId);
        AddApplicationInput application = null;
        ReadOnlyTransaction transaction = db.newReadOnlyTransaction();
        InstanceIdentifier<ApplicationRegistryEntry> iid = toInstanceIdentifier(appId);
        CheckedFuture<Optional<ApplicationRegistryEntry>, ReadFailedException> future =
                transaction.read(LogicalDatastoreType.OPERATIONAL, iid);
        Optional<ApplicationRegistryEntry> optional = Optional.absent();
        try {
            optional = future.checkedGet();
        } catch (ReadFailedException e) {
            LOG.error("Reading application failed:",e);
        }
        if(optional.isPresent()) {
            application = new AddApplicationInputBuilder()
                    .setAppId(optional.get().getAppId())
                    .setJitter(optional.get().getJitter())
                    .setPacketLoss(optional.get().getPacketLoss())
                    .setPacketDelay(optional.get().getPacketDelay())
                    .setBandwidth(optional.get().getBandwidth())
                    .build();
        }
        return application;
    }
}
