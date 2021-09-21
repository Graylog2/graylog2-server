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
import React, { useEffect, useRef, useState } from 'react';

import { LinkContainer } from 'components/graylog/router';
import { Row, Col, Button } from 'components/graylog';
import { SearchForm, PaginatedList, DocumentTitle, PageHeader } from 'components/common';
import DocumentationLink from 'components/support/DocumentationLink';
import DocsHelper from 'util/DocsHelper';
import RulesComponent from 'components/rules/RulesComponent';
import Routes from 'routing/Routes';
import CombinedProvider from 'injection/CombinedProvider';
import { Pagination, DEFAULT_PAGINATION } from 'stores/PaginationTypes';
import { PaginatedRules } from 'stores/rules/RulesStore';

const { RulesActions } = CombinedProvider.get('Rules');

const CreateRuleButton = () => (
  <div className="pull-right">
    <LinkContainer to={Routes.SYSTEM.PIPELINES.RULE('new')}>
      <Button bsStyle="success">Create Rule</Button>
    </LinkContainer>
  </div>
);

const _loadData = (pagination: Pagination, setIsLoading, setPaginatedRules) => {
  setIsLoading(true);

  RulesActions.listPage(pagination).then((paginatedRules) => {
    setPaginatedRules(paginatedRules);
    setIsLoading(false);
  });
};

const RulesPage = () => {
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [pagination, setPagination] = useState<Pagination>(DEFAULT_PAGINATION);
  const resetSearchIsLoading = useRef<() => void | undefined>();
  const [paginatedRules, setPaginatedRules] = useState<PaginatedRules | undefined>();
  const { list: rules, pagination: { total = 0 } = {} } = paginatedRules ?? {};

  useEffect(() => {
    _loadData(pagination, setIsLoading, setPaginatedRules);
  }, [pagination]);

  useEffect(() => {
    if (!isLoading && resetSearchIsLoading.current) {
      resetSearchIsLoading.current();
      resetSearchIsLoading.current = undefined;
    }
  }, [isLoading, resetSearchIsLoading]);

  const handlePageChange = (newPage, newPerPage) => {
    setPagination({ ...pagination, page: newPage, perPage: newPerPage });
  };

  const handleSearch = (query, resetLoadingCallback) => {
    setPagination({ ...pagination, query: query });
    resetSearchIsLoading.current = resetLoadingCallback;
  };

  const handleReset = () => {
    setPagination({ ...pagination, query: '' });
  };

  const handleDelete = (rule) => {
    return () => {
      if (window.confirm(`Do you really want to delete rule "${rule.title}"?`)) {
        RulesActions.delete(rule).then(() => {
          _loadData(pagination, setIsLoading, setPaginatedRules);
        });
      }
    };
  };

  return (
    <DocumentTitle title="Pipeline rules">
      <span>
        <PageHeader title="Pipeline Rules" subactions={<CreateRuleButton />}>
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
                  <SearchForm onSearch={handleSearch} onReset={handleReset} useLoadingState />
                </Col>
              </Row>
              <Row>
                <Col md={12}>
                  <PaginatedList onChange={handlePageChange} totalItems={total}>
                    <RulesComponent rules={rules} onDelete={handleDelete} />
                  </PaginatedList>
                </Col>
              </Row>
            </div>
          </Col>
        </Row>
      </span>
    </DocumentTitle>
  );
};

export default RulesPage;
