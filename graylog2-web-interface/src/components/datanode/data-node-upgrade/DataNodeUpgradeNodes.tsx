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
import { useTranslation } from 'react-i18next';

import { Button, Col, Label, Row, Table } from 'components/bootstrap';
import { Icon } from 'components/common';
import { isDataNodeAvailable } from 'components/datanode/hooks/useDataNodeUpgradeStatus';
import type { DataNodeInformation } from 'components/datanode/hooks/useDataNodeUpgradeStatus';

type Props = {
  outdatedNodes: DataNodeInformation[];
  upToDateNodes: DataNodeInformation[];
  upgradedListRef: React.Ref<HTMLTableSectionElement>;
  onStartNodeUpgrade: (node: DataNodeInformation) => void;
};

const NodeIdentityCell = ({ node }: { node: DataNodeInformation }) => {
  const { t } = useTranslation('datanode');

  return (
    <td>
      <div>
        {node?.hostname}&nbsp;
        <Label bsStyle={isDataNodeAvailable(node) ? 'success' : 'warning'} bsSize="xs">
          {node?.data_node_status}
        </Label>
        &nbsp;
        {node?.manager_node && (
          <Label bsStyle="info" bsSize="xs">
            {t('upgrade_nodes.manager')}
          </Label>
        )}
      </div>
      <div>
        <i>{node?.ip}</i>
      </div>
    </td>
  );
};

const DataNodeUpgradeNodes = ({ outdatedNodes, upToDateNodes, upgradedListRef, onStartNodeUpgrade }: Props) => {
  const { t } = useTranslation('datanode');

  return (
    <>
      <br />
      <Row>
        <Col sm={6}>
          <h3>{t('upgrade_nodes.outdated_nodes')}</h3>
          <br />
          <Table>
            <tbody>
              {outdatedNodes?.map((node) => (
                <tr key={node?.hostname}>
                  <NodeIdentityCell node={node} />
                  <td>
                    <i>{node?.datanode_version}</i>
                  </td>
                  <td align="right">
                    <Button
                      onClick={() => onStartNodeUpgrade(node)}
                      disabled={!node?.upgrade_possible}
                      bsSize="sm"
                      bsStyle="primary">
                      {t('upgrade_nodes.start_upgrade')}
                    </Button>
                  </td>
                </tr>
              ))}
              {!outdatedNodes?.length && (
                <tr>
                  <td>{t('upgrade_nodes.no_outdated_nodes')}</td>
                </tr>
              )}
            </tbody>
          </Table>
        </Col>
        <Col sm={6}>
          <h3>{t('upgrade_nodes.upgraded_nodes')}</h3>
          <br />
          <Table>
            <tbody ref={upgradedListRef}>
              {upToDateNodes?.map((node) => (
                <tr key={node?.hostname}>
                  <NodeIdentityCell node={node} />
                  <td>
                    <i>{node?.datanode_version}</i>
                  </td>
                  <td align="right">
                    <Label bsStyle="success" bsSize="xs">
                      {t('upgrade_nodes.upgraded')} <Icon name="check" />
                    </Label>
                  </td>
                </tr>
              ))}
              {!upToDateNodes?.length && (
                <tr>
                  <td>{t('upgrade_nodes.no_upgraded_nodes')}</td>
                </tr>
              )}
            </tbody>
          </Table>
        </Col>
      </Row>
    </>
  );
};

export default DataNodeUpgradeNodes;
