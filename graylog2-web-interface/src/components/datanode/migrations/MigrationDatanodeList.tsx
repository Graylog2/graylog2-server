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
import * as React from 'react';
import styled from 'styled-components';
import { Trans, useTranslation } from 'react-i18next';

import { Icon, Spinner, Timestamp } from 'components/common';
import { Alert, Table } from 'components/bootstrap';
import { DocumentationLink } from 'components/support';
import useDataNodes from 'components/datanode/hooks/useDataNodes';
import useProductName from 'brand-customization/useProductName';

type Props = {
  showProvisioningState?: boolean;
};

const StyledIcon = styled(Icon)`
  margin-right: 0.5em;
`;

const DataNodeDocsLink = ({ children = undefined }: React.PropsWithChildren) => (
  <DocumentationLink page="graylog-data-node" text={children} />
);

const MigrationDatanodeList = ({ showProvisioningState = true }: Props) => {
  const { t } = useTranslation('collectors', { keyPrefix: 'datanode' });
  const { data: dataNodes, isLoading } = useDataNodes();
  const productName = useProductName();

  if (isLoading) {
    return <Spinner text={t('migration_data_nodes.loading')} />;
  }

  return (
    <div>
      {!dataNodes || dataNodes?.list.length === 0 ? (
        <>
          <p>
            <StyledIcon name="info" />
            {t('migration_data_nodes.none_found')}
          </p>
          <Alert bsStyle="warning" title={t('migration_data_nodes.none_found_title')}>
            <Trans
              t={t}
              i18nKey="migration_data_nodes.start_data_node"
              components={{ docsLink: <DataNodeDocsLink /> }}
            />
          </Alert>
          <p>
            <Spinner text={t('migration_data_nodes.looking')} />
          </p>
        </>
      ) : (
        <>
          <h4>{t('migration_data_nodes.found', { count: dataNodes?.list.length })}</h4>
          {dataNodes.list.find((datanode) => !datanode.version_compatible) && (
            <Alert bsStyle="warning" title={t('migration_data_nodes.incompatible_title')}>
              {t('migration_data_nodes.incompatible', { productName })}
            </Alert>
          )}
          <br />
          <Table bordered condensed>
            <thead>
              <tr>
                <th>{t('migration_data_nodes.hostname')}</th>
                <th>{t('migration_data_nodes.transport_address')}</th>
                <th>{t('migration_data_nodes.status')}</th>
                <th>{t('migration_data_nodes.certificate_valid_until')}</th>
                <th>{t('migration_data_nodes.version')}</th>
              </tr>
            </thead>
            <tbody>
              {dataNodes.list.map((datanode) => (
                <tr key={datanode.id}>
                  <td>{datanode.hostname}</td>
                  <td>{datanode.transport_address}</td>
                  <td>{showProvisioningState ? datanode.status : datanode.datanode_status}</td>
                  <td>
                    {datanode.cert_valid_until ? (
                      <Timestamp dateTime={datanode.cert_valid_until} />
                    ) : (
                      t('migration_data_nodes.no_certificate')
                    )}
                  </td>
                  <td>
                    {!datanode.version_compatible && (
                      <Icon name="warning" title={t('migration_data_nodes.version_incompatible', { productName })} />
                    )}
                    {datanode.datanode_version}
                  </td>
                </tr>
              ))}
            </tbody>
          </Table>
        </>
      )}
    </div>
  );
};

export default MigrationDatanodeList;
