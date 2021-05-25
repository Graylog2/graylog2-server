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
import * as React from 'react';
import PropTypes from 'prop-types';
import * as Immutable from 'immutable';
import { Field } from 'formik';
import styled from 'styled-components';
import moment from 'moment';

import connect from 'stores/connect';
import DocumentationLink from 'components/support/DocumentationLink';
import DocsHelper from 'util/DocsHelper';
import { Spinner, Icon, FlatContentRow } from 'components/common';
import { Row, Col } from 'components/graylog';
import BottomRow from 'views/components/searchbar/BottomRow';
import ViewActionsWrapper from 'views/components/searchbar/ViewActionsWrapper';
import SearchButton from 'views/components/searchbar/SearchButton';
import SavedSearchControls from 'views/components/searchbar/saved-search/SavedSearchControls';
import TimeRangeInput from 'views/components/searchbar/TimeRangeInput';
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
import type { SearchesConfig } from 'components/search/SearchConfig';
import type { SearchBarFormValues } from 'views/Constants';
import WidgetFocusContext from 'views/components/contexts/WidgetFocusContext';

import SearchBarForm from './searchbar/SearchBarForm';

type Props = {
  availableStreams: Array<{ key: string, value: string }>,
  config: SearchesConfig,
  currentQuery: Query,
  disableSearch?: boolean,
  onSubmit?: (update: { timerange: SearchBarFormValues['timerange'], streams: Array<string>, queryString: string }, query: Query) => Promise<any>,
  queryFilters: Immutable.Map<QueryId, FilterType>,
};

const TopRow = styled(Row)`
  margin-bottom: 10px;
`;

const FlexCol = styled(Col)`
  display: flex;
  align-items: stretch;
  justify-content: space-between;
`;

const StreamWrap = styled.div`
  flex: 1;
  
  > div {
    margin-right: 18px;
  }
`;

const SearchButtonAndQuery = styled.div`
  flex: 1;
  display: flex;
`;

const StyledQueryInput = styled(QueryInput)`
  flex: 1;
`;

const defaultOnSubmit = ({ timerange, streams, queryString }, currentQuery: Query) => {
  const newQuery = currentQuery.toBuilder()
    .timerange(timerange)
    .filter(filtersForQuery(streams))
    .query(createElasticsearchQueryString(queryString))
    .build();

  return QueriesActions.update(newQuery.id, newQuery);
};

const defaultProps = {
  disableSearch: false,
  onSubmit: defaultOnSubmit,
};

const SearchBar = ({
  availableStreams,
  config,
  currentQuery,
  disableSearch = defaultProps.disableSearch,
  queryFilters,
  onSubmit = defaultProps.onSubmit,
}: Props) => {
  if (!currentQuery || !config) {
    return <Spinner />;
  }

  const { id, query, timerange } = currentQuery;
  const { query_string: queryString } = query;

  const streams = filtersToStreamSet(queryFilters.get(id, Immutable.Map())).toJS();
  const limitDuration = moment.duration(config.query_time_range_limit).asSeconds() ?? 0;

  const _onSubmit = (values) => onSubmit(values, currentQuery);

  return (
    <WidgetFocusContext.Consumer>
      {({ focusedWidget: { editing } = { editing: false } }) => (
        <ScrollToHint value={query.query_string}>
          <FlatContentRow>
            <SearchBarForm initialValues={{ timerange, streams, queryString }}
                           limitDuration={limitDuration}
                           onSubmit={_onSubmit}>
              {({ dirty, isSubmitting, isValid, handleSubmit, values, setFieldValue }) => (
                <>
                  <TopRow>
                    <Col md={5}>
                      <TimeRangeInput disabled={disableSearch}
                                      onChange={(nextTimeRange) => setFieldValue('timerange', nextTimeRange)}
                                      value={values?.timerange}
                                      hasErrorOnMount={!isValid} />
                    </Col>

                    <Col mdHidden lgHidden>
                      <HorizontalSpacer />
                    </Col>

                    <FlexCol md={7}>
                      <StreamWrap>
                        <Field name="streams">
                          {({ field: { name, value, onChange } }) => (
                            <StreamsFilter value={value}
                                           streams={availableStreams}
                                           onChange={(newStreams) => onChange({ target: { value: newStreams, name } })} />
                          )}
                        </Field>
                      </StreamWrap>

                      <RefreshControls />
                    </FlexCol>
                  </TopRow>

                  <BottomRow>
                    <SearchButtonAndQuery>
                      <SearchButton disabled={disableSearch || isSubmitting || !isValid}
                                    dirty={dirty} />

                      <Field name="queryString">
                        {({ field: { name, value, onChange } }) => (
                          <StyledQueryInput value={value}
                                            placeholder={'Type your search query here and press enter. E.g.: ("not found" AND http) OR http_response_code:[400 TO 404]'}
                                            onChange={(newQuery) => {
                                              onChange({ target: { value: newQuery, name } });

                                              return Promise.resolve(newQuery);
                                            }}
                                            onExecute={handleSubmit as () => void} />
                        )}
                      </Field>

                      <div className="search-help">
                        <DocumentationLink page={DocsHelper.PAGES.SEARCH_QUERY_LANGUAGE}
                                           title="Search query syntax documentation"
                                           text={<Icon name="lightbulb" />} />
                      </div>
                    </SearchButtonAndQuery>

                    {!editing && (
                      <ViewActionsWrapper>
                        <SavedSearchControls />
                      </ViewActionsWrapper>
                    )}
                  </BottomRow>
                </>
              )}
            </SearchBarForm>
          </FlatContentRow>
        </ScrollToHint>
      )}
    </WidgetFocusContext.Consumer>
  );
};

SearchBar.propTypes = {
  disableSearch: PropTypes.bool,
  onSubmit: PropTypes.func,
};

SearchBar.defaultProps = defaultProps;

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
