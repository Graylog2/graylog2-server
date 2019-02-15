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
import createReactClass from 'create-react-class';
import Reflux from 'reflux';

import { LinkContainer } from 'components/graylog/router';
import { Row, Col, Button } from 'components/graylog';
import { SearchForm, PaginatedList, DocumentTitle, PageHeader } from 'components/common';
import DocumentationLink from 'components/support/DocumentationLink';
import DocsHelper from 'util/DocsHelper';
import RulesComponent from 'components/rules/RulesComponent';
import Routes from 'routing/Routes';
import CombinedProvider from 'injection/CombinedProvider';

import RulesPageStyle from './RulesPage.css';

const { RulesStore, RulesActions } = CombinedProvider.get('Rules');

const RulesPage = createReactClass({
  displayName: 'RulesPage',

  mixins: [
    Reflux.connect(RulesStore),
  ],

  componentDidMount() {
    this.loadData();
  },

  loadData(callback) {
    const { page, perPage, query } = this.state.pagination;

    RulesActions.listPage(page, perPage, query).then(() => {
      if (callback) {
        callback();
      }
    });
  },

  _onPageChange(newPage, newPerPage) {
    const { pagination } = this.state;
    const newPagination = Object.assign(pagination, {
      page: newPage,
      perPage: newPerPage,
    });
    this.setState({ pagination, newPagination }, this.loadData);
  },

  _onSearch(query, resetLoadingCallback) {
    const { pagination } = this.state;
    const newPagination = Object.assign(pagination, { query: query });
    this.setState({ pagination, newPagination }, () => this.loadData(resetLoadingCallback));
  },

  _onReset() {
    const { pagination } = this.state;
    const newPagination = Object.assign(pagination, { query: '' });
    this.setState({ pagination, newPagination }, this.loadData);
  },

  _delete(rule) {
    return () => {
      if (window.confirm(`Do you really want to delete rule "${rule.title}"?`)) {
        RulesActions.delete(rule).then(() => {
          this.loadData();
        });
      }
    };
  },

  render() {
    const addRuleButton = (
      <div className={RulesPageStyle.addButton}>
        <LinkContainer to={Routes.SYSTEM.PIPELINES.RULE('new')}>
          <Button bsStyle="success">Create Rule</Button>
        </LinkContainer>
      </div>
    );

    return (
      <DocumentTitle title="Pipeline rules">
        <span>
          <PageHeader title="Pipeline Rules">
            <span>
              Rules are a way of applying changes to messages in Graylog. A rule consists of a condition and a list of actions.
              Graylog evaluates the condition against a message and executes the actions if the condition is satisfied.
            </span>

            <span>
              Read more about Graylog pipeline rules in the <DocumentationLink page={DocsHelper.PAGES.PIPELINE_RULES}
                                                                               text="documentation" />.
            </span>

            <span>
              <LinkContainer to={Routes.SYSTEM.PIPELINES.OVERVIEW}>
                <Button bsStyle="info">Manage pipelines</Button>
              </LinkContainer>
              &nbsp;
              <LinkContainer to={Routes.SYSTEM.PIPELINES.RULES}>
                <Button bsStyle="info">Manage rules</Button>
              </LinkContainer>
              &nbsp;
              <LinkContainer to={Routes.SYSTEM.PIPELINES.SIMULATOR}>
                <Button bsStyle="info">Simulator</Button>
              </LinkContainer>
            </span>
          </PageHeader>

          <Row className="content">
            <Col md={12}>
              <div>
                <Row className="row-sm">
                  <Col md={2}>
                    <SearchForm onSearch={this._onSearch} onReset={this._onReset} useLoadingState />
                  </Col>
                  <Col>
                    {addRuleButton}
                  </Col>
                </Row>
                <Row>
                  <Col md={12}>
                    <PaginatedList onChange={this._onPageChange} totalItems={this.state.pagination.total}>
                      <br />
                      <br />
                      <RulesComponent rules={this.state.rules} onDelete={this._delete} />
                    </PaginatedList>
                  </Col>
                </Row>
              </div>
            </Col>
          </Row>
        </span>
      </DocumentTitle>
    );
  },
});

export default RulesPage;
