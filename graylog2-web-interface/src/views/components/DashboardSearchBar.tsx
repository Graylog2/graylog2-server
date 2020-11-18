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
// @flow strict
import * as React from 'react';
import { useCallback } from 'react';
import PropTypes from 'prop-types';
import { Field } from 'formik';

import { Col, Row } from 'components/graylog';
import connect from 'stores/connect';
import DocumentationLink from 'components/support/DocumentationLink';
import DocsHelper from 'util/DocsHelper';
import RefreshControls from 'views/components/searchbar/RefreshControls';
import { Icon, Spinner } from 'components/common';
import ScrollToHint from 'views/components/common/ScrollToHint';
import TimeRangeOverrideTypeSelector from 'views/components/searchbar/TimeRangeOverrideTypeSelector';
import SearchButton from 'views/components/searchbar/SearchButton';
import QueryInput from 'views/components/searchbar/AsyncQueryInput';
import ViewActionsMenu from 'views/components/ViewActionsMenu';
import { GlobalOverrideActions, GlobalOverrideStore } from 'views/stores/GlobalOverrideStore';
import type { QueryString, TimeRange } from 'views/logic/queries/Query';
import TopRow from 'views/components/searchbar/TopRow';

import DashboardSearchForm from './DashboardSearchBarForm';
import TimeRangeInput from './searchbar/TimeRangeInput';

type Props = {
  config: any,
  globalOverride: {
    timerange: TimeRange,
    query: QueryString,
  },
  disableSearch: boolean,
  onExecute: () => void,
};

const DashboardSearchBar = ({ config, globalOverride, disableSearch = false, onExecute: performSearch }: Props) => {
  if (!config) {
    return <Spinner />;
  }

  const submitForm = useCallback(({ timerange, queryString }) => GlobalOverrideActions.set(timerange, queryString)
    .then(() => performSearch()), [performSearch]);
  const { timerange, query: { query_string: queryString = '' } = {} } = globalOverride || {};

  return (
    <ScrollToHint value={queryString}>
      <Row className="content">
        <Col md={12}>
          <DashboardSearchForm initialValues={{ timerange, queryString }} onSubmit={submitForm}>
            {({ dirty, isSubmitting, isValid, handleSubmit }) => (
              <>
                <TopRow>
                  <Col lg={4} md={6} xs={8}>
                    <TimeRangeOverrideTypeSelector />
                    <TimeRangeInput config={config} />
                  </Col>
                  <Col lg={8} md={6} xs={4}>
                    <RefreshControls />
                  </Col>
                </TopRow>

                <Row className="no-bm">
                  <Col md={8} lg={9}>
                    <div className="pull-right search-help">
                      <DocumentationLink page={DocsHelper.PAGES.SEARCH_QUERY_LANGUAGE}
                                         title="Search query syntax documentation"
                                         text={<Icon name="lightbulb" />} />
                    </div>
                    <SearchButton running={isSubmitting}
                                  disabled={disableSearch || isSubmitting || !isValid}
                                  glyph="filter"
                                  dirty={dirty} />

                    <Field name="queryString">
                      {({ field: { name, value, onChange } }) => (
                        <QueryInput value={value}
                                    placeholder="Apply filter to all widgets"
                                    onChange={(newQuery) => {
                                      onChange({ target: { value: newQuery, name } });

                                      return Promise.resolve();
                                    }}
                                    onExecute={handleSubmit} />
                      )}
                    </Field>
                  </Col>
                  <Col md={4} lg={3}>
                    <div className="pull-right">
                      <ViewActionsMenu />
                    </div>
                  </Col>
                </Row>
              </>
            )}
          </DashboardSearchForm>
        </Col>
      </Row>
    </ScrollToHint>
  );
};

DashboardSearchBar.propTypes = {
  config: PropTypes.object.isRequired,
  disableSearch: PropTypes.bool,
  onExecute: PropTypes.func.isRequired,
};

DashboardSearchBar.defaultProps = {
  disableSearch: false,
};

export default connect(
  DashboardSearchBar,
  {
    globalOverride: GlobalOverrideStore,
  },
);
