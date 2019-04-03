import React from 'react';
import createReactClass from 'create-react-class';
import { Row, Col, Button } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';

import Reflux from 'reflux';

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
    const pagination = this.state.pagination;
    const newPagination = Object.assign(pagination, {
      page: newPage,
      perPage: newPerPage,
    });
    this.setState({ pagination, newPagination }, this.loadData);
  },

  _onSearch(query, resetLoadingCallback) {
    const pagination = this.state.pagination;
    const newPagination = Object.assign(pagination, { query: query });
    this.setState({ pagination, newPagination }, () => this.loadData(resetLoadingCallback));
  },

  _onReset() {
    const pagination = this.state.pagination;
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
      </div>);

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
                <Button bsStyle="info" className="active">Manage rules</Button>
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
