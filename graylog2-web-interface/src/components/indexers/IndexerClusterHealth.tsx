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
import { useQueries } from '@tanstack/react-query';
import styled from 'styled-components';

import { isPermitted } from 'util/PermissionsMixin';
import { Spinner } from 'components/common';
import { Row, Col } from 'components/bootstrap';
import { DocumentationLink } from 'components/support';
import DocsHelper from 'util/DocsHelper';
import { IndexerClusterHealthSummary } from 'components/indexers';
import type FetchError from 'logic/errors/FetchError';
import ApiRoutes from 'routing/ApiRoutes';
import { fetchPeriodically } from 'logic/rest/FetchProvider';
import * as URLUtils from 'util/URLUtils';
import useCurrentUser from 'hooks/useCurrentUser';

import IndexerClusterHealthError from './IndexerClusterHealthError';

const Header = styled.div`
  display: flex;
  justify-content: space-between;
`;

const GET_INDEXER_CLUSTER_HEALTH = 'indexerCluster.health';
const GET_INDEXER_CLUSTER_NAME = 'indexerCluster.name';

const getIndexerClusterHealth = () => {
  const url = URLUtils.qualifyUrl(ApiRoutes.IndexerClusterApiController.health().url);

  return fetchPeriodically('GET', url);
};

const getIndexerClusterName = () => {
  const url = URLUtils.qualifyUrl(ApiRoutes.IndexerClusterApiController.name().url);

  return fetchPeriodically('GET', url);
};

const useLoadHealthAndName = (enabled: boolean) => {
  const options = { refetchInterval: 5000, retry: 0, enabled };
  const [
    { data: healthData, isFetching: healthIsFetching, error: healthError, isSuccess: healthIsSuccess, isRefetching: healthIsRefetching },
    { data: nameData, isFetching: nameIsFetching, error: nameError, isSuccess: nameIsSuccess, isRefetching: nameIsRefetching },
  ] = useQueries({
    queries: [
      { queryKey: [GET_INDEXER_CLUSTER_HEALTH], queryFn: getIndexerClusterHealth, ...options },
      { queryKey: [GET_INDEXER_CLUSTER_NAME], queryFn: getIndexerClusterName, ...options },
    ],
  });

  return ({
    health: healthData,
    name: nameData,
    error: (healthError || nameError) as FetchError,
    loading: (healthIsFetching || nameIsFetching) && !healthIsRefetching && !nameIsRefetching,
    isSuccess: healthIsSuccess && nameIsSuccess,
  });
};

type Props = {
  minimal?: boolean,
};

const IndexerClusterHealth = ({ minimal }: Props) => {
  const currentUser = useCurrentUser();
  const userHasRequiredPermissions = isPermitted(currentUser.permissions, 'indexercluster:read');
  const { health, name, loading, error, isSuccess } = useLoadHealthAndName(userHasRequiredPermissions);

  if (!userHasRequiredPermissions) {
    return null;
  }

  return (
    <Row className="content">
      <Col md={12}>
        {!minimal && (
          <Header>
            <h2>Elasticsearch cluster</h2>
            <DocumentationLink page={DocsHelper.PAGES.CONFIGURING_ES} text="Elasticsearch setup documentation" displayIcon />
          </Header>
        )}

        {isSuccess && <IndexerClusterHealthSummary health={health} name={name} />}
        {loading && <p><Spinner /></p>}
        {error && <IndexerClusterHealthError error={error} />}
      </Col>
    </Row>
  );
};

IndexerClusterHealth.defaultProps = {
  minimal: false,
};

export default IndexerClusterHealth;
