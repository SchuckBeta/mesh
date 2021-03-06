package com.gentics.mesh.core.endpoint.admin.consistency.check;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;
import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.HIGH;
import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.MEDIUM;
import static com.gentics.mesh.core.rest.admin.consistency.RepairAction.DELETE;

import java.util.Iterator;

import org.apache.commons.lang.StringUtils;

import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.impl.GraphFieldContainerEdgeImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheck;
import com.gentics.mesh.core.endpoint.admin.consistency.repair.NodeDeletionGraphFieldContainerFix;
import com.gentics.mesh.core.rest.admin.consistency.ConsistencyCheckResponse;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.util.VersionNumber;
import com.syncleus.ferma.tx.Tx;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class GraphFieldContainerCheck implements ConsistencyCheck {

	private static final Logger log = LoggerFactory.getLogger(GraphFieldContainerCheck.class);

	@Override
	public void invoke(Database db, ConsistencyCheckResponse response, boolean attemptRepair) {
		Iterator<? extends NodeGraphFieldContainerImpl> it = db.getVerticesForType(NodeGraphFieldContainerImpl.class);
		while (it.hasNext()) {
			checkGraphFieldContainer(db, it.next(), response, attemptRepair);
		}
	}

	private void checkGraphFieldContainer(Database db, NodeGraphFieldContainer container, ConsistencyCheckResponse response, boolean attemptRepair) {
		String uuid = container.getUuid();
		if (container.getSchemaContainerVersion() == null) {
			response.addInconsistency("The GraphFieldContainer has no assigned SchemaContainerVersion", uuid, HIGH);
		}
		VersionNumber version = container.getVersion();
		if (version == null) {
			response.addInconsistency("The GraphFieldContainer has no version number", uuid, HIGH);
		}

		// GFC must either have a previous GFC, or must be the initial GFC for a Node
		NodeGraphFieldContainer previous = container.getPreviousVersion();
		if (previous == null) {
			Iterable<GraphFieldContainerEdgeImpl> initialEdges = container.inE(HAS_FIELD_CONTAINER)
				.has(GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, ContainerType.INITIAL.getCode()).frameExplicit(GraphFieldContainerEdgeImpl.class);
			if (!initialEdges.iterator().hasNext()) {

				boolean repaired = false;
				if (attemptRepair) {
					//printVersions(container);
					repaired = true;
					try (Tx tx = db.tx()) {
						new NodeDeletionGraphFieldContainerFix().repair(container);
						tx.success();
					} catch (Exception e) {
						repaired = false;
						log.error("Error while repairing inconsistency", e);
						throw e;
					}
				}

				response.addInconsistency(
					String.format("GraphFieldContainer {" + version + "} does not have previous GraphFieldContainer and is not INITIAL for a Node"),
					uuid,
					MEDIUM,
					repaired,
					DELETE);
				return;

			}
		} else {
			VersionNumber previousVersion = previous.getVersion();
			if (previousVersion != null && version != null) {
				if (!version.equals(previousVersion.nextDraft()) && !version.equals(previousVersion.nextPublished())) {
					String nodeInfo = "unknown";
					try {
						Node node = container.getParentNode();
						nodeInfo = node.getUuid();
					} catch (Exception e) {
						log.debug("Could not load node uuid", e);
					}
					response.addInconsistency(
						String.format(
							"GraphFieldContainer of Node {" + nodeInfo
								+ "} has version %s which does not come after its previous GraphFieldContainer's version %s",
							version,
							previousVersion),
						uuid, MEDIUM);
				}
			}
		}

		// GFC must either have a next GFC, or must be the draft GFC for a Node
		if (!container.hasNextVersion() && !container.isDraft()) {
			String nodeInfo = "unknown";
			try {
				Node node = container.getParentNode();
				nodeInfo = node.getUuid();
			} catch (Exception e) {
				log.debug("Could not load node uuid", e);
			}
			response.addInconsistency(
				String.format("GraphFieldContainer {" + version + "} of Node {" + nodeInfo
					+ "} does not have next GraphFieldContainer and is not DRAFT for a Node"),
				uuid,
				MEDIUM);
		}
	}

	private void printVersions(NodeGraphFieldContainer container) {
		System.out.println("Version history for {" + container.getUuid() + "}" + "version {" + container.getVersion() + "}");
		// Find the root
		NodeGraphFieldContainer prev = container.getPreviousVersion();
		while (prev != null) {
			NodeGraphFieldContainer p = prev.getPreviousVersion();
			if (p != null) {
				prev = p;
			} else {
				break;
			}
		}

		if (prev == null) {
			prev = container;
		}

		System.out.println("(" + prev.getVersion() + ") - Node: " + prev.in(HAS_FIELD_CONTAINER).nextOrDefaultExplicit(NodeImpl.class, null));
		Iterable<? extends NodeGraphFieldContainer> versions = prev.getNextVersions();

		printVersions(versions, 1);

	}

	private void printVersions(Iterable<? extends NodeGraphFieldContainer> versions, int level) {
		Iterator<? extends NodeGraphFieldContainer> it = versions.iterator();
		if (it.hasNext()) {
			for (NodeGraphFieldContainer v : versions) {
				String info = " - Node: " + v.in(HAS_FIELD_CONTAINER).nextOrDefaultExplicit(NodeImpl.class, null);
				String str = "↳ (" + v.getVersion() + ")" + info;
				System.out.println(StringUtils.leftPad(str, (level * 2) + str.length()));
				printVersions(v.getNextVersions(), ++level);
			}
		}

	}
}
