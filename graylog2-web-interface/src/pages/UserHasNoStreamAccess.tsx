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
// @flow strict
import * as React from 'react';

import { DocumentTitle, Icon } from 'components/common';
import { Alert, Row, Col } from 'components/graylog';
import PageHeader from 'components/common/PageHeader';

import AppWithoutSearchBar from '../routing/AppWithoutSearchBar';

const UserHasNoStreamAccess = () => (
  <AppWithoutSearchBar>
    <DocumentTitle title="No stream permissions.">
      <div>
        <PageHeader title="No stream permissions." />
        <Row className="content">
          <Col md={12}>
            <Alert bsStyle="warning">
              <Icon name="info-circle" />&nbsp;We cannot start a search right now, because you are not allowed to access any stream.
              If you feel this is an error, please contact your administrator.
            </Alert>
          </Col>
        </Row>
      </div>
    </DocumentTitle>
  </AppWithoutSearchBar>
);

export default UserHasNoStreamAccess;
