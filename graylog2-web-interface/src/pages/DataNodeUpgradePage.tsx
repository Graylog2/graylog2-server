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
import { Trans, useTranslation } from 'react-i18next';

import useProductName from 'brand-customization/useProductName';
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

const OPEN_SEARCH_SECTION_STATUSES = ['outdated', 'upgrading', 'unconfirmed'] as const;

const DataNodeDocsLink = ({ children = undefined }: React.PropsWithChildren) => (
  <DocumentationLink text={children} page={DocsHelper.PAGES.GRAYLOG_DATA_NODE} />
);

const UpgradeInstructionsDocumentationMessage = () => {
  const { t } = useTranslation('datanode');

  return (
    <p>
      <Trans t={t} i18nKey="upgrade_page.manual_instructions" components={{ docsLink: <DataNodeDocsLink /> }} />
    </p>
  );
};

const DataNodeUpgradePage = () => {
  const { t } = useTranslation('datanode');
  const productName = useProductName();
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
    await startShardReplication();
    setOpenUpgradeConfirmDialog(false);
  };

  const manualUpgradeAlert = (nodeInProgress: string) => (
    <Alert bsStyle="warning">
      <p>
        <Trans
          t={t}
          i18nKey={nodeInProgress ? 'upgrade_page.manual_upgrade_of_node' : 'upgrade_page.manual_upgrade_of_data_node'}
          values={{ node: nodeInProgress }}
          components={{
            bold: <b />,
            confirmButton: <Button onClick={confirmNodeUpgrade} bsStyle="link" bsSize="large" />,
          }}
        />
      </p>
      <UpgradeInstructionsDocumentationMessage />
    </Alert>
  );

  const nodeInProgress = getNodeToUpgrade();

  const numberOfNodes = (data?.outdated_nodes?.length || 0) + (data?.up_to_date_nodes?.length || 0);

  const isRollingUpgradePossible = numberOfNodes >= 3;
  const showRollingUpgrade = upgradeMethod === 'rolling-upgrade' && (!!nodeInProgress || isRollingUpgradePossible);
  const areAllDataNodeVersionsUpToDate = !data?.outdated_nodes?.length && (data?.up_to_date_nodes?.length ?? 0) > 0;
  const isOpenSearchPhase =
    openSearchStatus === 'upgrading' || (areAllDataNodeVersionsUpToDate && data?.shard_replication_enabled === true);
  const showOpenSearchUpgradeSection =
    isOpenSearchPhase && OPEN_SEARCH_SECTION_STATUSES.some((status) => status === openSearchStatus);

  return (
    <DocumentTitle title={t('pages.upgrade_title')}>
      <ClusterConfigurationPageNavigation />
      <PageHeader
        title={t('pages.upgrade_title')}
        documentationLink={{
          title: t('pages.documentation_title'),
          path: DocsHelper.PAGES.GRAYLOG_DATA_NODE,
        }}>
        <span>{t('pages.description', { productName })}</span>
      </PageHeader>
      {isInitialLoading ? (
        <Spinner />
      ) : (
        <Row className="content">
          <Col xs={12}>
            {!isOpenSearchPhase && (
              <>
                <Title order={1}>{t('upgrade_page.select_strategy')}</Title>
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
                <Title order={1}>{t('upgrade_page.upgrade_data_node')}</Title>
                <br />
              </>
            )}
            <ClusterHealthInfo
              data={data}
              numberOfNodes={numberOfNodes}
              showShardReplication={upgradeMethod === 'rolling-upgrade'}
            />
            {!isOpenSearchPhase && showRollingUpgrade && (
              <DataNodeUpgradeNodes
                outdatedNodes={data?.outdated_nodes ?? []}
                upToDateNodes={data?.up_to_date_nodes ?? []}
                upgradedListRef={upgradeListRef}
                onStartNodeUpgrade={startNodeUpgrade}
              />
            )}
            {isOpenSearchPhase && (
              <UpgradeStatusAlert
                currentOpenSearchVersion={currentOpenSearchVersion}
                status={openSearchStatus}
                unavailableDataNodeCount={unavailableDataNodeCount}
              />
            )}
            {showOpenSearchUpgradeSection && <OpenSearchUpgradeSection />}
            {!isOpenSearchPhase && openUpgradeConfirmDialog && nodeInProgress && (
              <Modal
                show
                backdrop={false}
                onHide={() => setOpenUpgradeConfirmDialog(false)}
                rootProps={{ lockScroll: false }}>
                <Modal.Header>
                  <Modal.Title>{t('upgrade_page.manual_upgrade_title')}</Modal.Title>
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
