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
import { useStore } from 'stores/connect';
import type { IndexSet } from 'stores/indices/IndexSetsStore';
import { fetchIndexSet } from 'stores/indices/IndexSetsStore';
import useIndexerOverview from 'hooks/useIndexerOverview';
import { IndicesActions, IndicesStore } from 'stores/indices/IndicesStore';

const REFRESH_INTERVAL = 2000;

const ElasticsearchUnavailableInformation = () => {
  const productName = useProductName();

  return (
    <Row className="content">
      <Col md={8} mdOffset={2}>
        <div className="top-margin">
          <Panel
            bsStyle="danger"
            header={
              <span>
                <Icon name="warning" /> Indices overview unavailable
              </span>
            }>
            <p>
              We could not get the indices overview information. This usually means there was a problem connecting to
              the indexer, and <strong>you should ensure the indexer is up and reachable from {productName}</strong>.
            </p>
            <p>
              Messages will continue to be stored in the journal, but searching on them will not be possible until the
              indexer is reachable again.
            </p>
          </Panel>
        </div>
      </Col>
    </Row>
  );
};

const IndexSetPage = () => {
  const { indexSetId } = useParams<{ indexSetId?: string }>();
  const [indexSet, setIndexSet] = useState<IndexSet | undefined>(undefined);
  const { data: indexerOverview, error: indexerOverviewError, refetch } = useIndexerOverview(indexSetId);
  const { indices: indexDetailsIndices, closedIndices: indexDetailsClosedIndices } = useStore(IndicesStore) ?? {};

  useEffect(() => {
    fetchIndexSet(indexSetId).then((response) => setIndexSet(response));
    IndicesActions.list(indexSetId);

    const timerId = setInterval(() => {
      IndicesActions.multiple();
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
      title={`Index Set: ${indexSet.title}`}
      documentationLink={{
        title: 'Index model documentation',
        path: DocsHelper.PAGES.INDEX_MODEL,
      }}
      actions={
        <ButtonToolbar>
          <LinkContainer to={Routes.SYSTEM.INDEX_SETS.CONFIGURATION(indexSet.id, 'details')}>
            <Button bsStyle="info">Edit Index Set</Button>
          </LinkContainer>
          <IndicesMaintenanceDropdown indexSetId={indexSetId} indexSet={indexSet} />
          <IndicesConfigurationDropdown indexSetId={indexSetId} />
        </ButtonToolbar>
      }>
      <span>
        This is an overview of all indices (message stores) in this index set currently being considered for searches
        and analysis.
      </span>
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
          {indexerOverview.indices.length} indices with a total of{' '}
          {numeral(indexerOverview.counts.events).format('0,0')} messages under management, current write-active index
          is <i>{deflectorInfo.current_target}</i>.
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
    <DocumentTitle title={`Index Set - ${indexSet ? indexSet.title : ''}`}>
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
