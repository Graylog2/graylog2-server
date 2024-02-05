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

import PipelinesPageNavigation from 'components/pipelines/PipelinesPageNavigation';
import DocsHelper from 'util/DocsHelper';
import { Row, Col, Button, ButtonToolbar } from 'components/bootstrap';
import { SearchForm, PaginatedList, DocumentTitle, PageHeader, Spinner, QueryHelper } from 'components/common';
import RuleList from 'components/rules/RuleList';
import RuleMetricsConfigContainer from 'components/rules/RuleMetricsConfigContainer';
import { DEFAULT_PAGINATION } from 'stores/PaginationTypes';
import type { Pagination } from 'stores/PaginationTypes';
import type { MetricsConfigType, PaginatedRules, RuleType } from 'stores/rules/RulesStore';
import { RulesActions } from 'stores/rules/RulesStore';
import usePaginationQueryParameter from 'hooks/usePaginationQueryParameter';
import { getPathnameWithoutId } from 'util/URLUtils';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import useLocation from 'routing/useLocation';
import useHistory from 'routing/useHistory';
import Routes from 'routing/Routes';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

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
  const { page, pageSize: perPage, resetPage, setPagination } = usePaginationQueryParameter();
  const { pathname } = useLocation();
  const history = useHistory();
  const sendTelemetry = useSendTelemetry();
  const [query, setQuery] = useState('');
  const [isDataLoading, setIsDataLoading] = useState<boolean>(false);
  const [openMetricsConfig, toggleMetricsConfig] = useState<boolean>(false);
  const [metricsConfig, setMetricsConfig] = useState<MetricsConfigType>();
  const [paginatedRules, setPaginatedRules] = useState<PaginatedRules | undefined>();
  const { list: rules, pagination: { total = 0 } = {}, context: rulesContext } = paginatedRules ?? {};

  useEffect(() => {
    _loadData({ query, page, perPage }, setIsDataLoading, setPaginatedRules);
  }, [query, page, perPage]);

  useEffect(() => {
    _loadRuleMetricData(setMetricsConfig);
  }, []);

  const handleSearch = (nextQuery) => {
    resetPage();
    setQuery(nextQuery);
  };

  const handleDelete = (rule: RuleType) => () => {
    // TODO: Replace with custom confirm dialog
    // eslint-disable-next-line no-alert
    if (window.confirm(`Do you really want to delete rule "${rule.title}"?`)) {
      RulesActions.delete(rule).then(() => {
        _loadData({ query, page, perPage }, setIsDataLoading, setPaginatedRules);
        setPagination({ page: Math.max(DEFAULT_PAGINATION.page, page - 1) });
      });
    }
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

  // eslint-disable-next-line react/no-unstable-nested-components
  const RulesButtonToolbar = () => (
    <ButtonToolbar className="pull-right">
      <Button bsStyle="success"
              onClick={() => {
                sendTelemetry(TELEMETRY_EVENT_TYPE.PIPELINE_RULE_BUILDER.CREATE_RULE_CLICKED, {
                  app_pathname: getPathnameWithoutId(pathname),
                  app_section: 'pipeline-rules',
                  app_action_value: 'create-rule-button',
                });

                history.push(`${Routes.SYSTEM.PIPELINES.RULE('new')}?rule_builder=true`);
              }}>
        Create Rule
      </Button>
      {renderDebugMetricsButton()}
    </ButtonToolbar>
  );

  const isLoading = !rules;

  const searchFilter = (
    <Flex>
      <SearchForm query={query}
                  onSearch={handleSearch}
                  queryHelpComponent={<QueryHelper entityName="Pipeline Rule" />}
                  wrapperClass="has-bm"
                  onReset={() => handleSearch('')}
                  topMargin={0} />
      {isDataLoading && <SpinnerWrapper><Spinner text="" delay={0} /></SpinnerWrapper>}
    </Flex>
  );

  return (
    <DocumentTitle title="Pipeline rules">
      <PipelinesPageNavigation />
      <PageHeader title="Pipeline Rules"
                  actions={<RulesButtonToolbar />}
                  documentationLink={{
                    title: 'Pipeline rules documentation',
                    path: DocsHelper.PAGES.PIPELINE_RULES,
                  }}>
        <span>
          Rules are a way of applying changes to messages in Graylog. A rule consists of a condition and a list of actions.
          Graylog evaluates the condition against a message and executes the actions if the condition is satisfied.
        </span>
      </PageHeader>

      <Row className="content">
        <Col md={12}>
          {isLoading ? (
            <Spinner />
          ) : (
            <Row>
              <Col md={12}>
                <PaginatedList totalItems={total}>
                  <RuleList rules={rules}
                            rulesContext={rulesContext}
                            onDelete={handleDelete}
                            searchFilter={searchFilter} />
                  {openMetricsConfig && <RuleMetricsConfigContainer onClose={onCloseMetricsConfig} />}
                </PaginatedList>
              </Col>
            </Row>
          )}
        </Col>
      </Row>
    </DocumentTitle>
  );
};

export default RulesPage;
