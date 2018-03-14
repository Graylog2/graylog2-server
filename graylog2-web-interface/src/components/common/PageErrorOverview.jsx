import PropTypes from 'prop-types';
import React from 'react';
import { Col, Jumbotron, Row } from 'react-bootstrap';

import style from '!style/useable!css!pages/NotFoundPage.css';

/**
 * Component that renders a page when there was an error and certain information can't be fetched. Use it
 * only when the page would make no sense if the information is not available (i.e. a node page where we
 * can't reach the node).
 */
class PageErrorOverview extends React.Component {
  static propTypes = {
    /** Array of errors that prevented the original page to load. */
    errors: PropTypes.array.isRequired,
  };

  componentDidMount() {
    style.use();
  }

  componentWillUnmount() {
    style.unuse();
  }

  _formatErrors = (errors) => {
    const formattedErrors = errors ? errors.map(error => <li key={`key-${error.toString()}`}>{error.toString()}</li>) : [];
    return (
      <ul>
        {formattedErrors}
        <li>Check your Graylog logs for more information.</li>
      </ul>
    );
  };

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
  }
}

export default PageErrorOverview;
