import React from 'react';
import { Col, Panel, Row } from 'react-bootstrap';

import { ContactUs, DocumentationLink } from 'components/support';
import DocsHelper from 'util/DocsHelper';

const MalformedSearchQuery = React.createClass({
  propTypes: {
    error: React.PropTypes.object.isRequired,
  },

  _isGenericError(error) {
    return error.column === null || error.line === null;
  },

  _getFormattedErrorDetails(details) {
    return details.map(function(detail) {
        return <li><code>{detail}</code></li>
    });
  },

  _getFormattedErrorDescription(error) {
    return (
      <Panel bsStyle="danger">
        <dl style={{ marginBottom: 0 }}>
          <dt>Error Message:</dt>
          <dd>{error.message}</dd>
          <dt>Details:</dt>
          <dd>{this._getFormattedErrorDetails(error.details)}</dd>
        </dl>
      </Panel>
    );
  },

  render() {
    const error = this.props.error.body;

    let explanation;
    if (this._isGenericError(error)) {
      explanation = (
        <div>
          <p>The given query was malformed, and executing it caused the following error:</p>
          {this._getFormattedErrorDescription(error)}
        </div>
      );
    } else {
      explanation = (
        <div>
          {this._getFormattedErrorDescription(error)}
        </div>
      );
    }

    return (
      <div>
        <Row className="content content-head">
          <Col md={12}>

            <h1>
              Malformed search query
            </h1>

            <p className="description">
              The search query could not be executed, please correct it and try again.{' '}
              <strong>
                Take a look at the{' '}
                <DocumentationLink page={DocsHelper.PAGES.SEARCH_QUERY_LANGUAGE} text="documentation" />{' '}
                if you need help with the search syntax.
              </strong>
            </p>
          </Col>
        </Row>

        <Row className="content">
          <Col md={12}>
            {explanation}
          </Col>
        </Row>

        <ContactUs />
      </div>
    );
  },
});

export default MalformedSearchQuery;
