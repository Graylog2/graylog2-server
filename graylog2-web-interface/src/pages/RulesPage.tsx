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
import React, { useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import styled, { css } from 'styled-components';

import PipelinesPageNavigation from 'components/pipelines/PipelinesPageNavigation';
import {
  EnableDebugMetricsButton,
  ProcessingLoadDebugMetricsBanner,
  ProcessingLoadProvider,
} from 'components/pipelines/processing-load';
import DocsHelper from 'util/DocsHelper';
import { Row, Col, ButtonToolbar } from 'components/bootstrap';
import { SearchForm, PaginatedList, DocumentTitle, PageHeader, Spinner, QueryHelper } from 'components/common';
import RuleList from 'components/rules/RuleList';
import { DEFAULT_PAGINATION } from 'stores/PaginationTypes';
import type { RuleType } from 'components/rules/hooks/useRules';
import { useRulesPaginated, deleteRule, RULES_QUERY_KEY } from 'components/rules/hooks/useRules';
import usePaginationQueryParameter from 'hooks/usePaginationQueryParameter';
import CreateButton from 'components/common/CreateButton';

const Flex = styled.div`
  display: flex;
`;

const SpinnerWrapper = styled.div(
  ({ theme }) => css`
    font-size: ${theme.fonts.size.h3};
    padding: ${theme.spacings.xxs} ${theme.spacings.sm};
  `,
);

const rulesButtonToolbar = (
  <ButtonToolbar className="pull-right">
    <EnableDebugMetricsButton />
    <CreateButton entityKey="Pipeline Rule" />
  </ButtonToolbar>
);

const RulesPage = () => {
  const { page, pageSize: perPage, resetPage, setPagination } = usePaginationQueryParameter();
  const queryClient = useQueryClient();
  const [query, setQuery] = useState('');
  const { data: paginatedRules, isFetching: isDataLoading } = useRulesPaginated({ query, page, perPage });
  const { list: rules, pagination: { total = 0 } = {}, context: rulesContext } = paginatedRules ?? {};

  const handleSearch = (nextQuery: string) => {
    resetPage();
    setQuery(nextQuery);
  };

  const handleDelete = (rule: RuleType) => () => {
    // TODO: Replace with custom confirm dialog
    // eslint-disable-next-line no-alert
    if (window.confirm(`Do you really want to delete rule "${rule.title}"?`)) {
      deleteRule(rule)
        .then(() => {
          queryClient.invalidateQueries({ queryKey: RULES_QUERY_KEY });
          setPagination({ page: Math.max(DEFAULT_PAGINATION.page, page - 1) });
        })
        .catch(() => {
          /* feedback handled in deleteRule */
        });
    }
  };

  const isLoading = !rules;

  const searchFilter = (
    <Flex>
      <SearchForm
        query={query}
        onSearch={handleSearch}
        queryHelpComponent={<QueryHelper entityName="Pipeline Rule" />}
        wrapperClass="has-bm"
        onReset={() => handleSearch('')}
        topMargin={0}
      />
      {isDataLoading && (
        <SpinnerWrapper>
          <Spinner text="" delay={0} />
        </SpinnerWrapper>
      )}
    </Flex>
  );

  return (
    <DocumentTitle title="Pipeline rules">
      <PipelinesPageNavigation />
      <PageHeader
        title="Pipeline Rules"
        actions={rulesButtonToolbar}
        documentationLink={{
          title: 'Pipeline rules documentation',
          path: DocsHelper.PAGES.PIPELINE_RULES,
        }}>
        <span>
          Rules are a way of applying changes to messages. A rule consists of a condition and a list of actions. The
          condition is evaluated against a message, and the actions are executed if the condition is satisfied.
        </span>
      </PageHeader>

      <Row className="content">
        <Col md={12}>
          <ProcessingLoadProvider>
            <ProcessingLoadDebugMetricsBanner />
            {isLoading ? (
              <Spinner />
            ) : (
              <Row>
                <Col md={12}>
                  <PaginatedList totalItems={total}>
                    <RuleList
                      rules={rules}
                      rulesContext={rulesContext}
                      onDelete={handleDelete}
                      searchFilter={searchFilter}
                    />
                  </PaginatedList>
                </Col>
              </Row>
            )}
          </ProcessingLoadProvider>
        </Col>
      </Row>
    </DocumentTitle>
  );
};

export default RulesPage;
