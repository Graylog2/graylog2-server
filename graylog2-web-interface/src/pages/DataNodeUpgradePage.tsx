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
import React, { useState, useRef } from 'react';

import { Row, Col, Button, Alert, Modal } from 'components/bootstrap';
import { DocumentTitle, PageHeader, Spinner, Title } from 'components/common';
import DocsHelper from 'util/DocsHelper';
import useDataNodeUpgradeStatus, {
  getNodeToUpgrade,
  saveNodeToUpgrade,
  startShardReplication,
  stopShardReplication,
} from 'components/datanode/hooks/useDataNodeUpgradeStatus';
import type { DataNodeInformation } from 'components/datanode/hooks/useDataNodeUpgradeStatus';
import ClusterConfigurationPageNavigation from 'components/cluster-configuration/ClusterConfigurationPageNavigation';
import DocumentationLink from 'components/support/DocumentationLink';
import OpenSearchUpgradeSection from 'components/datanode/opensearch-upgrade/OpenSearchUpgradeSection';
import useOpenSearchClusterStats from 'components/datanode/opensearch-upgrade/hooks/useOpenSearchClusterStats';
import useOpenSearchUpgradeStatus from 'components/datanode/opensearch-upgrade/hooks/useOpenSearchUpgradeStatus';
import DataNodeUpgradeNodes from 'components/datanode/data-node-upgrade/DataNodeUpgradeNodes';
import UpgradeMethodSelector, {
  type DataNodeUpgradeMethodType,
} from 'components/datanode/data-node-upgrade/UpgradeMethodSelector';
import ClusterHealthInfo from 'components/datanode/data-node-upgrade/ClusterHealthInfo';
import UpgradeStatusAlert from 'components/datanode/data-node-upgrade/UpgradeStatusAlert';

const upgradeInstructionsDocumentationMessage = (
  <p>
    To upgrade your Data Nodes manually, please follow the instructions in the&nbsp;
    <DocumentationLink text="documentation" page={DocsHelper.PAGES.GRAYLOG_DATA_NODE} />.
  </p>
);

const DataNodeUpgradePage = () => {
  const upgradeListRef = useRef<HTMLTableSectionElement>(null);

  const { data, isInitialLoading } = useDataNodeUpgradeStatus();
  const { currentVersion: currentOpenSearchVersion, unavailableDataNodeCount } = useOpenSearchClusterStats();
  const openSearchStatus = useOpenSearchUpgradeStatus();
  const [upgradeMethod, setUpgradeMethod] = useState<DataNodeUpgradeMethodType>('cluster-restart');
  const [openUpgradeConfirmDialog, setOpenUpgradeConfirmDialog] = useState<boolean>(false);

  const scrollIntoDataNodeUpgradedList = () => {
    if (!isInitialLoading && upgradeListRef.current) {
      upgradeListRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  };

  const startNodeUpgrade = async (node: DataNodeInformation) => {
    scrollIntoDataNodeUpgradedList();
    saveNodeToUpgrade(node?.hostname);
    setOpenUpgradeConfirmDialog(true);
    stopShardReplication();
  };

  const confirmNodeUpgrade = async () => {
    startShardReplication();
    setOpenUpgradeConfirmDialog(false);
  };

  const manualUpgradeAlert = (nodeInProgress: string) => (
    <Alert bsStyle="warning">
      <p>
        Once you have completed the manual upgrade of {nodeInProgress ? <b>{nodeInProgress}</b> : 'your Data Node'} on
        the system, wait until it reconnects and apears in the <b>Upgraded Nodes</b> panel, then click on&nbsp;
        <Button onClick={confirmNodeUpgrade} bsStyle="link" bsSize="large">
          <b>Confirm Upgrade</b>
        </Button>
        &nbsp; and continue with next node.
      </p>
      {upgradeInstructionsDocumentationMessage}
    </Alert>
  );

  const nodeInProgress = getNodeToUpgrade();

  const numberOfNodes = (data?.outdated_nodes?.length || 0) + (data?.up_to_date_nodes?.length || 0);

  const isRollingUpgradePossible = numberOfNodes >= 3;
  const showRollingUpgrade = upgradeMethod === 'rolling-upgrade' && (!!nodeInProgress || isRollingUpgradePossible);
  const areAllDataNodesUpToDate = !data?.outdated_nodes?.length && (data?.up_to_date_nodes?.length ?? 0) > 0;
  const showOpenSearchUpgradeSection =
    areAllDataNodesUpToDate && ['outdated', 'upgrading', 'unconfirmed'].includes(openSearchStatus);

  return (
    <DocumentTitle title="Data Node Upgrade">
      <ClusterConfigurationPageNavigation />
      <PageHeader
        title="Data Node Upgrade"
        documentationLink={{
          title: 'Data Nodes documentation',
          path: DocsHelper.PAGES.GRAYLOG_DATA_NODE,
        }}>
        <span>
          Graylog Data Nodes offer a better integration with Graylog and simplify future updates. They allow you to
          index and search through all the messages in your Graylog message database.
        </span>
      </PageHeader>
      {isInitialLoading ? (
        <Spinner />
      ) : (
        <Row className="content">
          <Col xs={12}>
            {!areAllDataNodesUpToDate && (
              <>
                <Title order={1}>Select Upgrade Strategy</Title>
                <br />
                <UpgradeMethodSelector upgradeMethod={upgradeMethod} onChange={setUpgradeMethod} />
                {!data?.shard_replication_enabled && manualUpgradeAlert(nodeInProgress)}
                {(data?.warnings?.length || 0) > 0 && (
                  <Alert bsStyle="danger">
                    {data.warnings.map((warning) => (
                      <p key={warning}>{warning}</p>
                    ))}
                  </Alert>
                )}
              </>
            )}
            {showOpenSearchUpgradeSection && (
              <>
                <Title order={1}>Upgrade Data Node</Title>
                <br />
              </>
            )}
            <ClusterHealthInfo
              data={data}
              numberOfNodes={numberOfNodes}
              showShardReplication={upgradeMethod === 'rolling-upgrade'}
            />
            {!areAllDataNodesUpToDate && showRollingUpgrade && (
              <DataNodeUpgradeNodes
                outdatedNodes={data?.outdated_nodes ?? []}
                upToDateNodes={data?.up_to_date_nodes ?? []}
                upgradedListRef={upgradeListRef}
                onStartNodeUpgrade={startNodeUpgrade}
              />
            )}
            {areAllDataNodesUpToDate && (
              <UpgradeStatusAlert
                currentOpenSearchVersion={currentOpenSearchVersion}
                status={openSearchStatus}
                unavailableDataNodeCount={unavailableDataNodeCount}
              />
            )}
            {showOpenSearchUpgradeSection && <OpenSearchUpgradeSection />}
            {openUpgradeConfirmDialog && nodeInProgress && (
              <Modal
                show
                backdrop={false}
                onHide={() => setOpenUpgradeConfirmDialog(false)}
                rootProps={{ lockScroll: false }}>
                <Modal.Header>
                  <Modal.Title>Data Node Manual Upgrade</Modal.Title>
                </Modal.Header>

                <Modal.Body>{manualUpgradeAlert(nodeInProgress)}</Modal.Body>
              </Modal>
            )}
          </Col>
        </Row>
      )}
    </DocumentTitle>
  );
};

export default DataNodeUpgradePage;
