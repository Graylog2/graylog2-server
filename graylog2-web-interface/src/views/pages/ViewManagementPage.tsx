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

import { useStore } from 'stores/connect';
import { LinkContainer } from 'components/graylog/router';
import Routes from 'routing/Routes';
import { Col, Row, Button } from 'components/graylog';
import { DocumentTitle, PageHeader } from 'components/common';
import { ViewManagementStore, ViewManagementActions } from 'views/stores/ViewManagementStore';
import ViewList from 'views/components/views/ViewList';

const handleSearch = (query, page, perPage) => {
  return ViewManagementActions.search(query, page, perPage);
};

const handleViewDelete = (view) => {
  // eslint-disable-next-line no-alert
  if (window.confirm(`Are you sure you want to delete "${view.title}"?`)) {
    return ViewManagementActions.delete(view);
  }

  return null;
};

const ViewManagementPage = () => {
  const { list, pagination } = useStore(ViewManagementStore);

  return (
    <DocumentTitle title="Views">
      <span>
        <PageHeader title="Views">
          <span>
            Graylog view management.
          </span>

          {null}

          <span>
            <LinkContainer to={Routes.EXTENDEDSEARCH}>
              <Button bsStyle="success" bsSize="lg">Create new view</Button>
            </LinkContainer>
          </span>
        </PageHeader>

        <Row className="content">
          <Col md={12}>
            <ViewList views={list}
                      pagination={pagination}
                      handleSearch={handleSearch}
                      handleViewDelete={handleViewDelete} />
          </Col>
        </Row>
      </span>
    </DocumentTitle>
  );
};

export default ViewManagementPage;
