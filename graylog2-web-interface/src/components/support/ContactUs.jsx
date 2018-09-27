import React from 'react';
import { Col, Row } from 'react-bootstrap';

import SupportSources from './SupportSources';

class ContactUs extends React.Component {
  render() {
    return (
      <Row className="content">
        <Col md={12}>
          <SupportSources />
        </Col>
      </Row>
    );
  }
}

export default ContactUs;
