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
import styled from 'styled-components';

import {
  DataTable,
  Icon,
  IfPermitted,
  PageHeader,
  PaginatedList,
  SearchForm,
} from 'components/common';
import { Button, Col, Row, OverlayTrigger } from 'components/graylog';
import EditPatternModal from 'components/grok-patterns/EditPatternModal';
import BulkLoadPatternModal from 'components/grok-patterns/BulkLoadPatternModal';
import StoreProvider from 'injection/StoreProvider';

import GrokPatternQueryHelper from './GrokPatternQueryHelper';

const GrokPatternsStore = StoreProvider.getStore('GrokPatterns');

const GrokPatternsList = styled(DataTable)`
  th.name {
    min-width: 200px;
  }

  td {
    word-break: break-all;
  }
`;

class GrokPatterns extends React.Component {
  constructor(props) {
    super(props);

    this.state = {
      patterns: [],
      pagination: {
        page: 1,
        perPage: 10,
        count: 0,
        total: 0,
        query: '',
      },
    };
  }

  componentDidMount() {
    this.loadData();
  }

  componentWillUnmount() {
    if (this.loadPromise) {
      this.loadPromise.cancel();
    }
  }

  loadData = (callback) => {
    const { pagination: { page, perPage, query } } = this.state;

    this.loadPromise = GrokPatternsStore.searchPaginated(page, perPage, query)
      .then(({ patterns, pagination }) => {
        if (callback) {
          callback();
        }

        if (!this.loadPromise.isCancelled()) {
          this.loadPromise = undefined;

          this.setState({ patterns, pagination });
        }
      });
  };

  validPatternName = (name) => {
    // Check if patterns already contain a pattern with the given name.
    const { patterns } = this.state;

    return !patterns.some((pattern) => pattern.name === name);
  };

  savePattern = (pattern, callback) => {
    GrokPatternsStore.savePattern(pattern, () => {
      callback();
      this.loadData();
    });
  };

  testPattern = (pattern, callback, errCallback) => {
    GrokPatternsStore.testPattern(pattern, callback, errCallback);
  };

  _onPageChange = (newPage, newPerPage) => {
    const { pagination } = this.state;
    const newPagination = Object.assign(pagination, {
      page: newPage,
      perPage: newPerPage,
    });
    this.setState({ pagination: newPagination }, this.loadData);
  };

  _onSearch = (query, resetLoadingCallback) => {
    const { pagination } = this.state;
    const newPagination = Object.assign(pagination, { query: query });
    this.setState({ pagination: newPagination }, () => this.loadData(resetLoadingCallback));
  };

  _onReset = () => {
    const { pagination } = this.state;
    const newPagination = Object.assign(pagination, { query: '' });
    this.setState({ pagination: newPagination }, this.loadData);
  };

  confirmedRemove = (pattern) => {
    // eslint-disable-next-line no-alert
    if (window.confirm(`Really delete the grok pattern ${pattern.name}?\nIt will be removed from the system and unavailable for any extractor. If it is still in use by extractors those will fail to work.`)) {
      GrokPatternsStore.deletePattern(pattern, this.loadData);
    }
  };

  _headerCellFormatter = (header) => {
    let formattedHeaderCell;

    switch (header.toLocaleLowerCase()) {
      case 'name':
        formattedHeaderCell = <th className="name">{header}</th>;
        break;
      case 'actions':
        formattedHeaderCell = <th className="actions">{header}</th>;
        break;
      default:
        formattedHeaderCell = <th>{header}</th>;
    }

    return formattedHeaderCell;
  };

  _patternFormatter = (pattern) => {
    const { patterns: unfilteredPatterns } = this.state;
    const patterns = unfilteredPatterns.filter((p) => p.name !== pattern.name);

    return (
      <tr key={pattern.id}>
        <td>{pattern.name}</td>
        <td>{pattern.pattern}</td>
        <td>
          <IfPermitted permissions="inputs:edit">
            <Button style={{ marginRight: 5 }}
                    bsStyle="primary"
                    bsSize="xs"
                    onClick={() => this.confirmedRemove(pattern)}>
              Delete
            </Button>
            <EditPatternModal id={pattern.id}
                              name={pattern.name}
                              pattern={pattern.pattern}
                              testPattern={this.testPattern}
                              patterns={patterns}
                              create={false}
                              reload={this.loadData}
                              savePattern={this.savePattern}
                              validPatternName={this.validPatternName} />
          </IfPermitted>
        </td>
      </tr>
    );
  };

  render() {
    const headers = ['Name', 'Pattern', 'Actions'];
    const { pagination, patterns } = this.state;

    const queryHelperComponent = (
      <OverlayTrigger trigger="click" rootClose placement="bottom" overlay={<GrokPatternQueryHelper />}>
        <Button bsStyle="link" className="archive-search-help-button">
          <Icon name="question-circle" fixedWidth />
        </Button>
      </OverlayTrigger>
    );

    return (
      <div>
        <PageHeader title="Grok patterns">
          <span>
            This is a list of grok patterns you can use in your Graylog grok extractors. You can add
            your own manually or import a whole list of patterns from a so called pattern file.
          </span>
          {null}
          <IfPermitted permissions="inputs:edit">
            <span>
              <BulkLoadPatternModal onSuccess={this.loadData} />
              <EditPatternModal id=""
                                name=""
                                pattern=""
                                patterns={patterns}
                                create
                                testPattern={this.testPattern}
                                reload={this.loadData}
                                savePattern={this.savePattern}
                                validPatternName={this.validPatternName} />
            </span>
          </IfPermitted>
        </PageHeader>

        <Row className="content">
          <Col md={12}>
            <IfPermitted permissions="inputs:read">
              <Row className="row-sm">
                <Col md={8}>
                  <SearchForm onSearch={this._onSearch}
                              onReset={this._onReset}
                              queryHelpComponent={queryHelperComponent}
                              useLoadingState />
                </Col>
              </Row>
              <Row>
                <Col md={12}>
                  <PaginatedList onChange={this._onPageChange}
                                 totalItems={pagination.total}>
                    <GrokPatternsList id="grok-pattern-list"
                                      className="table-striped table-hover"
                                      headers={headers}
                                      headerCellFormatter={this._headerCellFormatter}
                                      sortByKey="name"
                                      rows={patterns}
                                      dataRowFormatter={this._patternFormatter} />
                  </PaginatedList>
                </Col>
              </Row>
            </IfPermitted>
          </Col>
        </Row>
      </div>
    );
  }
}

export default GrokPatterns;
