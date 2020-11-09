// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import { Field } from 'formik';
import styled from 'styled-components';

import { Col, Row } from 'components/graylog';
import connect from 'stores/connect';
import DocumentationLink from 'components/support/DocumentationLink';
import DocsHelper from 'util/DocsHelper';
import { Icon, Spinner } from 'components/common';
import ScrollToHint from 'views/components/common/ScrollToHint';
import ViewActionsMenu from 'views/components/ViewActionsMenu';
import { GlobalOverrideActions, GlobalOverrideStore } from 'views/stores/GlobalOverrideStore';
import type { QueryString, TimeRange } from 'views/logic/queries/Query';
import QueryInput from 'views/components/searchbar/AsyncQueryInput';
import SearchButton from 'views/components/searchbar/SearchButton';
import RefreshControls from 'views/components/searchbar/RefreshControls';
import TopRow from 'views/components/searchbar/TopRow';
import DashboardSearchForm from 'views/components/DashboardSearchBarForm';
import TimeRangeTypeSelector from 'views/components/searchbar/TimeRangeTypeSelector';
import TimeRangeDisplay from 'views/components/searchbar/TimeRangeDisplay';

const FlexCol = styled(Col)`
  display: flex;
  align-items: stretch;
  justify-content: space-between;
`;

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

  const submitForm = ({ timerange, queryString }) => GlobalOverrideActions.set(timerange, queryString)
    .then(() => performSearch());
  const { timerange, query: { query_string: queryString = '' } = {} } = globalOverride || {};

  return (
    <ScrollToHint value={queryString}>
      <Row className="content">
        <Col md={12}>
          <DashboardSearchForm initialValues={{ timerange, queryString }} onSubmit={submitForm}>
            {({ dirty, isSubmitting, isValid, handleSubmit, values }) => (
              <>
                <TopRow>
                  <FlexCol lg={6} md={6} xs={12}>
                    <TimeRangeTypeSelector disabled={disableSearch}
                                           config={config}
                                           noOverride />
                    <TimeRangeDisplay timerange={values?.timerange} />
                    <RefreshControls />
                  </FlexCol>
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
