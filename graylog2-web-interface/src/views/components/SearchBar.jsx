// @flow strict
import * as React from 'react';
import { useCallback } from 'react';
import PropTypes from 'prop-types';
import * as Immutable from 'immutable';
import moment from 'moment';
import { Formik, Form, Field } from 'formik';

import connect from 'stores/connect';
import DocumentationLink from 'components/support/DocumentationLink';
import DocsHelper from 'util/DocsHelper';
import { Spinner, Icon } from 'components/common';
import { Col, Row } from 'components/graylog';

import SearchButton from 'views/components/searchbar/SearchButton';
import SavedSearchControls from 'views/components/searchbar/saved-search/SavedSearchControls';
import TimeRangeInput from 'views/components/searchbar/TimeRangeInput';
import TimeRangeTypeSelector from 'views/components/searchbar/TimeRangeTypeSelector';
import QueryInput from 'views/components/searchbar/AsyncQueryInput';
import StreamsFilter from 'views/components/searchbar/StreamsFilter';
import RefreshControls from 'views/components/searchbar/RefreshControls';
import ScrollToHint from 'views/components/common/ScrollToHint';
import HorizontalSpacer from 'views/components/horizontalspacer/HorizontalSpacer';
import { QueriesActions } from 'views/stores/QueriesStore';
import { CurrentQueryStore } from 'views/stores/CurrentQueryStore';
import { StreamsStore } from 'views/stores/StreamsStore';
import { QueryFiltersActions, QueryFiltersStore } from 'views/stores/QueryFiltersStore';
import Query, { filtersToStreamSet } from 'views/logic/queries/Query';
import type { FilterType, QueryId, TimeRange } from 'views/logic/queries/Query';

type Props = {
  availableStreams: Array<*>,
  config: any,
  currentQuery: Query,
  disableSearch: boolean,
  onExecute: () => void,
  queryFilters: Immutable.Map<QueryId, FilterType>,
};

const migrateTimeRangeToNewType = (oldTimerange: TimeRange, type: string): TimeRange => {
  const oldType = oldTimerange.type;

  if (type === oldType) {
    return oldTimerange;
  }

  switch (type) {
    case 'absolute':
      return {
        type,
        from: moment().subtract(oldTimerange.type === 'relative' ? oldTimerange.range : 300, 'seconds').toISOString(),
        to: moment().toISOString(),
      };
    case 'relative':
      return {
        type,
        range: 300,
      };
    case 'keyword':
      return {
        type,
        keyword: 'Last five Minutes',
      };
    default: throw new Error(`Invalid time range type: ${type}`);
  }
};

const SearchBar = ({ availableStreams, config, currentQuery, disableSearch = false, onExecute: performSearch, queryFilters }: Props) => {
  if (!currentQuery || !config) {
    return <Spinner />;
  }

  const { id, query, timerange } = currentQuery;

  const setTimeRange = useCallback((newTimerange: TimeRange) => QueriesActions.timerange(id, newTimerange), [id]);

  const streams = filtersToStreamSet(queryFilters.get(id, Immutable.Map())).toJS();

  const setStreams = useCallback(value => setTimeRange(timerange).then(() => QueryFiltersActions.streams(id, value)), [id, timerange, setTimeRange]);
  const setQueryString = useCallback(value => setTimeRange(timerange).then(() => QueriesActions.query(id, value)), [id, timerange, setTimeRange]);

  return (
    <ScrollToHint value={query.query_string}>
      <Row className="content">
        <Col md={12}>
          <Formik initialValues={{ timerange }}
                  validate={(values) => {
                    console.log('Validating: ', values);
                  }}
                  onSubmit={(values) => {
                    console.log('Submitting: ', values);
                    return QueriesActions.timerange(currentQuery.id, values.timerange);
                  }}>
            {({ values, isSubmitting, errors }) => (
              <Form>
                <Row className="no-bm extended-search-query-metadata">
                  <Col md={4}>
                    <Field name="timerange.type">
                      {({ field: { value, onChange } }) => (
                        <TimeRangeTypeSelector onSelect={newType => onChange({ target: { value: migrateTimeRangeToNewType(values.timerange, newType), name: 'timerange' } })}
                                               value={value} />
                      )}
                    </Field>
                    <TimeRangeInput type={values.timerange.type}
                                    config={config} />
                  </Col>

                  <Col mdHidden lgHidden>
                    <HorizontalSpacer />
                  </Col>

                  <Col md={5} xs={8}>
                    <StreamsFilter value={streams}
                                   streams={availableStreams}
                                   onChange={setStreams} />
                  </Col>

                  <Col md={3} xs={4}>
                    <RefreshControls />
                  </Col>
                </Row>

                <Row className="no-bm">
                  <Col md={9} xs={8}>
                    <div className="pull-right search-help">
                      <DocumentationLink page={DocsHelper.PAGES.SEARCH_QUERY_LANGUAGE}
                                         title="Search query syntax documentation"
                                         text={<Icon name="lightbulb-o" />} />
                    </div>
                    <SearchButton disabled={disableSearch || isSubmitting || Object.keys(errors).length > 0} />

                    <QueryInput value={query.query_string}
                                placeholder={'Type your search query here and press enter. E.g.: ("not found" AND http) OR http_response_code:[400 TO 404]'}
                                onChange={setQueryString}
                                onExecute={performSearch} />
                  </Col>
                  <Col md={3} xs={4} className="pull-right">
                    <SavedSearchControls />
                  </Col>
                </Row>
              </Form>
            )}
          </Formik>
        </Col>
      </Row>
    </ScrollToHint>
  );
};

SearchBar.propTypes = {
  config: PropTypes.object.isRequired,
  disableSearch: PropTypes.bool,
  onExecute: PropTypes.func.isRequired,
};

SearchBar.defaultProps = {
  disableSearch: false,
};

export default connect(
  SearchBar,
  {
    currentQuery: CurrentQueryStore,
    availableStreams: StreamsStore,
    queryFilters: QueryFiltersStore,
  },
  ({ availableStreams: { streams }, ...rest }) => ({
    ...rest,
    availableStreams: streams.map((stream) => ({ key: stream.title, value: stream.id })),
  }),
);
