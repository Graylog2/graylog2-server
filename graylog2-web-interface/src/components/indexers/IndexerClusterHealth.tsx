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
import { useQueries } from 'react-query';

import { Spinner } from 'components/common';
import { Row, Col } from 'components/bootstrap';
import { DocumentationLink, SmallSupportLink } from 'components/support';
import DocsHelper from 'util/DocsHelper';
import { IndexerClusterHealthSummary } from 'components/indexers';
import { getIndexerClusterHealth, getIndexerClusterName } from 'components/indexers/api';
import type FetchError from 'logic/errors/FetchError';
import { GET_INDEXER_CLUSTER_HEALTH, GET_INDEXER_CLUSTER_NAME } from 'logic/reactQueryKeys';

import IndexerClusterHealthError from './IndexerClusterHealthError';

const useLoadHealthAndName = () => {
  const [
    { data: healthData, isFetching: healthIsFetching, error: healthError, isSuccess: healthIsSuccess },
    { data: nameData, isFetching: nameIsFetching, error: nameError, isSuccess: nameIsSuccess },
  ] = useQueries([
    { queryKey: GET_INDEXER_CLUSTER_HEALTH, queryFn: getIndexerClusterHealth },
    { queryKey: GET_INDEXER_CLUSTER_NAME, queryFn: getIndexerClusterName },
  ]);

  return ({
    health: healthData,
    name: nameData,
    error: (healthError || nameError) as FetchError,
    loading: healthIsFetching || nameIsFetching,
    isSuccess: healthIsSuccess && nameIsSuccess,
  });
};

const IndexerClusterHealth = () => {
  const { health, name, loading, error, isSuccess } = useLoadHealthAndName();

  return (
    <Row className="content">
      <Col md={12}>
        <h2>Elasticsearch cluster</h2>

        <SmallSupportLink>
          The possible Elasticsearch cluster states and more related information is available in the{' '}
          <DocumentationLink page={DocsHelper.PAGES.CONFIGURING_ES} text="Graylog documentation" />.
        </SmallSupportLink>
        {isSuccess && <IndexerClusterHealthSummary health={health} name={name} />}
        {loading && <Spinner />}
        {error && <IndexerClusterHealthError error={error} />}
      </Col>
    </Row>
  );
};

export default IndexerClusterHealth;
