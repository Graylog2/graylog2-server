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
import { useCallback } from 'react';
import PropTypes from 'prop-types';
import * as Immutable from 'immutable';
import { Field } from 'formik';
import styled from 'styled-components';
import moment from 'moment';

import connect from 'stores/connect';
import { Spinner, FlatContentRow } from 'components/common';
import { Row, Col } from 'components/bootstrap';
import BottomRow from 'views/components/searchbar/BottomRow';
import ViewActionsWrapper from 'views/components/searchbar/ViewActionsWrapper';
import SearchButton from 'views/components/searchbar/SearchButton';
import SavedSearchControls from 'views/components/searchbar/saved-search/SavedSearchControls';
import TimeRangeInput from 'views/components/searchbar/TimeRangeInput';
import QueryInput from 'views/components/searchbar/queryinput/AsyncQueryInput';
import StreamsFilter from 'views/components/searchbar/StreamsFilter';
import RefreshControls from 'views/components/searchbar/RefreshControls';
import ScrollToHint from 'views/components/common/ScrollToHint';
import HorizontalSpacer from 'views/components/horizontalspacer/HorizontalSpacer';
import { QueriesActions } from 'views/stores/QueriesStore';
import { CurrentQueryStore } from 'views/stores/CurrentQueryStore';
import { StreamsStore } from 'views/stores/StreamsStore';
import { QueryFiltersStore } from 'views/stores/QueryFiltersStore';
import QueryValidation from 'views/components/searchbar/queryvalidation/QueryValidation';
import type { FilterType, QueryId } from 'views/logic/queries/Query';
import type Query from 'views/logic/queries/Query';
import { createElasticsearchQueryString, filtersForQuery, filtersToStreamSet } from 'views/logic/queries/Query';
import type { SearchesConfig } from 'components/search/SearchConfig';
import type { SearchBarFormValues } from 'views/Constants';
import WidgetFocusContext from 'views/components/contexts/WidgetFocusContext';
import FormWarningsContext from 'contexts/FormWarningsContext';
import FormWarningsProvider from 'contexts/FormWarningsProvider';
import useParameters from 'views/hooks/useParameters';
import debounceWithPromise from 'views/logic/debounceWithPromise';
import validateQuery from 'views/components/searchbar/queryvalidation/validateQuery';
import PluggableSearchBarControls from 'views/components/searchbar/PluggableSearchBarControls';

import ValidateOnParameterChange from './searchbar/ValidateOnParameterChange';
import SearchBarForm from './searchbar/SearchBarForm';

type Props = {
  availableStreams: Array<{ key: string, value: string }>,
  config: SearchesConfig,
  currentQuery: Query,
  disableSearch?: boolean,
  onSubmit?: (update: { timerange: SearchBarFormValues['timerange'], streams: Array<string>, queryString: string }, query: Query) => Promise<any>,
  queryFilters: Immutable.Map<QueryId, FilterType>,
};

const Container = styled.div`
  display: grid;
  row-gap: 10px;
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
  align-items: flex-start;
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

const debouncedValidateQuery = debounceWithPromise(validateQuery, 350);

const SearchBar = ({
  availableStreams,
  config,
  currentQuery,
  disableSearch = defaultProps.disableSearch,
  queryFilters,
  onSubmit = defaultProps.onSubmit,
}: Props) => {
  const { parameterBindings, parameters } = useParameters();

  const _validateQueryString = useCallback((values: SearchBarFormValues) => {
    const request = {
      timeRange: values?.timerange,
      streams: values?.streams,
      queryString: values?.queryString,
      parameterBindings,
      parameters,
    };

    return debouncedValidateQuery(request);
  }, [parameterBindings, parameters]);

  if (!currentQuery || !config) {
    return <Spinner />;
  }

  const { id, query, timerange } = currentQuery;
  const { query_string: queryString } = query;

  const streams = filtersToStreamSet(queryFilters.get(id, Immutable.Map())).toJS();
  const limitDuration = moment.duration(config.query_time_range_limit).asSeconds() ?? 0;

  const _onSubmit = (values: SearchBarFormValues) => onSubmit(values, currentQuery);

  return (
    <WidgetFocusContext.Consumer>
      {({ focusedWidget: { editing } = { editing: false } }) => (
        <ScrollToHint value={query.query_string}>
          <FlatContentRow>
            <FormWarningsProvider>
              <SearchBarForm initialValues={{ timerange, streams, queryString }}
                             limitDuration={limitDuration}
                             onSubmit={_onSubmit}
                             validateQueryString={_validateQueryString}>
                {({ dirty, errors, isSubmitting, isValid, isValidating, handleSubmit, values, setFieldValue, validateForm }) => {
                  const disableSearchSubmit = disableSearch || isSubmitting || isValidating || !isValid;

                  return (
                    <>
                      <ValidateOnParameterChange parameterBindings={parameterBindings} parameters={parameters} />
                      <Container>
                        <Row>
                          <Col md={5}>
                            <TimeRangeInput disabled={disableSearch}
                                            limitDuration={limitDuration}
                                            onChange={(nextTimeRange) => setFieldValue('timerange', nextTimeRange)}
                                            value={values?.timerange}
                                            hasErrorOnMount={!!errors.timerange} />
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
                                                 onChange={(newStreams) => onChange({
                                                   target: {
                                                     value: newStreams,
                                                     name,
                                                   },
                                                 })} />
                                )}
                              </Field>
                            </StreamWrap>

                            <RefreshControls />
                          </FlexCol>
                        </Row>
                        <BottomRow>
                          <SearchButtonAndQuery>
                            <SearchButton disabled={disableSearchSubmit}
                                          dirty={dirty} />

                            <Field name="queryString">
                              {({ field: { name, value, onChange }, meta: { error } }) => (
                                <FormWarningsContext.Consumer>
                                  {({ warnings }) => (
                                    <QueryInput value={value}
                                                timeRange={values.timerange}
                                                streams={values.streams}
                                                placeholder='Type your search query here and press enter. E.g.: ("not found" AND http) OR http_response_code:[400 TO 404]'
                                                error={error}
                                                isValidating={isValidating}
                                                warning={warnings.queryString}
                                                onChange={(newQuery) => {
                                                  onChange({ target: { value: newQuery, name } });

                                                  return Promise.resolve(newQuery);
                                                }}
                                                disableExecution={disableSearchSubmit}
                                                validate={validateForm}
                                                onExecute={handleSubmit as () => void} />
                                  )}
                                </FormWarningsContext.Consumer>
                              )}
                            </Field>

                            <QueryValidation />

                          </SearchButtonAndQuery>

                          {!editing && (
                            <ViewActionsWrapper>
                              <SavedSearchControls />
                            </ViewActionsWrapper>
                          )}
                        </BottomRow>
                        <PluggableSearchBarControls />
                      </Container>
                    </>
                  );
                }}
              </SearchBarForm>
            </FormWarningsProvider>
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
