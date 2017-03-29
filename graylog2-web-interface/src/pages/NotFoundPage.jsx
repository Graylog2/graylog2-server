import React from 'react';
import { Col, Jumbotron, Row } from 'react-bootstrap';
import { DocumentTitle } from 'components/common';

import style from '!style/useable!css!./NotFoundPage.css';

const NotFoundPage = React.createClass({
  componentDidMount() {
    style.use();
  },

  componentWillUnmount() {
    style.unuse();
  },

  render() {
    return (
      <DocumentTitle title="Not Found">
        <Row className="jumbotron-container">
          <Col mdOffset={2} md={8}>
            <Jumbotron>
              <h1>404 - Page not found</h1>
              <p>The URL requested does not exist.</p>
              <p>Please consult the Militus USAM User's Guide!</p>
            </Jumbotron>
          </Col>
        </Row>
      </DocumentTitle>
    );
  },
});

export default NotFoundPage;
