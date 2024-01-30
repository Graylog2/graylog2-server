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

import useView from 'views/hooks/useView';
import { useStore } from 'stores/connect';
import { Spinner } from 'components/common';
import SearchButton from 'views/components/searchbar/SearchButton';
import SearchActionsMenu from 'views/components/searchbar/saved-search/SearchActionsMenu';
import TimeRangeFilter from 'views/components/searchbar/time-range-filter';
import QueryInput from 'views/components/searchbar/queryinput/AsyncQueryInput';
import StreamsFilter from 'views/components/searchbar/StreamsFilter';
import RefreshControls from 'views/components/searchbar/RefreshControls';
import ScrollToHint from 'views/components/common/ScrollToHint';
import { StreamsStore } from 'views/stores/StreamsStore';
import QueryValidation from 'views/components/searchbar/queryvalidation/QueryValidation';
import type { FilterType, QueryId } from 'views/logic/queries/Query';
import type Query from 'views/logic/queries/Query';
import { createElasticsearchQueryString, filtersForQuery, filtersToStreamSet } from 'views/logic/queries/Query';
import type { SearchBarFormValues } from 'views/Constants';
import WidgetFocusContext from 'views/components/contexts/WidgetFocusContext';
import FormWarningsContext from 'contexts/FormWarningsContext';
import FormWarningsProvider from 'contexts/FormWarningsProvider';
import debounceWithPromise from 'views/logic/debounceWithPromise';
import validateQuery from 'views/components/searchbar/queryvalidation/validateQuery';
import usePluginEntities from 'hooks/usePluginEntities';
import PluggableSearchBarControls from 'views/components/searchbar/PluggableSearchBarControls';
import useParameters from 'views/hooks/useParameters';
import ValidateOnParameterChange from 'views/components/searchbar/ValidateOnParameterChange';
import type { SearchBarControl, HandlerContext } from 'views/types';
import { SearchConfigStore } from 'views/stores/SearchConfigStore';
import useUserDateTime from 'hooks/useUserDateTime';
import {
  SEARCH_BAR_GAP,
  TimeRangeRow,
  SearchBarContainer,
  SearchQueryRow,
  SearchButtonAndQuery,
  SearchInputAndValidationContainer,
} from 'views/components/searchbar/SearchBarLayout';
import PluggableCommands from 'views/components/searchbar/queryinput/PluggableCommands';
import useCurrentQuery from 'views/logic/queries/useCurrentQuery';
import useQueryFilters from 'views/logic/queries/useQueryFilters';
import type { AppDispatch } from 'stores/useAppDispatch';
import useAppDispatch from 'stores/useAppDispatch';
import { execute } from 'views/logic/slices/searchExecutionSlice';
import { updateQuery } from 'views/logic/slices/viewSlice';
import useHandlerContext from 'views/components/useHandlerContext';

import SearchBarForm from './searchbar/SearchBarForm';

import {
  executeSearchSubmitHandler as executePluggableSubmitHandler,
  useInitialSearchValues as usePluggableInitialValues,
  pluggableValidationPayload,
} from '../logic/searchbar/pluggableSearchBarControlsHandler';

const StreamsAndRefresh = styled.div`
  display: flex;
  gap: ${SEARCH_BAR_GAP};
  flex: 1.5;
`;

const defaultOnSubmit = async (dispatch: AppDispatch, values: SearchBarFormValues, pluggableSearchBarControls: Array<() => SearchBarControl>, currentQuery: Query) => {
  const { timerange, streams, queryString } = values;

  const queryWithPluginData = await executePluggableSubmitHandler(dispatch, values, pluggableSearchBarControls, currentQuery);

  const newQuery = queryWithPluginData.toBuilder()
    .timerange(timerange)
    .filter(filtersForQuery(streams))
    .query(createElasticsearchQueryString(queryString))
    .build();

  if (!currentQuery.equals(newQuery)) {
    return dispatch(updateQuery(newQuery.id, newQuery));
  }

  return dispatch(execute());
};

const defaultProps = {
  onSubmit: defaultOnSubmit,
};

const debouncedValidateQuery = debounceWithPromise(validateQuery, 350);

const useInitialFormValues = ({ currentQuery, queryFilters }: {
  currentQuery: Query | undefined,
  queryFilters: Immutable.Map<QueryId, FilterType>
}) => {
  const { id, query, timerange } = currentQuery ?? {};
  const { query_string: queryString } = query ?? {};
  const initialValuesFromPlugins = usePluggableInitialValues(currentQuery);
  const streams = filtersToStreamSet(queryFilters.get(id, Immutable.Map())).toJS();

  return ({ timerange, streams, queryString, ...initialValuesFromPlugins });
};

const _validateQueryString = (values: SearchBarFormValues, pluggableSearchBarControls: Array<() => SearchBarControl>, userTimezone: string, context: HandlerContext) => {
  const request = {
    timeRange: values?.timerange,
    streams: values?.streams,
    queryString: values?.queryString,
    ...pluggableValidationPayload(values, context, pluggableSearchBarControls),
  };

  return debouncedValidateQuery(request, userTimezone);
};

type Props = {
  onSubmit?: (dispatch: AppDispatch, update: SearchBarFormValues, pluggableSearchBarControls: Array<() => SearchBarControl>, query: Query) => Promise<any>
};

const SearchBar = ({ onSubmit = defaultProps.onSubmit }: Props) => {
  const view = useView();
  const availableStreams = useStore(StreamsStore, ({ streams }) => streams.map((stream) => ({
    key: stream.title,
    value: stream.id,
  })));
  const { searchesClusterConfig: config } = useStore(SearchConfigStore);
  const { userTimezone } = useUserDateTime();
  const { parameters } = useParameters();
  const currentQuery = useCurrentQuery();
  const queryFilters = useQueryFilters();
  const pluggableSearchBarControls = usePluginEntities('views.components.searchBar');
  const initialValues = useInitialFormValues({ queryFilters, currentQuery });
  const dispatch = useAppDispatch();
  const _onSubmit = useCallback((values: SearchBarFormValues) => onSubmit(dispatch, values, pluggableSearchBarControls, currentQuery),
    [currentQuery, dispatch, onSubmit, pluggableSearchBarControls]);
  const handlerContext = useHandlerContext();

  if (!currentQuery || !config) {
    return <Spinner />;
  }

  const { query } = currentQuery;
  const limitDuration = moment.duration(config.query_time_range_limit).asSeconds() ?? 0;

  return (
    <WidgetFocusContext.Consumer>
      {({ focusedWidget: { editing } = { editing: false } }) => (
        <ScrollToHint value={query.query_string}>
          <FormWarningsProvider>
            <SearchBarForm initialValues={initialValues}
                           limitDuration={limitDuration}
                           onSubmit={_onSubmit}
                           validateQueryString={(values) => _validateQueryString(values, pluggableSearchBarControls, userTimezone, handlerContext)}>
              {({
                dirty,
                errors,
                isSubmitting,
                isValid,
                isValidating,
                handleSubmit,
                values,
                setFieldValue,
                validateForm,
              }) => {
                const disableSearchSubmit = isSubmitting || isValidating || !isValid;

                return (
                  <>
                    <ValidateOnParameterChange parameters={parameters} />
                    <SearchBarContainer>
                      <TimeRangeRow>
                        <TimeRangeFilter limitDuration={limitDuration}
                                         onChange={(nextTimeRange) => setFieldValue('timerange', nextTimeRange)}
                                         value={values?.timerange}
                                         hasErrorOnMount={!!errors.timerange} />
                        <StreamsAndRefresh>
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

                          <RefreshControls />
                        </StreamsAndRefresh>
                      </TimeRangeRow>
                      <SearchQueryRow>
                        <SearchButtonAndQuery>
                          <SearchButton disabled={disableSearchSubmit}
                                        dirty={dirty}
                                        displaySpinner={isSubmitting} />
                          <SearchInputAndValidationContainer>
                            <Field name="queryString">
                              {({ field: { name, value, onChange }, meta: { error } }) => (
                                <FormWarningsContext.Consumer>
                                  {({ warnings }) => (
                                    <PluggableCommands usage="search_query">
                                      {(customCommands) => (
                                        <QueryInput value={value}
                                                    view={view}
                                                    timeRange={values.timerange}
                                                    streams={values.streams}
                                                    name={name}
                                                    onChange={onChange}
                                                    placeholder='Type your search query here and press enter. E.g.: ("not found" AND http) OR http_response_code:[400 TO 404]'
                                                    error={error}
                                                    isValidating={isValidating}
                                                    warning={warnings.queryString}
                                                    disableExecution={disableSearchSubmit}
                                                    validate={validateForm}
                                                    onExecute={handleSubmit as () => void}
                                                    commands={customCommands} />
                                      )}
                                    </PluggableCommands>
                                  )}
                                </FormWarningsContext.Consumer>
                              )}
                            </Field>

                            <QueryValidation />
                          </SearchInputAndValidationContainer>
                        </SearchButtonAndQuery>
                        {!editing && <SearchActionsMenu />}
                      </SearchQueryRow>
                      <PluggableSearchBarControls />
                    </SearchBarContainer>
                  </>
                );
              }}
            </SearchBarForm>
          </FormWarningsProvider>
        </ScrollToHint>
      )}
    </WidgetFocusContext.Consumer>
  );
};

SearchBar.propTypes = {
  onSubmit: PropTypes.func,
};

SearchBar.defaultProps = defaultProps;

export default SearchBar;
