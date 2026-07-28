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
import React from 'react';

import { Alert, SegmentedControl } from 'components/bootstrap';

export type DataNodeUpgradeMethodType = 'cluster-restart' | 'rolling-upgrade';

const UpgradeMethodSegments: Array<{ value: DataNodeUpgradeMethodType; label: string }> = [
  { value: 'cluster-restart', label: 'Cluster Restart' },
  { value: 'rolling-upgrade', label: 'Rolling Upgrade' },
];

type Props = {
  upgradeMethod: DataNodeUpgradeMethodType;
  onChange: (value: DataNodeUpgradeMethodType) => void;
};

const UpgradeMethodSelector = ({ upgradeMethod, onChange }: Props) => (
  <>
    <SegmentedControl data={UpgradeMethodSegments} value={upgradeMethod} onChange={onChange} />
    <Alert bsStyle="info">
      {upgradeMethod === 'cluster-restart' && (
        <>
          <p>
            When using the cluster restart method, you will upgrade all Data Nodes at once. During this time, messages
            will be buffered in the journal and processed as the Data Node cluster comes back online, leading to no data
            loss provided your journal size is configured for the message volume which is expected during the Data Node
            downtime.
          </p>
          <p>
            If you are running a Data Node cluster with less than three nodes, the cluster restart method is the only
            method available.
          </p>
          <p>
            If you are running a Data Node cluster with three or more nodes, you can choose to use the cluster restart
            method after consideration of your journal size and your message throughput.
          </p>
        </>
      )}
      {upgradeMethod === 'rolling-upgrade' && (
        <>
          <p>
            Rolling upgrades can be performed on a running Data Node cluster only with <b>three or more nodes</b>, with
            virtually no downtime.
          </p>
          <p>
            Data Nodes are individually stopped and upgraded in place. Alternatively, Data Nodes can be stopped and
            replaced, one at a time, by hosts running the new version. During this process you can continue to index and
            query data in your cluster.
          </p>
        </>
      )}
    </Alert>
  </>
);

export default UpgradeMethodSelector;
