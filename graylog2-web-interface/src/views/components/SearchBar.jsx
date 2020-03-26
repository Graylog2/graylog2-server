// @flow strict
import * as React from 'react';
import { useCallback } from 'react';
import PropTypes from 'prop-types';
import * as Immutable from 'immutable';
import { Field } from 'formik';

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
import { QueryFiltersStore } from 'views/stores/QueryFiltersStore';
import Query, { createElasticsearchQueryString, filtersForQuery, filtersToStreamSet } from 'views/logic/queries/Query';
import type { FilterType, QueryId } from 'views/logic/queries/Query';
import SearchBarForm from './searchbar/SearchBarForm';

type Props = {
  availableStreams: Array<*>,
  config: any,
  currentQuery: Query,
  disableSearch: boolean,
  onExecute: () => void,
  queryFilters: Immutable.Map<QueryId, FilterType>,
};

const onSubmit = ({ timerange, streams, queryString }, currentQuery: Query) => {
  const newQuery = currentQuery.toBuilder()
    .timerange(timerange)
    .filter(filtersForQuery(streams))
    .query(createElasticsearchQueryString(queryString))
    .build();

  return QueriesActions.update(newQuery.id, newQuery);
};

const SearchBar = ({ availableStreams, config, currentQuery, disableSearch = false, queryFilters }: Props) => {
  if (!currentQuery || !config) {
    return <Spinner />;
  }

  const { id, query, timerange } = currentQuery;
  const { query_string: queryString } = query;

  const streams = filtersToStreamSet(queryFilters.get(id, Immutable.Map())).toJS();

  const _onSubmit = useCallback(values => onSubmit(values, currentQuery), [query]);

  return (
    <ScrollToHint value={query.query_string}>
      <Row className="content">
        <Col md={12}>
          <SearchBarForm initialValues={{ timerange, streams, queryString }}
                         onSubmit={_onSubmit}>
            {({ dirty, isSubmitting, isValid, handleSubmit }) => (
              <>
                <Row className="no-bm extended-search-query-metadata">
                  <Col md={4}>
                    <TimeRangeTypeSelector />
                    <TimeRangeInput config={config} />
                  </Col>

                  <Col mdHidden lgHidden>
                    <HorizontalSpacer />
                  </Col>

                  <Col md={5} xs={8}>
                    <Field name="streams">
                      {({ field: { name, value, onChange } }) => (
                        <StreamsFilter value={value}
                                       streams={availableStreams}
                                       onChange={newStreams => onChange({ target: { value: newStreams, name } })} />
                      )}
                    </Field>
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
                    <SearchButton disabled={disableSearch || isSubmitting || !isValid} dirty={dirty} />

                    <Field name="queryString">
                      {({ field: { name, value, onChange } }) => (
                        <QueryInput value={value}
                                    placeholder={'Type your search query here and press enter. E.g.: ("not found" AND http) OR http_response_code:[400 TO 404]'}
                                    onChange={(newQuery) => { onChange({ target: { value: newQuery, name } }); return Promise.resolve(); }}
                                    onExecute={handleSubmit} />
                      )}
                    </Field>
                  </Col>
                  <Col md={3} xs={4} className="pull-right">
                    <SavedSearchControls />
                  </Col>
                </Row>
              </>
            )}
          </SearchBarForm>
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
