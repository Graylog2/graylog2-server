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
import { useMemo, useCallback } from 'react';
import { Field } from 'formik';
import moment from 'moment';
import styled, { css } from 'styled-components';

import { useStore } from 'stores/connect';
import RefreshControls from 'views/components/searchbar/RefreshControls';
import { Spinner } from 'components/common';
import ScrollToHint from 'views/components/common/ScrollToHint';
import SearchButton from 'views/components/searchbar/SearchButton';
import QueryInput from 'views/components/searchbar/queryinput/AsyncQueryInput';
import DashboardActionsMenu from 'views/components/DashboardActionsMenu';
import WidgetFocusContext from 'views/components/contexts/WidgetFocusContext';
import QueryValidation from 'views/components/searchbar/queryvalidation/QueryValidation';
import FormWarningsContext from 'contexts/FormWarningsContext';
import FormWarningsProvider from 'contexts/FormWarningsProvider';
import useParameters from 'views/hooks/useParameters';
import debounceWithPromise from 'views/logic/debounceWithPromise';
import validateQuery from 'views/components/searchbar/queryvalidation/validateQuery';
import { isNoTimeRangeOverride } from 'views/typeGuards/timeRange';
import ValidateOnParameterChange from 'views/components/searchbar/ValidateOnParameterChange';
import {
  executeSearchSubmitHandler as executePluggableSubmitHandler,
  useInitialSearchValues as usePluggableInitialValues,
  pluggableValidationPayload,
} from 'views/logic/searchbar/pluggableSearchBarControlsHandler';
import type { SearchBarControl, HandlerContext } from 'views/types';
import usePluginEntities from 'hooks/usePluginEntities';
import { SearchConfigStore } from 'views/stores/SearchConfigStore';
import useUserDateTime from 'hooks/useUserDateTime';
import {
  SEARCH_BAR_GAP,
  SearchBarContainer,
  SearchQueryRow,
  SearchButtonAndQuery,
  SearchInputAndValidationContainer,
} from 'views/components/searchbar/SearchBarLayout';
import PluggableCommands from 'views/components/searchbar/queryinput/PluggableCommands';
import useAppDispatch from 'stores/useAppDispatch';
import { setGlobalOverride, execute } from 'views/logic/slices/searchExecutionSlice';
import useGlobalOverride from 'views/hooks/useGlobalOverride';
import useHandlerContext from 'views/components/useHandlerContext';
import type { TimeRange } from 'views/logic/queries/Query';
import useView from 'views/hooks/useView';

import TimeRangeFilter from './searchbar/time-range-filter';
import type { DashboardFormValues } from './DashboardSearchBarForm';
import DashboardSearchForm from './DashboardSearchBarForm';
import PluggableSearchBarControls from './searchbar/PluggableSearchBarControls';

const TimeRangeRow = styled.div(({ theme }) => css`
  display: flex;
  justify-content: space-between;
  gap: ${SEARCH_BAR_GAP};

  @media (max-width: ${theme.breakpoints.max.sm}) {
    flex-direction: column;
  }
`);

const StyledTimeRangeFilter = styled(TimeRangeFilter)(({ theme }) => css`
  flex: 0.2;
  flex-basis: 380px;
  
  @media (max-width: ${theme.breakpoints.max.sm}) {
    flex: 1;
    flex-basis: auto;
  }
`);

const debouncedValidateQuery = debounceWithPromise(validateQuery, 350);

const _validateQueryString = (values: DashboardFormValues, pluggableSearchBarControls: Array<() => SearchBarControl>, userTimezone: string, context: HandlerContext) => {
  const request = {
    timeRange: isNoTimeRangeOverride(values?.timerange) ? undefined : values?.timerange,
    queryString: values?.queryString,
    ...pluggableValidationPayload(values, context, pluggableSearchBarControls),
  };

  return debouncedValidateQuery(request, userTimezone);
};

const useInitialFormValues = (timerange: TimeRange, queryString: string) => {
  const initialValuesFromPlugins = usePluggableInitialValues();

  return useMemo(() => ({ timerange, queryString, ...initialValuesFromPlugins }), [queryString, timerange, initialValuesFromPlugins]);
};

const DashboardSearchBar = () => {
  const view = useView();
  const { userTimezone } = useUserDateTime();
  const { searchesClusterConfig: config } = useStore(SearchConfigStore);
  const { timerange, query: { query_string: queryString = '' } = {} } = useGlobalOverride() ?? {};
  const pluggableSearchBarControls = usePluginEntities('views.components.searchBar');
  const dispatch = useAppDispatch();
  const handlerContext = useHandlerContext();

  const submitForm = useCallback(async (values) => {
    const { timerange: newTimerange, queryString: newQueryString } = values;
    await executePluggableSubmitHandler(dispatch, values, pluggableSearchBarControls);

    dispatch(setGlobalOverride(newQueryString, newTimerange));
    dispatch(execute());
  }, [dispatch, pluggableSearchBarControls]);

  const { parameters } = useParameters();
  const initialValues = useInitialFormValues(timerange, queryString);

  if (!config) {
    return <Spinner />;
  }

  const limitDuration = moment.duration(config.query_time_range_limit).asSeconds() ?? 0;

  return (
    <WidgetFocusContext.Consumer>
      {({ focusedWidget: { editing } = { editing: false } }) => (
        <ScrollToHint value={queryString}>
          <FormWarningsProvider>
            <DashboardSearchForm initialValues={initialValues}
                                 limitDuration={limitDuration}
                                 onSubmit={submitForm}
                                 validateQueryString={(values) => _validateQueryString(values, pluggableSearchBarControls, userTimezone, handlerContext)}>
              {({ dirty, errors, isSubmitting, isValid, isValidating, handleSubmit, values, setFieldValue, validateForm }) => {
                const disableSearchSubmit = isSubmitting || isValidating || !isValid;

                return (
                  <SearchBarContainer>
                    <ValidateOnParameterChange parameters={parameters} />
                    <TimeRangeRow>
                      <StyledTimeRangeFilter onChange={(nextTimeRange) => setFieldValue('timerange', nextTimeRange)}
                                             value={values?.timerange}
                                             limitDuration={limitDuration}
                                             hasErrorOnMount={!!errors.timerange}
                                             noOverride />
                      <RefreshControls />
                    </TimeRangeRow>

                    <SearchQueryRow>
                      <SearchButtonAndQuery>
                        <SearchButton disabled={disableSearchSubmit}
                                      glyph="filter"
                                      displaySpinner={isSubmitting}
                                      dirty={dirty} />
                        <SearchInputAndValidationContainer>
                          <Field name="queryString">
                            {({ field: { name, value, onChange }, meta: { error } }) => (
                              <FormWarningsContext.Consumer>
                                {({ warnings }) => (
                                  <PluggableCommands usage="global_override_query">
                                    {(customCommands) => (
                                      <QueryInput value={value}
                                                  view={view}
                                                  timeRange={values?.timerange}
                                                  placeholder="Apply filter to all widgets"
                                                  name={name}
                                                  onChange={onChange}
                                                  disableExecution={disableSearchSubmit}
                                                  error={error}
                                                  isValidating={isValidating}
                                                  validate={validateForm}
                                                  warning={warnings.queryString}
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

                      {!editing && <DashboardActionsMenu />}
                    </SearchQueryRow>
                    <PluggableSearchBarControls showLeftControls={false} />
                  </SearchBarContainer>
                );
              }}
            </DashboardSearchForm>
          </FormWarningsProvider>
        </ScrollToHint>
      )}
    </WidgetFocusContext.Consumer>
  );
};

export default DashboardSearchBar;
