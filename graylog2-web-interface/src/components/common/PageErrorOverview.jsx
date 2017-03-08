import React from 'react';
import { Col, Jumbotron, Row } from 'react-bootstrap';

import style from '!style/useable!css!pages/NotFoundPage.css';

const PageErrorOverview = React.createClass({
  propTypes: {
    errors: React.PropTypes.array.isRequired,
  },
  componentDidMount() {
    style.use();
  },

  componentWillUnmount() {
    style.unuse();
  },

  _formatErrors(errors) {
    const formattedErrors = errors ? errors.map(error => <li>{error.toString()}</li>) : [];
    return (
      <ul>
        {formattedErrors}
        <li>Check your Graylog logs for more information.</li>
      </ul>
    );
  },
  render() {
    return (
      <Row className="jumbotron-container">
        <Col mdOffset={2} md={8}>
          <Jumbotron>
            <h1>Error getting data</h1>
            <p>We had trouble fetching some data required to build this page, so here is a picture instead.</p>
            {this._formatErrors(this.props.errors)}
          </Jumbotron>
        </Col>
      </Row>
    );
  },
});

export default PageErrorOverview;
