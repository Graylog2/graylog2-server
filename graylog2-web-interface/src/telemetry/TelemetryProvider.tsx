import React, { useMemo, useEffect, useState } from 'react';
import { PluginStore } from 'graylog-web-plugin/plugin';
import reduce from 'lodash/reduce';
import { usePostHog } from 'posthog-js/react';

import TelemetryContext from 'telemetry/TelemetryContext';
import { useStore } from 'stores/connect';
import type { CurrentUserStoreState } from 'stores/users/CurrentUserStore';
import { CurrentUserStore } from 'stores/users/CurrentUserStore';
import type { Store } from 'stores/StoreTypes';
import { NodesStore } from 'stores/nodes/NodesStore';
import { ClusterTrafficStore, ClusterTrafficActions } from 'stores/cluster/ClusterTrafficStore';
import { ClusterOverviewStore } from 'stores/cluster/ClusterOverviewStore';

const isLocalAdmin = (id: string) => {
  return id === 'local:admin';
};

const isEnterprisePluginInstalled = () => {
  return PluginStore.get().filter((p) => p.metadata.name === 'graylog-plugin-enterprise').length > 0;
};

const getAverageTraffic = (traffic) => {
  return reduce(traffic.output, (result, value) => result + value);
};

const getClusterOverview = (clusterOverview, nodes) => {
  return Object.keys(nodes).reduce((acc, nodeId) => {
    acc.nodes[nodeId] = {
      is_processing: clusterOverview[nodeId].is_processing,
      operating_system: clusterOverview[nodeId].operating_system,
      version: clusterOverview[nodeId].version,
    };

    return acc;
  }, { nodes: {} });
};

const getSHA256Hash = async (input) => {
  const textAsBuffer = new TextEncoder().encode(input);
  const hashBuffer = await window.crypto.subtle.digest('SHA-256', textAsBuffer);
  const hashArray = Array.from(new Uint8Array(hashBuffer));

  return hashArray
    .map((item) => item.toString(16).padStart(2, '0'))
    .join('');
};

const TelemetryProvider = ({ children }: { children: React.ReactElement }) => {
  const posthog = usePostHog();
  const currentUser = useStore(CurrentUserStore as Store<CurrentUserStoreState>, (state) => state?.currentUser);
  const nodes = useStore(NodesStore);
  const { traffic } = useStore(ClusterTrafficStore);
  const { clusterOverview } = useStore(ClusterOverviewStore) as any;
  const licensePlugin = PluginStore.exports('license');
  const licenseInfo = licensePlugin[0]?.hooks?.useLicenseStatusTelemetry();
  const [telemetryGroupId, setTelemetryGroupId] = useState(undefined);

  const telemetryGroupInfo = useMemo(() => {
    if (posthog) {
      return {
        isEnterprisePluginInstalled: isEnterprisePluginInstalled(),
        clusterInfo: {
          nodeCount: nodes.nodeCount,
          ...(clusterOverview && nodes?.nodes && getClusterOverview(clusterOverview, nodes.nodes)),
          ...(traffic && { averageLastMonthTraffic: getAverageTraffic(traffic) }),
        },
        ...(licenseInfo && { licenseInfo }),
      };
    }

    return undefined;
  }, [posthog, nodes.nodeCount, nodes.nodes, clusterOverview, traffic, licenseInfo]);

  useEffect(() => {
    ClusterTrafficActions.getTraffic(30);
  }, []);

  useEffect(() => {
    const setGroup = () => {
      if (nodes?.clusterId && clusterOverview && traffic && !telemetryGroupId) {
        setTelemetryGroupId(nodes.clusterId);
        posthog.group('company', nodes.clusterId, telemetryGroupInfo);
      }
    };

    if (posthog) {
      setGroup();
    }
  }, [clusterOverview, nodes, posthog, telemetryGroupId, telemetryGroupInfo, traffic]);

  useEffect(() => {
    const identify = async () => {
      if (currentUser && telemetryGroupId) {
        const id = await getSHA256Hash(currentUser.id + nodes.clusterId);

        posthog.identify(id, {
          isLocalAdmin: !!isLocalAdmin(currentUser.id),
          rolesCount: currentUser.roles.length,
        });
      }
    };

    if (posthog) {
      identify();
    }
  }, [currentUser, nodes.clusterId, posthog, telemetryGroupId]);

  const TelemetryContextValue = useMemo(() => {
    const sendTelemetry = (eventType: string, event: object) => {
      if (posthog) {
        posthog.capture(eventType, event);
      }
    };

    return ({
      sendTelemetry,
    });
  }, [posthog]);

  return (
    <TelemetryContext.Provider value={TelemetryContextValue}>
      {children}
    </TelemetryContext.Provider>
  );
};

export default TelemetryProvider;
