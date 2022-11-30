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
import { GlobalOverrideActions, GlobalOverrideStore } from 'views/stores/GlobalOverrideStore';
import WidgetFocusContext from 'views/components/contexts/WidgetFocusContext';
import QueryValidation from 'views/components/searchbar/queryvalidation/QueryValidation';
import FormWarningsContext from 'contexts/FormWarningsContext';
import FormWarningsProvider from 'contexts/FormWarningsProvider';
import useParameters from 'views/hooks/useParameters';
import debounceWithPromise from 'views/logic/debounceWithPromise';
import validateQuery from 'views/components/searchbar/queryvalidation/validateQuery';
import { isNoTimeRangeOverride } from 'views/typeGuards/timeRange';
import ValidateOnParameterChange from 'views/components/searchbar/ValidateOnParameterChange';
import { SearchActions } from 'views/stores/SearchStore';
import {
  executeSearchSubmitHandler as executePluggableSubmitHandler,
  useInitialSearchValues as usePluggableInitialValues,
  pluggableValidationPayload,
} from 'views/logic/searchbar/pluggableSearchBarControlsHandler';
import type { SearchBarControl } from 'views/types';
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

import TimeRangeInput from './searchbar/TimeRangeInput';
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

const StyledTimeRangeInput = styled(TimeRangeInput)(({ theme }) => css`
  flex: 0.2;
  flex-basis: 380px;
  
  @media (max-width: ${theme.breakpoints.max.sm}) {
    flex: 1;
    flex-basis: auto;
  }
`);

const debouncedValidateQuery = debounceWithPromise(validateQuery, 350);

const _validateQueryString = (values: DashboardFormValues, pluggableSearchBarControls: Array<() => SearchBarControl>, userTimezone: string) => {
  const request = {
    timeRange: isNoTimeRangeOverride(values?.timerange) ? undefined : values?.timerange,
    queryString: values?.queryString,
    ...pluggableValidationPayload(values, pluggableSearchBarControls),
  };

  return debouncedValidateQuery(request, userTimezone);
};

const useInitialFormValues = (timerange, queryString) => {
  const initialValuesFromPlugins = usePluggableInitialValues();

  return useMemo(() => {
    return { timerange, queryString, ...initialValuesFromPlugins };
  }, [queryString, timerange, initialValuesFromPlugins]);
};

const DashboardSearchBar = () => {
  const { userTimezone } = useUserDateTime();
  const { searchesClusterConfig: config } = useStore(SearchConfigStore);
  const { timerange, query: { query_string: queryString = '' } = {} } = useStore(GlobalOverrideStore) ?? {};
  const pluggableSearchBarControls = usePluginEntities('views.components.searchBar');

  const submitForm = useCallback(async (values) => {
    const { timerange: newTimerange, queryString: newQueryString } = values;
    await executePluggableSubmitHandler(values, pluggableSearchBarControls);

    return GlobalOverrideActions.set(newTimerange, newQueryString).then(() => SearchActions.refresh());
  }, [pluggableSearchBarControls]);

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
                                 validateQueryString={(values) => _validateQueryString(values, pluggableSearchBarControls, userTimezone)}>
              {({ dirty, errors, isSubmitting, isValid, isValidating, handleSubmit, values, setFieldValue, validateForm }) => {
                const disableSearchSubmit = isSubmitting || isValidating || !isValid;

                return (
                  <SearchBarContainer>
                    <ValidateOnParameterChange parameters={parameters} />
                    <TimeRangeRow>
                      <StyledTimeRangeInput onChange={(nextTimeRange) => setFieldValue('timerange', nextTimeRange)}
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
                                  <QueryInput value={value}
                                              timeRange={values?.timerange}
                                              placeholder="Apply filter to all widgets"
                                              name={name}
                                              onChange={onChange}
                                              disableExecution={disableSearchSubmit}
                                              error={error}
                                              isValidating={isValidating}
                                              validate={validateForm}
                                              warning={warnings.queryString}
                                              onExecute={handleSubmit as () => void} />
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
