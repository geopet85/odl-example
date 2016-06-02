/*
 * Copyright © 2015 George and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package odl.example.impl;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.openflowplugin.api.OFConstants;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetQueueActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.queue.action._case.SetQueueActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowCookie;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowModFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Instructions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.EtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4MatchBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;
import com.google.common.util.concurrent.ListenableFuture;


public class SwitchConfigurator {

    private static final Logger LOG = LoggerFactory.getLogger(SwitchConfigurator.class);

    private static SwitchConfigurator instance = null;
    private DataBroker db;
    private SalFlowService salFlowService;
    private long flowId;

    protected SwitchConfigurator() {
        flowId = 1;
    }

    public static SwitchConfigurator getInstance() {
        if(instance == null) {
            instance = new SwitchConfigurator();
        }
        return instance;
    }

    public void setDb(DataBroker db) {
        this.db = db;
    }

    public void setSalFlowService(SalFlowService sal) {
        this.salFlowService = sal;
    }

    private AtomicLong flowCookieInc = new AtomicLong(0x2a00000000000000L);

    public void send(String edge_switch, String edge_nodeconnector) {

        LOG.info("Start executing RPC");

        // create the flow
        Flow createdFlow = createFlow(edge_nodeconnector);

        // build instance identifier for flow
        InstanceIdentifier<Flow> flowPath = InstanceIdentifier
                .builder(Nodes.class)
                .child(Node.class, new NodeKey(new NodeId(edge_switch)))
                .augmentation(FlowCapableNode.class)
                .child(Table.class, new TableKey(createdFlow.getTableId()))
                .child(Flow.class, new FlowKey(createdFlow.getId())).build();


        final AddFlowInputBuilder builder = new AddFlowInputBuilder(createdFlow);
        final InstanceIdentifier<Table> tableInstanceId = flowPath
                .<Table> firstIdentifierOf(Table.class);
        final InstanceIdentifier<Node> nodeInstanceId = flowPath
                .<Node> firstIdentifierOf(Node.class);
        builder.setNode(new NodeRef(nodeInstanceId));
        builder.setFlowTable(new FlowTableRef(tableInstanceId));
        builder.setTransactionUri(new Uri(createdFlow.getId().getValue()));
        final AddFlowInput flow = builder.build();

        final String sw = edge_switch;
        LOG.info("onPacketReceived - About to write flow (via SalFlowService) {}", flow);
        // add flow to sal
        ListenableFuture<RpcResult<AddFlowOutput>> result = JdkFutureAdapters
                .listenInPoolThread(salFlowService.addFlow(flow));
        Futures.addCallback(result, new FutureCallback<RpcResult<AddFlowOutput>>() {
            @Override
            public void onSuccess(final RpcResult<AddFlowOutput> o) {
                LOG.info("Successful outcome.");
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOG.warn("Failure.");
                throwable.printStackTrace();
            }
        });
    }

    /**
     * Adds an application flow by using the REST API.
     */
    private Flow createFlow(String edge_nodeconnector) {


        FlowBuilder flowBuilder = new FlowBuilder() //
                .setTableId((short) 0) //
                .setFlowName("random");

        //
        flowBuilder.setId(new FlowId(Long.toString(flowBuilder.hashCode())));

        Ipv4Prefix srcIp = new Ipv4Prefix("10.0.0.1/32");
        Ipv4Prefix dstIp = new Ipv4Prefix("10.0.0.2/32");
        Match match = new MatchBuilder()

                .setLayer3Match(new Ipv4MatchBuilder()
                        .setIpv4Source(srcIp)
                        .setIpv4Destination(dstIp)
                        .build())
                .setEthernetMatch(new EthernetMatchBuilder()
                        .setEthernetType(new EthernetTypeBuilder()
                                .setType(new EtherType(0x0800L))
                                .build())
                        .build())
                .build();

        ActionBuilder actionBuilder = new ActionBuilder();
        List<Action> actions = new ArrayList<Action>();

        //Actions
        //currently changing tos and sending to output connector

        Action queueAction = actionBuilder
                .setOrder(0).setAction(new SetQueueActionCaseBuilder()
                    .setSetQueueAction(new SetQueueActionBuilder()
                    .setQueueId((long)1)
                    .build())
                .build())
                .build();
        actions.add(queueAction);

        Action outputNodeConnectorAction = actionBuilder
                .setOrder(1).setAction(new OutputActionCaseBuilder()
                        .setOutputAction(new OutputActionBuilder()
                                .setOutputNodeConnector(new Uri(edge_nodeconnector.split(":")[2]))
                                .build())
                        .build())
                .build();
        actions.add(outputNodeConnectorAction);

        //ApplyActions
        ApplyActions applyActions = new ApplyActionsBuilder().setAction(actions).build();

        //Instruction
        Instruction applyActionsInstruction = new InstructionBuilder() //
                .setOrder(0).setInstruction(new ApplyActionsCaseBuilder()//
                        .setApplyActions(applyActions) //
                        .build()) //
                .build();

        Instructions applyInstructions =  new InstructionsBuilder()
                .setInstruction(ImmutableList.of(applyActionsInstruction))
                .build();

        // Put our Instruction in a list of Instructions
        flowBuilder
                .setMatch(match)
                .setBufferId(OFConstants.OFP_NO_BUFFER)
                .setInstructions(applyInstructions)
                .setPriority(1000)
                .setHardTimeout((int)300)
                .setIdleTimeout(0)
                .setCookie(new FlowCookie(BigInteger.valueOf(flowCookieInc.getAndIncrement())))
                .setFlags(new FlowModFlags(false, false, false, false, false));

        return flowBuilder.build();
    }
}
