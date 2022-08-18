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
import React, { useEffect, useState } from 'react';
import styled, { css } from 'styled-components';

import { LinkContainer } from 'components/common/router';
import { Row, Col, Button, ButtonToolbar } from 'components/bootstrap';
import { SearchForm, PaginatedList, DocumentTitle, PageHeader, Spinner, QueryHelper } from 'components/common';
import DocumentationLink from 'components/support/DocumentationLink';
import DocsHelper from 'util/DocsHelper';
import RuleList from 'components/rules/RuleList';
import RuleMetricsConfigContainer from 'components/rules/RuleMetricsConfigContainer';
import Routes from 'routing/Routes';
import { DEFAULT_PAGINATION } from 'stores/PaginationTypes';
import type { Pagination } from 'stores/PaginationTypes';
import type { MetricsConfigType, PaginatedRules, RuleType } from 'stores/rules/RulesStore';
import { RulesActions } from 'stores/rules/RulesStore';
import useLocationSearchPagination from 'hooks/useLocationSearchPagination';

const Flex = styled.div`
  display: flex;
`;

const SpinnerWrapper = styled.div(({ theme }) => css`
  font-size: ${theme.fonts.size.h3};
  padding: ${theme.spacings.xxs} ${theme.spacings.sm};
`);

const _loadData = (pagination: Pagination, setIsLoading, setPaginatedRules) => {
  setIsLoading(true);

  RulesActions.listPaginated(pagination).then((paginatedRules) => {
    setPaginatedRules(paginatedRules);
    setIsLoading(false);
  });
};

const _loadRuleMetricData = (setMetricsConfig) => {
  RulesActions.loadMetricsConfig().then((metricsConfig: MetricsConfigType) => {
    setMetricsConfig(metricsConfig);
  });
};

const RulesPage = () => {
  const [isDataLoading, setIsDataLoading] = useState<boolean>(false);
  const [openMetricsConfig, toggleMetricsConfig] = useState<boolean>(false);
  const [metricsConfig, setMetricsConfig] = useState<MetricsConfigType>();
  const { isInitialized: isPaginationReady, pagination, setPagination } = useLocationSearchPagination(DEFAULT_PAGINATION);
  const [paginatedRules, setPaginatedRules] = useState<PaginatedRules | undefined>();
  const { list: rules, pagination: { total = 0, count = 0 } = {}, context: rulesContext } = paginatedRules ?? {};
  const { page, perPage, query } = pagination;

  useEffect(() => {
    if (isPaginationReady) {
      _loadData(pagination, setIsDataLoading, setPaginatedRules);
    }
  }, [isPaginationReady, pagination]);

  useEffect(() => {
    _loadRuleMetricData(setMetricsConfig);
  }, []);

  const handlePageChange = (newPage, newPerPage) => {
    setPagination({ ...pagination, page: newPage, perPage: newPerPage });
  };

  const handleSearch = (nextQuery) => {
    setPagination({ ...pagination, query: nextQuery, page: DEFAULT_PAGINATION.page });
  };

  const handleDelete = (rule: RuleType) => {
    return () => {
      // TODO: Replace with custom confirm dialog
      // eslint-disable-next-line no-alert
      if (window.confirm(`Do you really want to delete rule "${rule.title}"?`)) {
        RulesActions.delete(rule).then(() => {
          if (count > 1) {
            _loadData(pagination, setIsDataLoading, setPaginatedRules);

            return;
          }

          setPagination({ page: Math.max(DEFAULT_PAGINATION.page, pagination.page - 1), perPage, query });
        });
      }
    };
  };

  const onCloseMetricsConfig = () => {
    _loadRuleMetricData(setMetricsConfig);
    toggleMetricsConfig(false);
  };

  const renderDebugMetricsButton = () => {
    if (metricsConfig && metricsConfig.metrics_enabled) {
      return <Button bsStyle="warning" onClick={toggleMetricsConfig}>Debug Metrics: ON</Button>;
    }

    return <Button onClick={toggleMetricsConfig}>Debug Metrics</Button>;
  };

  const RulesButtonToolbar = () => (
    <ButtonToolbar className="pull-right">
      <LinkContainer to={Routes.SYSTEM.PIPELINES.RULE('new')}>
        <Button bsStyle="success">Create Rule</Button>
      </LinkContainer>
      {renderDebugMetricsButton()}
    </ButtonToolbar>
  );

  const isLoading = !rules;

  const searchFilter = (
    <Flex>
      <SearchForm query={query}
                  onSearch={handleSearch}
                  queryWidth={400}
                  queryHelpComponent={<QueryHelper entityName="Pipeline Rule" />}
                  wrapperClass="has-bm"
                  onReset={() => handleSearch('')}
                  topMargin={0} />
      {isDataLoading && <SpinnerWrapper><Spinner text="" delay={0} /></SpinnerWrapper>}
    </Flex>
  );

  return (
    <DocumentTitle title="Pipeline rules">
      <span>
        <PageHeader title="Pipeline Rules" subactions={<RulesButtonToolbar />}>
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
            {isLoading ? (
              <Spinner />
            ) : (
              <Row>
                <Col md={12}>
                  <PaginatedList onChange={handlePageChange} totalItems={total} activePage={page} pageSize={perPage}>
                    <RuleList rules={rules} rulesContext={rulesContext} onDelete={handleDelete} searchFilter={searchFilter} />
                    {openMetricsConfig && <RuleMetricsConfigContainer onClose={onCloseMetricsConfig} />}
                  </PaginatedList>
                </Col>
              </Row>
            )}
          </Col>
        </Row>
      </span>
    </DocumentTitle>
  );
};

export default RulesPage;
