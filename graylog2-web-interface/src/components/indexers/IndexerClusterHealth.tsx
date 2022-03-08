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

import { Spinner } from 'components/common';
import { Row, Col } from 'components/bootstrap';
import { DocumentationLink, SmallSupportLink } from 'components/support';
import DocsHelper from 'util/DocsHelper';
import { IndexerClusterHealthSummary } from 'components/indexers';
import { IndexerClusterActions } from 'stores/indexers/IndexerClusterStore';

import IndexerClusterHealthError from './IndexerClusterHealthError';

const useLoadHealthAndName = () => {
  const [state, setState] = useState({ health: undefined, name: undefined, error: undefined, loading: false });

  useEffect(() => {
    setState({ health: undefined, name: undefined, error: undefined, loading: true });

    Promise.all([
      IndexerClusterActions.health(),
      IndexerClusterActions.name(),
    ]).then(([healthResponse, nameResponse]) => setState({ health: healthResponse, name: nameResponse, error: undefined, loading: false }))
      .catch(
        (error) => {
          setState({ health: undefined, name: undefined, error, loading: false });
        },
      );
  }, [setState]);

  return state;
};

const IndexerClusterHealth = () => {
  const { health, loading, error } = useLoadHealthAndName();

  return (
    <Row className="content">
      <Col md={12}>
        <h2>Elasticsearch cluster</h2>

        <SmallSupportLink>
          The possible Elasticsearch cluster states and more related information is available in the{' '}
          <DocumentationLink page={DocsHelper.PAGES.CONFIGURING_ES} text="Graylog documentation" />.
        </SmallSupportLink>
        {health && <IndexerClusterHealthSummary health={health} />}
        {loading && <Spinner />}
        {error && <IndexerClusterHealthError error={error} />}
      </Col>
    </Row>
  );
};

export default IndexerClusterHealth;
