/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import React from 'react';
import PropTypes from 'prop-types';
import numeral from 'numeral';
import moment from 'moment';

import { Col, Row } from 'components/bootstrap';
import DocsHelper from 'util/DocsHelper';
import { DocumentTitle, Spinner, PageHeader, PaginatedList } from 'components/common';
import { DocumentationLink } from 'components/support';
import { IndexerFailuresList } from 'components/indexers';
import withPaginationQueryParameter from 'components/common/withPaginationQueryParameter';
import { IndexerFailuresStore } from 'stores/indexers/IndexerFailuresStore';

class IndexerFailuresPage extends React.Component {
  static propTypes = {
    paginationQueryParameter: PropTypes.object.isRequired,
  };

  constructor(props) {
    super(props);

    this.state = {};
  }

  componentDidMount() {
    IndexerFailuresStore.count(moment().subtract(10, 'years')).then((response) => {
      this.setState({ total: response.count });
    });

    this.loadData();
  }

  loadData = (page = this.props.paginationQueryParameter.page, size = this.props.paginationQueryParameter.pageSize) => {
    IndexerFailuresStore.list(size, (page - 1) * size).then((response) => {
      this.setState({ failures: response.failures });
    });
  };

  _onChangePaginatedList = (page, size) => {
    this.loadData(page, size);
  };

  render() {
    if (this.state.total === undefined || !this.state.failures) {
      return <Spinner />;
    }

    return (
      <DocumentTitle title="Indexer failures">
        <span>
          <PageHeader title="Indexer failures"
                      documentationLink={{
                        title: 'Indexer failures documentation',
                        path: DocsHelper.PAGES.INDEXER_FAILURES,
                      }}>
            <span>
              This is a list of message index attempts that failed. A failure means that a message you sent to Graylog was{' '}
              properly processed but writing it to the Elasticsearch cluster failed. Note that the list is capped to a size{' '}
              of 50 MB so it will contain a lot of failure logs but not necessarily all that ever occurred.
              <br />
              Collection containing a total of {numeral(this.state.total).format('0,0')} indexer failures.
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
  }
}

export default withPaginationQueryParameter(IndexerFailuresPage);
