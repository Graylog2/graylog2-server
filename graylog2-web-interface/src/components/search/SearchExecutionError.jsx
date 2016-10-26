import React from 'react';
import { Col, Panel, Row } from 'react-bootstrap';

import DocumentationLink from 'components/support/DocumentationLink';
import DocsHelper from 'util/DocsHelper';

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

        <Row className="content">
          <Col md={12}>
            <div className="support-sources">
              <h2>Need help?</h2>
              <p>
                Do not hesitate to consult the Graylog community if your questions are not answered in the{' '}
                <DocumentationLink page={DocsHelper.PAGES.WELCOME} text="documentation" />.
              </p>

              <ul>
                <li>
                  <i className="fa fa-group" />&nbsp;
                  <a href="https://www.graylog.org/community-support/" target="_blank">Community support</a>
                </li>
                <li>
                  <i className="fa fa-github-alt" />&nbsp;
                  <a href="https://github.com/Graylog2/graylog2-server/issues" target="_blank">Issue tracker</a>
                </li>
                <li>
                  <i className="fa fa-heart" />&nbsp;
                  <a href="https://www.graylog.org/professional-support" target="_blank">Professional support</a>
                </li>
              </ul>
            </div>
          </Col>
        </Row>
      </div>
    );
  },
});

export default SearchExecutionError;
