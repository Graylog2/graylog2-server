import React from 'react';
import { Row, Col } from 'react-bootstrap';

import RolesComponent from 'components/users/RolesComponent';
import { DocumentTitle } from 'components/common';

const RolesPage = React.createClass({
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
  },
});

export default RolesPage;
