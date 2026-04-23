/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import type { TreeExpandedState, TreeNodeData } from '@mantine/core';

export type HealthStatus = 'success' | 'warning' | 'danger' | 'disabled';
export type HealthNodeKind = 'group' | 'check' | 'node';

type HealthNodeSeed = {
  value: string;
  label: string;
  description: string;
  suggestedAction?: string;
  affectedNodes?: string[];
  status?: HealthStatus;
  kind?: HealthNodeKind;
  children?: HealthNodeSeed[];
};

export type HealthTreeNode = {
  value: string;
  label: string;
  kind: HealthNodeKind;
  status: HealthStatus;
  description: string;
  suggestedAction?: string;
  affectedNodes: string[];
  path: string[];
  children?: HealthTreeNode[];
};

export type HealthTreeNodeProps = Pick<HealthTreeNode, 'kind' | 'status' | 'description' | 'suggestedAction' | 'affectedNodes' | 'path'>;

export type HealthTreeDataNode = TreeNodeData & {
  nodeProps: HealthTreeNodeProps;
  children?: HealthTreeDataNode[];
};

export type HealthStatusCounts = Record<HealthStatus, number>;

const emptyStatusCounts = (): HealthStatusCounts => ({
  success: 0,
  warning: 0,
  danger: 0,
  disabled: 0,
});

const deriveStatus = (statuses: HealthStatus[]): HealthStatus => {
  if (statuses.includes('danger')) return 'danger';
  if (statuses.includes('warning')) return 'warning';
  if (statuses.includes('success')) return 'success';

  return 'disabled';
};

const createCheck = ({
  value,
  label,
  description,
  status,
  suggestedAction = 'Review the affected telemetry and inspect the impacted resources in the detail view.',
  affectedNodes = [],
}: Required<Pick<HealthNodeSeed, 'value' | 'label' | 'description' | 'status'>> & Pick<HealthNodeSeed, 'suggestedAction' | 'affectedNodes'>): HealthNodeSeed => ({
  value,
  label,
  description,
  status,
  suggestedAction,
  affectedNodes,
  kind: 'check',
});

const createNode = ({
  value,
  label,
  description,
  status,
  suggestedAction = 'Inspect this node and compare it against its peers before adjusting thresholds or capacity.',
}: Required<Pick<HealthNodeSeed, 'value' | 'label' | 'description' | 'status'>> & Pick<HealthNodeSeed, 'suggestedAction'>): HealthNodeSeed => ({
  value,
  label,
  description,
  status,
  suggestedAction,
  kind: 'node',
});

const createGroup = ({
  value,
  label,
  description,
  children,
  suggestedAction = 'Open the nested checks to isolate the impacted subsystem and review the contributing signals.',
}: Required<Pick<HealthNodeSeed, 'value' | 'label' | 'description' | 'children'>> & Pick<HealthNodeSeed, 'suggestedAction'>): HealthNodeSeed => ({
  value,
  label,
  description,
  suggestedAction,
  children,
  kind: 'group',
});

const seedTree: HealthNodeSeed = createGroup({
  value: 'cluster-health',
  label: 'Cluster Health',
  description: 'Preview of a tree-based health surface that aggregates Graylog, search, forwarder, and collector signals into one drilldown.',
  suggestedAction: 'Select any branch or check to review the mocked root cause, suggested action, and affected entities.',
  children: [
    createGroup({
      value: 'graylog',
      label: 'Graylog',
      description: 'Application-layer health for Graylog processing nodes, message flow, platform dependencies, and core integrations.',
      children: [
        createGroup({
          value: 'graylog-nodes',
          label: 'Nodes',
          description: 'Infrastructure signals that describe the current health of Graylog processing nodes.',
          children: [
            createGroup({
              value: 'graylog-nodes-storage',
              label: 'Storage',
              description: 'Disk utilization and write headroom across Graylog nodes.',
              children: [
                createNode({
                  value: 'graylog-nodes-storage-node-1',
                  label: 'node-1',
                  status: 'success',
                  description: 'Storage utilization is within the healthy band and ingest headroom is stable on this node.',
                }),
                createNode({
                  value: 'graylog-nodes-storage-node-2',
                  label: 'node-2',
                  status: 'warning',
                  description: 'Disk usage is trending upward and is approaching the warning threshold on this node.',
                }),
                createNode({
                  value: 'graylog-nodes-storage-node-x',
                  label: 'node-x',
                  status: 'success',
                  description: 'This node has healthy free space and no abnormal write amplification.',
                }),
              ],
            }),
            createCheck({
              value: 'graylog-nodes-cpu',
              label: 'CPU',
              status: 'success',
              description: 'CPU utilization across Graylog nodes is balanced and there are no sustained saturation spikes.',
            }),
            createCheck({
              value: 'graylog-nodes-memory',
              label: 'Memory / JVM',
              status: 'warning',
              description: 'Heap utilization is elevated on one processing node and GC pressure is starting to increase.',
            }),
            createCheck({
              value: 'graylog-nodes-load-balancer',
              label: 'Load Balancer',
              status: 'success',
              description: 'All Graylog nodes are registered and accepting traffic from the load balancer.',
            }),
            createCheck({
              value: 'graylog-nodes-processing-state',
              label: 'Processing State',
              status: 'success',
              description: 'Message processing is enabled on the cluster and no node is paused or draining unexpectedly.',
            }),
          ],
        }),
        createGroup({
          value: 'graylog-input',
          label: 'Input',
          description: 'Checks for ingress pressure, failed input instances, and pipeline entry readiness.',
          children: [
            createCheck({
              value: 'graylog-input-buffer',
              label: 'Input Buffer',
              status: 'success',
              description: 'Input buffers are draining normally and there is no sustained backpressure at the intake edge.',
            }),
            createCheck({
              value: 'graylog-input-failures',
              label: 'Input Failures',
              status: 'warning',
              description: 'One input has entered a failed state recently and is reducing available ingest capacity.',
              affectedNodes: ['forwarder-input-a', 'beats-input-2'],
            }),
          ],
        }),
        createGroup({
          value: 'graylog-processing',
          label: 'Processing',
          description: 'Checks for processing buffer utilization and journal pressure inside the Graylog pipeline.',
          children: [
            createCheck({
              value: 'graylog-processing-buffer',
              label: 'Process Buffer',
              status: 'success',
              description: 'Process buffers are stable and current throughput is within the healthy operating band.',
            }),
            createCheck({
              value: 'graylog-processing-journal-size',
              label: 'Journal Size',
              status: 'warning',
              description: 'Journal utilization is elevated and indicates temporary downstream processing pressure.',
            }),
          ],
        }),
        createGroup({
          value: 'graylog-output',
          label: 'Output',
          description: 'Delivery health for output buffers and downstream output execution.',
          children: [
            createCheck({
              value: 'graylog-output-buffer',
              label: 'Output Buffer',
              status: 'success',
              description: 'Output buffer depth is stable and no sustained delivery congestion is visible.',
            }),
            createCheck({
              value: 'graylog-output-failures',
              label: 'Output Failures',
              status: 'danger',
              description: 'One or more outputs are actively failing, which can block message delivery for affected streams.',
              suggestedAction: 'Inspect the failing output destination and compare the error rate with recent backlog growth.',
              affectedNodes: ['primary-output', 'archive-output'],
            }),
          ],
        }),
        createGroup({
          value: 'graylog-archiving',
          label: 'Archiving',
          description: 'Archive export health and delivery status for long-term retention workflows.',
          children: [
            createCheck({
              value: 'graylog-archiving-failures',
              label: 'Archive Failures',
              status: 'disabled',
              description: 'Archive export monitoring is disabled in this preview, so no active health signal is being evaluated.',
              suggestedAction: 'Enable or wire archive health telemetry before treating this branch as actionable.',
            }),
          ],
        }),
        createGroup({
          value: 'graylog-data-lake',
          label: 'Data Lake',
          description: 'Connectivity and delivery health for the data lake integration path.',
          children: [
            createCheck({
              value: 'graylog-data-lake-connectivity',
              label: 'Connectivity',
              status: 'warning',
              description: 'Data lake connectivity is intermittent and some requests are retrying before they complete.',
            }),
            createCheck({
              value: 'graylog-data-lake-message-drops',
              label: 'Message Drops',
              status: 'danger',
              description: 'Messages are being dropped on the data lake path because the backend cannot keep up with current demand.',
              suggestedAction: 'Review the data lake sink health and reduce pressure before increasing retry or queue thresholds.',
            }),
          ],
        }),
        createGroup({
          value: 'graylog-integrations',
          label: 'Integrations',
          description: 'Operational checks for critical identity and notification integrations.',
          children: [
            createCheck({
              value: 'graylog-integrations-idp-sync',
              label: 'IdP Sync',
              status: 'disabled',
              description: 'Identity provider sync health is not currently evaluated in this preview.',
              suggestedAction: 'Add a normalized IdP synchronization signal before enabling this check in production.',
            }),
            createCheck({
              value: 'graylog-integrations-email-transport',
              label: 'Email Transport',
              status: 'warning',
              description: 'Email transport retries increased recently and delivery latency is outside the normal band.',
            }),
          ],
        }),
        createGroup({
          value: 'graylog-mongodb',
          label: 'MongoDB',
          description: 'Platform dependency checks for MongoDB connectivity, topology, and storage behavior.',
          children: [
            createCheck({
              value: 'graylog-mongodb-connectivity',
              label: 'Connectivity',
              status: 'success',
              description: 'Graylog can currently reach all configured MongoDB members without transport errors.',
            }),
            createCheck({
              value: 'graylog-mongodb-primary-state',
              label: 'Primary State',
              status: 'warning',
              description: 'Primary state recently changed and the replica set is stabilizing after an election event.',
            }),
            createCheck({
              value: 'graylog-mongodb-replication-lag',
              label: 'Replication Lag',
              status: 'warning',
              description: 'Replica lag is above the warning threshold on at least one MongoDB secondary.',
            }),
            createCheck({
              value: 'graylog-mongodb-slow-queries',
              label: 'Slow Queries',
              status: 'warning',
              description: 'Observed slow query volume is above the expected baseline for the current workload.',
            }),
            createCheck({
              value: 'graylog-mongodb-storage',
              label: 'Storage',
              status: 'success',
              description: 'MongoDB storage headroom is healthy and current write amplification is within the normal range.',
            }),
            createCheck({
              value: 'graylog-mongodb-connections',
              label: 'Connections',
              status: 'success',
              description: 'Connection utilization is stable and well below current pool or server limits.',
            }),
          ],
        }),
      ],
    }),
    createGroup({
      value: 'search-cluster',
      label: 'Search Cluster',
      description: 'Search backend health for data nodes, certificates, cluster state, and index management workflows.',
      children: [
        createGroup({
          value: 'search-cluster-data-nodes',
          label: 'Data Nodes',
          description: 'Operational health for the search cluster nodes backing indexing and query execution.',
          children: [
            createCheck({
              value: 'search-cluster-data-nodes-storage',
              label: 'Storage',
              status: 'success',
              description: 'Storage utilization across data nodes is balanced and shard writes have healthy headroom.',
            }),
            createCheck({
              value: 'search-cluster-data-nodes-cpu',
              label: 'CPU',
              status: 'success',
              description: 'CPU load is healthy on data nodes and query execution is not saturating the cluster.',
            }),
            createCheck({
              value: 'search-cluster-data-nodes-memory',
              label: 'Memory / JVM',
              status: 'warning',
              description: 'Heap pressure is elevated on one data node and cache churn is increasing.',
            }),
            createGroup({
              value: 'search-cluster-data-nodes-certificate',
              label: 'Certificate',
              description: 'Certificate validity and renewal readiness across data nodes.',
              children: [
                createNode({
                  value: 'search-cluster-data-nodes-certificate-node-a',
                  label: 'node-a',
                  status: 'success',
                  description: 'Certificate validity and trust configuration are healthy for this data node.',
                }),
                createNode({
                  value: 'search-cluster-data-nodes-certificate-node-b',
                  label: 'node-b',
                  status: 'warning',
                  description: 'Certificate renewal should be planned soon because the warning threshold has been crossed.',
                }),
                createNode({
                  value: 'search-cluster-data-nodes-certificate-node-x',
                  label: 'node-x',
                  status: 'danger',
                  description: 'Certificate validity is critically low for this node and service disruption risk is high.',
                  suggestedAction: 'Renew or replace the certificate before the node falls out of trust with the rest of the cluster.',
                }),
              ],
            }),
            createCheck({
              value: 'search-cluster-data-nodes-node-state',
              label: 'Node State',
              status: 'success',
              description: 'All data nodes are currently online and participating in cluster operations.',
            }),
            createCheck({
              value: 'search-cluster-data-nodes-cluster-state',
              label: 'Cluster State',
              status: 'warning',
              description: 'The cluster is operating with degraded tolerance and needs attention before the risk increases further.',
            }),
          ],
        }),
        createGroup({
          value: 'search-cluster-index-management',
          label: 'Index Management',
          description: 'Health of rotation, retention, tiering, and shard management behaviors.',
          children: [
            createCheck({
              value: 'search-cluster-index-management-rotation',
              label: 'Rotation',
              status: 'warning',
              description: 'Index rotation latency is above the normal baseline and rollover operations are queueing.',
            }),
            createCheck({
              value: 'search-cluster-index-management-retention-delete',
              label: 'Retention Delete',
              status: 'success',
              description: 'Retention deletions are completing successfully and no backlog is building.',
            }),
            createCheck({
              value: 'search-cluster-index-management-warm-tier-move',
              label: 'Warm Tier Move',
              status: 'disabled',
              description: 'Warm tier movement is not configured in this preview, so the check is intentionally inactive.',
            }),
            createCheck({
              value: 'search-cluster-index-management-shard-count',
              label: 'Shard Count',
              status: 'warning',
              description: 'Shard count is growing faster than expected and is approaching the configured warning boundary.',
            }),
          ],
        }),
      ],
    }),
    createGroup({
      value: 'forwarders',
      label: 'Forwarders',
      description: 'Edge ingest health for connectivity, throughput, and queue build-up at forwarder boundaries.',
      children: [
        createCheck({
          value: 'forwarders-connectivity',
          label: 'Connectivity',
          status: 'success',
          description: 'Connected forwarders are communicating normally and no transport interruption is visible.',
        }),
        createCheck({
          value: 'forwarders-throughput',
          label: 'Throughput',
          status: 'success',
          description: 'Forwarder throughput is within the expected operating range for the current message volume.',
        }),
        createCheck({
          value: 'forwarders-backlog',
          label: 'Backlog',
          status: 'warning',
          description: 'Forwarder backlog has started to increase and requires monitoring before it impacts ingest latency.',
        }),
      ],
    }),
    createGroup({
      value: 'collectors',
      label: 'Collectors',
      description: 'Sidecar and collector fleet health across connectivity, config propagation, and runtime failures.',
      children: [
        createCheck({
          value: 'collectors-connectivity',
          label: 'Connectivity',
          status: 'success',
          description: 'Collectors are reachable and current heartbeat coverage is within the expected healthy band.',
        }),
        createCheck({
          value: 'collectors-config-sync',
          label: 'Config Sync',
          status: 'disabled',
          description: 'Config sync is not modeled in this preview, so the check is intentionally displayed as disabled.',
        }),
        createCheck({
          value: 'collectors-failure-tracking',
          label: 'Failure Tracking',
          status: 'danger',
          description: 'Collector runtime failures are spiking and the fleet is losing reliability on multiple endpoints.',
          affectedNodes: ['collector-win-01', 'collector-linux-07', 'collector-edge-02'],
        }),
      ],
    }),
  ],
});

const buildTree = (seed: HealthNodeSeed, parentPath: string[] = []): HealthTreeNode => {
  const path = [...parentPath, seed.label];
  const children = seed.children?.map((child) => buildTree(child, path));
  const status = seed.status ?? deriveStatus(children?.map((child) => child.status) ?? []);

  return {
    value: seed.value,
    label: seed.label,
    kind: seed.kind ?? (children?.length ? 'group' : 'check'),
    status,
    description: seed.description,
    suggestedAction: seed.suggestedAction,
    affectedNodes: seed.affectedNodes ?? [],
    path,
    children,
  };
};

const toTreeData = (node: HealthTreeNode): HealthTreeDataNode => ({
  value: node.value,
  label: node.label,
  nodeProps: {
    kind: node.kind,
    status: node.status,
    description: node.description,
    suggestedAction: node.suggestedAction,
    affectedNodes: node.affectedNodes,
    path: node.path,
  },
  children: node.children?.map(toTreeData),
});

const buildLookup = (node: HealthTreeNode): Record<string, HealthTreeNode> => {
  const childLookup = Object.assign({}, ...(node.children ?? []).map(buildLookup));

  return {
    [node.value]: node,
    ...childLookup,
  };
};

const mergeCounts = (current: HealthStatusCounts, next: HealthStatusCounts): HealthStatusCounts => ({
  success: current.success + next.success,
  warning: current.warning + next.warning,
  danger: current.danger + next.danger,
  disabled: current.disabled + next.disabled,
});

const countStatuses = (node: HealthTreeNode): HealthStatusCounts => {
  if (!node.children?.length) {
    const counts = emptyStatusCounts();

    counts[node.status] = 1;

    return counts;
  }

  return node.children.reduce((counts, child) => mergeCounts(counts, countStatuses(child)), emptyStatusCounts());
};

const findFirstLeafByStatus = (node: HealthTreeNode, status: HealthStatus): HealthTreeNode | undefined => {
  if (!node.children?.length) {
    return node.status === status ? node : undefined;
  }

  for (const child of node.children) {
    const matchingLeaf = findFirstLeafByStatus(child, status);

    if (matchingLeaf) {
      return matchingLeaf;
    }
  }

  return undefined;
};

const getExpandedPath = (node: HealthTreeNode, value: string): string[] | undefined => {
  if (node.value === value) {
    return [node.value];
  }

  if (!node.children?.length) {
    return undefined;
  }

  for (const child of node.children) {
    const childPath = getExpandedPath(child, value);

    if (childPath) {
      return [node.value, ...childPath];
    }
  }

  return undefined;
};

export const mockHealthTree = buildTree(seedTree);
export const mockHealthTreeData: HealthTreeDataNode[] = [toTreeData(mockHealthTree)];
export const mockHealthTreeLookup = buildLookup(mockHealthTree);
export const mockHealthSummary = countStatuses(mockHealthTree);
export const defaultSelectedHealthNode = findFirstLeafByStatus(mockHealthTree, 'danger') ?? mockHealthTree;

const defaultExpandedValues = [
  mockHealthTree.value,
  ...(mockHealthTree.children?.map((child) => child.value) ?? []),
  ...(getExpandedPath(mockHealthTree, defaultSelectedHealthNode.value) ?? []),
];

export const mockHealthTreeInitialExpandedState = defaultExpandedValues.reduce<TreeExpandedState>((expandedState, value) => ({
  ...expandedState,
  [value]: true,
}), {});
