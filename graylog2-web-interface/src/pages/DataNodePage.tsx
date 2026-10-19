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
import styled, { css } from 'styled-components';
import { useTranslation } from 'react-i18next';

import type { DataNode } from 'components/datanode/Types';
import useParams from 'routing/useParams';
import DocsHelper from 'util/DocsHelper';
import { Alert, Row, Col, Label } from 'components/bootstrap';
import { DocumentTitle, NoSearchResult, PageHeader, RelativeTime, Spinner } from 'components/common';
import { CertRenewalButton } from 'components/datanode/DataNodeConfiguration/CertificateRenewal';
import useDataNode from 'components/datanode/hooks/useDataNode';
import DataNodeActions from 'components/datanode/DataNodeList/DataNodeActions';
import ClusterConfigurationPageNavigation from 'components/cluster-configuration/ClusterConfigurationPageNavigation';

const StyledHorizontalDl = styled.dl(
  ({ theme }) => css`
    margin: ${theme.spacings.md} 0;

    > dt {
      clear: left;
      float: left;
      margin-bottom: ${theme.spacings.md};
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      width: 160px;
    }

    > *:not(dt) {
      margin-bottom: ${theme.spacings.md};
      margin-left: 140px;
    }
  `,
);
const ActionsCol = styled(Col)`
  text-align: right;
`;

const DataNodePage = () => {
  const { t } = useTranslation('datanode');
  const { dataNodeId } = useParams();
  const { data, isInitialLoading, error, refetch } = useDataNode(dataNodeId);

  if (isInitialLoading) {
    return <Spinner />;
  }

  if (!isInitialLoading && (!data || error)) {
    return <NoSearchResult>{t('details_page.error', { message: error?.message })}</NoSearchResult>;
  }

  const datanode = data as DataNode;
  const datanodeDisabled = datanode.datanode_status !== 'AVAILABLE';

  return (
    <DocumentTitle title={t('details_page.title', { hostname: datanode.hostname })}>
      <ClusterConfigurationPageNavigation />
      <PageHeader
        title={t('details_page.title', { hostname: datanode.hostname })}
        documentationLink={{
          title: t('pages.documentation_title'),
          path: DocsHelper.PAGES.GRAYLOG_DATA_NODE,
        }}
      />
      <Row className="content">
        <Col xs={12}>
          <Col xs={9}>
            <h2>{t('details_page.details')}</h2>
            <StyledHorizontalDl>
              <dt>{t('details_page.hostname')}</dt>
              <dd>{datanode.hostname}</dd>
              <dt>{t('details_page.transport_address')}</dt>
              <dd>{datanode.transport_address || '-'}</dd>
              <dt>{t('details_page.status')}</dt>
              <dd>
                <Label
                  bsStyle={datanodeDisabled ? 'warning' : 'success'}
                  title={datanode.datanode_status}
                  aria-label={datanode.datanode_status}
                  role="button">
                  {datanode.datanode_status || t('details_page.not_available')}
                </Label>
              </dd>
              <dt>{t('details_page.certificate_valid_until')}</dt>
              <dd>
                <RelativeTime dateTime={datanode.cert_valid_until} />{' '}
                <CertRenewalButton nodeId={datanode.node_id} status={datanode.status} />
              </dd>
              <dt>{t('details_page.datanode_version')}</dt>
              <dd>{datanode.datanode_version}</dd>
            </StyledHorizontalDl>
          </Col>
          <ActionsCol xs={3}>
            <DataNodeActions dataNode={datanode} refetch={refetch} displayAs="buttons" />
          </ActionsCol>
        </Col>
        {datanode.configuration_warnings?.length > 0 && (
          <Col xs={12}>
            <Alert bsStyle="warning" title={t('details_page.configuration_warnings')}>
              <ul>
                {datanode.configuration_warnings.map((warning) => (
                  <li key={warning}>{warning}</li>
                ))}
              </ul>
            </Alert>
          </Col>
        )}
      </Row>
    </DocumentTitle>
  );
};

export default DataNodePage;
