import React from 'react';
import { Col, Panel, Row } from 'react-bootstrap';

import { ContactUs } from 'components/support';

const SearchExecutionError = React.createClass({
  propTypes: {
    error: React.PropTypes.object.isRequired,
  },

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
                  <dd>{error.body.message ? `${error.body.message}: ` : ''}{error.message}</dd>
                  <dt>Search status code:</dt>
                  <dd>{error.status}</dd>
                </dl>
              </Panel>
            </div>
          </Col>
        </Row>

        <ContactUs />
      </div>
    );
  },
});

export default SearchExecutionError;
