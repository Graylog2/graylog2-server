import PropTypes from 'prop-types';
import React from "react";
import {Col, Panel, Row} from "react-bootstrap";

import {ContactUs} from "components/support";

class SearchExecutionError extends React.Component {
  static propTypes = {
    error: PropTypes.object.isRequired,
  };

  _getFormattedErrorDetails = (details) => {
      return details.map(function(detail) {
          return <li><code>{detail}</code></li>
      });
  };

  render() {
    const error = this.props.error;
    return (
      <div>
        <Row className="content content-head">
          <Col md={12}>

            <h1>
              Could not execute search
            </h1>

            <div>
              <p>There was an error executing your search. Please check your Graylog server logs for more information.</p>
              <Panel bsStyle="danger">
                <dl style={{ marginBottom: 0 }}>
                  <dt>Error Message:</dt>
                  <dd>{error.body.message ? error.body.message : ''}</dd>
                  <dt>Details:</dt>
                  <dd>{error.body.message ? this._getFormattedErrorDetails(error.body.details) : ''}</dd>
                  <dt>Search status code:</dt>
                  <dd>{error.status}</dd>
                  <dt>Search response:</dt>
                  <dd>{error.message}</dd>
                </dl>
              </Panel>
            </div>
          </Col>
        </Row>

        <ContactUs />
      </div>
    );
  }
}

export default SearchExecutionError;
