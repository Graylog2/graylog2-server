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

import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import { Row, Col, Button } from 'components/bootstrap';
import EntityGroupsList from 'components/entity-groups/EntityGroupsList';
import { useGetEntityGroups } from 'components/entity-groups/hooks/useEntityGroups';

const EntityGroupsPage = () => {
  const { data: entityGroupsList, isInitialLoading } = useGetEntityGroups();
  const [showAddEntityGroup, setShowAddEntityGroup] = React.useState(false);

  return (
    <DocumentTitle title="Manage Entity Groups">
      <PageHeader title="Manage Entity Groups"
                  actions={(
                    <Button bsStyle="success" onClick={() => setShowAddEntityGroup(true)}>Create a new entity group</Button>
                )}>
        <span>
          Manage the tags/categories of your content.
        </span>
      </PageHeader>
      <Row className="content">
        <Col md={12}>
          {isInitialLoading ? (
            <Spinner />
          ) : (
            <EntityGroupsList entityGroups={entityGroupsList} showAddEntityGroup={showAddEntityGroup} setShowAddEntityGroup={(value) => setShowAddEntityGroup(value)} />
          )}
        </Col>
      </Row>
    </DocumentTitle>
  );
};

export default EntityGroupsPage;
