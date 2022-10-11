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

import { LinkContainer } from 'components/common/router';
import Routes from 'routing/Routes';
import { Col, Row, Button } from 'components/bootstrap';
import HideOnCloud from 'util/conditional/HideOnCloud';
import DocsHelper from 'util/DocsHelper';
import { DocumentTitle, PageHeader } from 'components/common';
import { DocumentationLink } from 'components/support';
import { IndexSetsComponent } from 'components/indices';
import { IndexerClusterHealth } from 'components/indexers';

const IndicesPage = () => (
  <DocumentTitle title="Indices and Index Sets">
    <span>
      <PageHeader title="Indices & Index Sets"
                  subactions={(
                    <LinkContainer to={Routes.SYSTEM.INDEX_SETS.CREATE}>
                      <Button bsStyle="success">Create index set</Button>
                    </LinkContainer>
                  )}>
        <span>
          A Graylog stream write messages to an index set, which is a configuration for retention, sharding, and
          replication of the stored data.
          By configuring index sets, you could, for example, have different retention times for certain streams.
        </span>

        <span>
          You can learn more about the index model in the{' '}
          <DocumentationLink page={DocsHelper.PAGES.INDEX_MODEL} text="documentation" />
        </span>
      </PageHeader>

      <HideOnCloud>
        <IndexerClusterHealth minimal />
      </HideOnCloud>

      <Row className="content">
        <Col md={12}>
          <IndexSetsComponent />
        </Col>
      </Row>
    </span>
  </DocumentTitle>
);

export default IndicesPage;
