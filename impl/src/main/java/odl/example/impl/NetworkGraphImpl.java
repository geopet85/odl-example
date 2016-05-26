/*
 * Copyright © 2015 Intracom Telecom and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package odl.example.impl;

import com.google.common.base.Optional;

import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.SparseMultigraph;
import edu.uci.ics.jung.graph.util.EdgeType;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class NetworkGraphImpl {

    private static final Logger LOG = LoggerFactory.getLogger(NetworkGraphImpl.class);
    private Graph<NodeId, Link> networkGraph = null;
    private Set<String> linkAdded = new HashSet<>();
    private DijkstraShortestPath<NodeId, Link> shortestPath = null;
    private static NetworkGraphImpl instance = null;
    private DataBroker db;
    
    protected NetworkGraphImpl() {

    }

    public static NetworkGraphImpl getInstance() {
        if(instance == null) {
            instance = new NetworkGraphImpl();
        }
        return instance;
    }

    public void setDb(DataBroker db) {
        this.db = db;
    }

	public DijkstraShortestPath<NodeId, Link> getShortestPath() {
		return shortestPath;
	}

	public void setShortestPath(DijkstraShortestPath<NodeId, Link> shortestPath) {
		this.shortestPath = shortestPath;
	}

	public void init () {
        LOG.info("Initializing network graph!");
        clearGraph();
        List<Link> links = getLinksFromTopology();
        if(links == null || links.isEmpty()) {
            return;
        }
        addLinks(links);
    }

    private List<Link> getLinksFromTopology() {
        InstanceIdentifier<Topology> topologyInstanceIdentifier = InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(new TopologyId("flow:1")))
                .build();
        Topology topology = null;
        ReadOnlyTransaction readOnlyTransaction = db.newReadOnlyTransaction();
        try {
            Optional<Topology> topologyOptional = readOnlyTransaction.read(LogicalDatastoreType.OPERATIONAL, topologyInstanceIdentifier).get();
            if(topologyOptional.isPresent()) {
                topology = topologyOptional.get();
            }
        } catch(Exception e) {
            LOG.error("Error reading topology {}", topologyInstanceIdentifier);
            readOnlyTransaction.close();
            throw new RuntimeException("Error reading from operational store, topology : " + topologyInstanceIdentifier, e);
        }
        readOnlyTransaction.close();
        if(topology == null) {
            return null;
        }
        List<Link> links = topology.getLink();
        if(links == null || links.isEmpty()) {
            return null;
        }
        List<Link> internalLinks = new ArrayList<>();
        for(Link link : links) {
            if(!(link.getLinkId().getValue().contains("host"))) {
                internalLinks.add(link);
            }
        }

        return internalLinks;
    }

    public synchronized void addLinks(List<Link> links) {
        if(links == null || links.isEmpty()) {
            LOG.debug("In addLinks: No link added as links is null or empty.");
            return;
        }

        if(networkGraph == null) {
            networkGraph = new SparseMultigraph<>();
        }

        for(Link link : links) {
            if(linkAlreadyAdded(link)) {
                continue;
            }
            NodeId sourceNodeId = link.getSource().getSourceNode();
            NodeId destinationNodeId = link.getDestination().getDestNode();
            networkGraph.addVertex(sourceNodeId);
            networkGraph.addVertex(destinationNodeId);
            networkGraph.addEdge(link, sourceNodeId, destinationNodeId, EdgeType.UNDIRECTED);
        }

        LOG.info("Created topology graph {} ", networkGraph);

        if(shortestPath == null) {
            shortestPath = new DijkstraShortestPath<>(networkGraph);
        } else {
            shortestPath.reset();
        }
        LOG.info("Shortest paths {} ", shortestPath);
    }

    private boolean linkAlreadyAdded(Link link) {
        String linkAddedKey = null;
        if(link.getDestination().getDestTp().hashCode() > link.getSource().getSourceTp().hashCode()) {
            linkAddedKey = link.getSource().getSourceTp().getValue() + link.getDestination().getDestTp().getValue();
        } else {
            linkAddedKey = link.getDestination().getDestTp().getValue() + link.getSource().getSourceTp().getValue();
        }
        if(linkAdded.contains(linkAddedKey)) {
            return true;
        } else {
            linkAdded.add(linkAddedKey);
            return false;
        }
    }
    
    public void clearGraph() {
    	networkGraph = new SparseMultigraph<>();
    }

}
