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
