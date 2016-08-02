import React from 'react';
import { Row, Col } from 'react-bootstrap';

import RolesComponent from 'components/users/RolesComponent';

const RolesPage = React.createClass({
  render() {
    return (
      <Row>
        <Col md={12}>
          <RolesComponent />
        </Col>
      </Row>
    );
  },
});

export default RolesPage;
