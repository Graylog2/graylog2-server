// @flow strict
import * as React from 'react';
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
import { GlobalOverrideActions, GlobalOverrideStore } from '../stores/GlobalOverrideStore';
import DashboardSearchForm from './DashboardSearchBarForm';
import type { QueryString, TimeRange } from '../logic/queries/Query';
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
  const submitForm = ({ timerange, queryString }) => {
    return [
      () => (timerange ? GlobalOverrideActions.timerange(timerange) : Promise.resolve()),
      () => ((queryString !== undefined && queryString !== null) ? GlobalOverrideActions.query(queryString) : Promise.resolve()),
      performSearch,
    ].reduce((prev, cur) => prev.then(cur), Promise.resolve());
  };
  const { timerange, query: { query_string: queryString = '' } = {} } = globalOverride || {};

  return (
    <ScrollToHint value={queryString}>
      <Row className="content">
        <Col md={12}>
          <DashboardSearchForm initialValues={{ timerange, queryString }} onSubmit={submitForm}>
            {({ isSubmitting, isValid, handleSubmit }) => (
              <>
                <Row className="no-bm extended-search-query-metadata">
                  <Col lg={4} md={6} xs={8}>
                    <Field name="timerange.type">
                      {({ field: { name, value, onChange } }) => (
                        <TimeRangeOverrideTypeSelector onSelect={(newType) => onChange({ target: { value: newType, name } })}
                                                       value={value} />
                      )}
                    </Field>
                    <TimeRangeInput config={config} />
                  </Col>
                  <Col lg={8} md={6} xs={4}>
                    <RefreshControls />
                  </Col>
                </Row>

                <Row className="no-bm">
                  <Col md={9} xs={8}>
                    <div className="pull-right search-help">
                      <DocumentationLink page={DocsHelper.PAGES.SEARCH_QUERY_LANGUAGE}
                                         title="Search query syntax documentation"
                                         text={<Icon name="lightbulb" />} />
                    </div>
                    <SearchButton disabled={disableSearch || isSubmitting || !isValid} glyph="filter" />

                    <Field name="queryString">
                      {({ field: { name, value, onChange } }) => (
                        <QueryInput value={value}
                                    placeholder="Apply filter to all widgets"
                                    onChange={(newQuery) => onChange({ target: { value: newQuery, name } })}
                                    onExecute={handleSubmit} />
                      )}
                    </Field>
                  </Col>
                  <Col md={3} xs={4}>
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
