import React from 'react';

import { Row, Col } from 'components/graylog';
import RolesComponent from 'components/users/RolesComponent';
import { DocumentTitle } from 'components/common';

class RolesPage extends React.Component {
  render() {
    return (
      <DocumentTitle title="Roles">
        <Row>
          <Col md={12}>
            <RolesComponent />
          </Col>
        </Row>
      </DocumentTitle>
    );
  }
}

export default RolesPage;
