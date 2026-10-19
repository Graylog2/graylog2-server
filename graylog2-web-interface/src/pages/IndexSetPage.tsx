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
import React, { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import numeral from 'numeral';

import HideOnCloud from 'util/conditional/HideOnCloud';
import { LinkContainer, DocumentTitle, PageHeader, Spinner, Icon } from 'components/common';
import { Alert, Row, Col, Panel, Button, ButtonToolbar } from 'components/bootstrap';
import useProductName from 'brand-customization/useProductName';
import {
  IndicesConfigurationDropdown,
  IndicesMaintenanceDropdown,
  IndicesOverview,
  IndexSetDetails,
  IndicesPageNavigation,
} from 'components/indices';
import { IndexerClusterHealthSummary } from 'components/indexers';
import DocsHelper from 'util/DocsHelper';
import Routes from 'routing/Routes';
import useParams from 'routing/useParams';
import type { IndexSet } from 'stores/indices/IndexSetsStore';
import { fetchIndexSet } from 'stores/indices/IndexSetsStore';
import useIndexerOverview from 'hooks/useIndexerOverview';
import { useIndices } from 'hooks/useIndices';

const REFRESH_INTERVAL = 2000;

const ElasticsearchUnavailableInformation = () => {
  const { t } = useTranslation('indexsets');
  const productName = useProductName();

  return (
    <Row className="content">
      <Col md={8} mdOffset={2}>
        <div className="top-margin">
          <Panel
            bsStyle="danger"
            header={
              <span>
                <Icon name="warning" /> {t('overview.elasticsearch_unavailable.title')}
              </span>
            }>
            <p>
              {t('overview.elasticsearch_unavailable.description', { productName })}
            </p>
            <p>
              {t('overview.elasticsearch_unavailable.journal_info')}
            </p>
          </Panel>
        </div>
      </Col>
    </Row>
  );
};

const IndexSetPage = () => {
  const { t } = useTranslation('indexsets');
  const { indexSetId } = useParams<{ indexSetId?: string }>();
  const [indexSet, setIndexSet] = useState<IndexSet | undefined>(undefined);
  const { data: indexerOverview, error: indexerOverviewError, refetch } = useIndexerOverview(indexSetId);
  const { data: indicesData } = useIndices(indexSetId);
  const { indices: indexDetailsIndices, closedIndices: indexDetailsClosedIndices } = indicesData ?? {};

  useEffect(() => {
    fetchIndexSet(indexSetId).then((response) => setIndexSet(response));

    const timerId = setInterval(() => {
      refetch();
    }, REFRESH_INTERVAL);

    return () => {
      clearInterval(timerId);
    };
  }, [indexSetId, refetch]);

  if (!indexSet) {
    return <Spinner />;
  }

  const pageHeader = (
    <PageHeader
      title={t('overview.header.index_set_title', { title: indexSet.title })}
      documentationLink={{
        title: t('overview.header.documentation_link'),
        path: DocsHelper.PAGES.INDEX_MODEL,
      }}
      actions={
        <ButtonToolbar>
          <LinkContainer to={Routes.SYSTEM.INDEX_SETS.CONFIGURATION(indexSet.id, 'details')}>
            <Button bsStyle="info">{t('overview.header.edit_button')}</Button>
          </LinkContainer>
          <IndicesMaintenanceDropdown indexSetId={indexSetId} indexSet={indexSet} />
          <IndicesConfigurationDropdown indexSetId={indexSetId} />
        </ButtonToolbar>
      }>
      <span>{t('overview.header.description')}</span>
    </PageHeader>
  );

  if (indexerOverviewError) {
    return (
      <span>
        {pageHeader}
        <ElasticsearchUnavailableInformation />
      </span>
    );
  }

  let indicesInfo;
  let indicesOverview;

  if (indexerOverview && indexDetailsClosedIndices) {
    const deflectorInfo = indexerOverview.deflector;

    indicesInfo = (
      <span>
        <Alert bsStyle="success" style={{ marginTop: '10' }}>
          {t('overview.indices_info.summary', {
            count: indexerOverview.indices.length,
            messageCount: numeral(indexerOverview.counts.events).format('0,0'),
            currentTarget: deflectorInfo.current_target,
          })}
        </Alert>
        <HideOnCloud>
          <IndexerClusterHealthSummary health={indexerOverview.indexer_cluster.health} />
        </HideOnCloud>
      </span>
    );

    indicesOverview = (
      <IndicesOverview indices={indexerOverview.indices} indexDetails={indexDetailsIndices} indexSetId={indexSetId} />
    );
  } else {
    indicesInfo = <Spinner />;
    indicesOverview = <Spinner />;
  }

  return (
    <DocumentTitle title={t('overview.document_title', { title: indexSet?.title ?? '' })}>
      <IndicesPageNavigation />
      <div>
        {pageHeader}

        <Row className="content">
          <Col md={12}>
            <IndexSetDetails indexSet={indexSet} />
          </Col>
        </Row>

        <Row className="content">
          <Col md={12}>{indicesInfo}</Col>
        </Row>

        {indicesOverview}
      </div>
    </DocumentTitle>
  );
};

export default IndexSetPage;
