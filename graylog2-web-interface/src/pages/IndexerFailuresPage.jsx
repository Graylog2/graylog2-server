import React from 'react';
import { Col, Row } from 'react-bootstrap';
import numeral from 'numeral';
import moment from 'moment';

import StoreProvider from 'injection/StoreProvider';
const IndexerFailuresStore = StoreProvider.getStore('IndexerFailures');

import DocsHelper from 'util/DocsHelper';

import { DocumentTitle, Spinner, PageHeader, PaginatedList } from 'components/common';
import { DocumentationLink } from 'components/support';
import { IndexerFailuresList } from 'components/indexers';

const IndexerFailuresPage = React.createClass({
  getInitialState() {
    return {};
  },
  componentDidMount() {
    IndexerFailuresStore.count(moment().subtract(10, 'years')).then((response) => {
      this.setState({ total: response.count });
    });
    this.loadData(1, this.defaultPageSize);
  },
  defaultPageSize: 50,
  loadData(page, size) {
    IndexerFailuresStore.list(size, (page - 1) * size).then((response) => {
      this.setState({ failures: response.failures });
    });
  },
  _onChangePaginatedList(page, size) {
    this.loadData(page, size);
  },
  render() {
    if (this.state.total === undefined || !this.state.failures) {
      return <Spinner />;
    }
    return (
      <DocumentTitle title="Indexer failures">
        <span>
          <PageHeader title="Indexer failures">
            <span>
              This is a list of message index attempts that failed. A failure means that a message you sent to Graylog was{' '}
              properly processed but writing it to the Elasticsearch cluster failed. Note that the list is capped to a size{' '}
              of 50 MB so it will contain a lot of failure logs but not necessarily all that ever occurred.
            </span>

            <span>
              Collection containing a total of {numeral(this.state.total).format('0,0')} indexer failures. Read more about
              this topic in the <DocumentationLink page={DocsHelper.PAGES.INDEXER_FAILURES} text="documentation" />.
            </span>
          </PageHeader>
          <Row className="content">
            <Col md={12}>
              <PaginatedList totalItems={this.state.total} onChange={this._onChangePaginatedList} pageSize={this.defaultPageSize}>
                <IndexerFailuresList failures={this.state.failures} />
              </PaginatedList>
            </Col>
          </Row>
        </span>
      </DocumentTitle>
    );
  },
});

export default IndexerFailuresPage;
