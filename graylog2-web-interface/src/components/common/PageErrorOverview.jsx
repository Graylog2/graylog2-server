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
    const formattedErrors = errors ? errors.map((error) => <li>{error.toString()}</li>) : [];
    return (
      <ul>
        {formattedErrors}
      </ul>
    );
  },
  render() {
    return (
      <Row className="jumbotron-container">
        <Col mdOffset={2} md={8}>
          <Jumbotron>
            <h1>Error fetching data</h1>
            <p>We had trouble fetching some data required to build this page.</p>
            {this._formatErrors(this.props.errors)}
          </Jumbotron>
        </Col>
      </Row>
    );
  },
});

export default PageErrorOverview;
