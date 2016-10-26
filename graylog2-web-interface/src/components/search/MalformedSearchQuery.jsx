import React from 'react';
import { Col, Panel, Row } from 'react-bootstrap';

import { ContactUs, DocumentationLink } from 'components/support';
import DocsHelper from 'util/DocsHelper';

const MalformedSearchQuery = React.createClass({
  propTypes: {
    error: React.PropTypes.object.isRequired,
  },

  _isGenericError(error) {
    return error.begin_column === null
      || error.begin_line === null
      || error.end_column === null
      || error.end_line === null;
  },

  _highlightQueryError(error) {
    if (error.begin_line > 1 || error.begin_line !== error.end_line) {
      return error.query;
    }

    return (
      <span>
        {error.query.substring(0, error.begin_column)}
        <span className="parse-error">{error.query.substring(error.begin_column, error.end_column)}</span>
        {error.query.substring(error.end_column, error.query.length)}
      </span>
    );
  },

  _getFormattedErrorDescription(error) {
    return (
      <Panel bsStyle="danger">
        <dl style={{ marginBottom: 0 }}>
          <dt>Error Message:</dt>
          <dd>{error.message}</dd>
          <dt>Exception:</dt>
          <dd><code>{error.exception_name}</code></dd>
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
          <p>The given query was malformed at the following position:</p>
          <pre>{this._highlightQueryError(error)}</pre>
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
