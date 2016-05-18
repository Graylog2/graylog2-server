import React from 'react';
import { Col, Row } from 'react-bootstrap';

import { AddSearchCountToDashboard, ShowQueryModal } from 'components/search';
import DocumentationLink from 'components/support/DocumentationLink';

import DocsHelper from 'util/DocsHelper';

const NoSearchResults = React.createClass({
  propTypes: {
    builtQuery: React.PropTypes.string,
    permissions: React.PropTypes.array.isRequired,
    searchInStream: React.PropTypes.object,
  },

  _showQueryModal(event) {
    event.preventDefault();
    this.refs.showQueryModal.open();
  },

  render() {
    let streamDescription = null;
    if (this.props.searchInStream) {
      streamDescription = <span>in stream <em>{this.props.searchInStream.title}</em></span>;
    }

    return (
      <div>
        <Row className="content content-head">
          <Col md={12}>
            <h1>
              <span className="pull-right">
                <AddSearchCountToDashboard searchInStream={this.props.searchInStream} permissions={this.props.permissions} pullRight/>
              </span>
              <span>Nothing found {streamDescription}</span>
            </h1>

            <p className="description">
              Your search returned no results, try changing the used time range or the search query.{' '}
              Do you want more details? <a href="#" onClick={this._showQueryModal}>Show the Elasticsearch query</a>.
              <ShowQueryModal key="debugQuery" ref="showQueryModal" builtQuery={this.props.builtQuery} />
              <br />
              <strong>
                Take a look at the{' '}
                <DocumentationLink page={DocsHelper.PAGES.SEARCH_QUERY_LANGUAGE} text="documentation" />{' '}
                if you need help with the search syntax or the time range selector.
              </strong>
            </p>
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
                  <i className="fa fa-group"/>&nbsp;
                  <a href="https://www.graylog.org/community-support/" target="_blank">Community support</a>
                </li>
                <li>
                  <i className="fa fa-github-alt"/>&nbsp;
                  <a href="https://github.com/Graylog2/graylog2-server/issues" target="_blank">Issue tracker</a>
                </li>
                <li>
                  <i className="fa fa-heart"/>&nbsp;
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

export default NoSearchResults;
