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
import { useMemo, useCallback, useContext } from 'react';
import { Field } from 'formik';
import moment from 'moment';
import styled, { css } from 'styled-components';

import UserDateTimeContext from 'contexts/UserDateTimeContext';
import { useStore } from 'stores/connect';
import RefreshControls from 'views/components/searchbar/RefreshControls';
import { FlatContentRow, Spinner } from 'components/common';
import ScrollToHint from 'views/components/common/ScrollToHint';
import SearchButton from 'views/components/searchbar/SearchButton';
import QueryInput from 'views/components/searchbar/queryinput/AsyncQueryInput';
import ViewActionsMenu from 'views/components/ViewActionsMenu';
import { GlobalOverrideActions, GlobalOverrideStore } from 'views/stores/GlobalOverrideStore';
import BottomRow from 'views/components/searchbar/BottomRow';
import ViewActionsWrapper from 'views/components/searchbar/ViewActionsWrapper';
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
} from 'views/components/searchbar/pluggableSearchBarControlsHandler';
import type { SearchBarControl } from 'views/types';
import usePluginEntities from 'views/logic/usePluginEntities';
import { SearchConfigStore } from 'views/stores/SearchConfigStore';

import TimeRangeInput from './searchbar/TimeRangeInput';
import type { DashboardFormValues } from './DashboardSearchBarForm';
import DashboardSearchForm from './DashboardSearchBarForm';
import PluggableSearchBarControls from './searchbar/PluggableSearchBarControls';

const Container = styled.div`
  display: grid;
  row-gap: 10px;
`;

const TopRow = styled.div(({ theme }) => css`
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;

  @media (max-width: ${theme.breakpoints.max.sm}) {
    flex-direction: column;
  }
`);

const StyledTimeRangeInput = styled(TimeRangeInput)(({ theme }) => `
  flex: 0.2;
  flex-basis: 700px;

  @media (max-width: ${theme.breakpoints.max.sm}) {
    flex 1;
    flex-basis: auto;
  }
`);

const RefreshControlsWrapper = styled.div(({ theme }) => css`
  margin-left: 18px;

  @media (max-width: ${theme.breakpoints.max.sm}) {
    margin-top: 10px;
    display: flex;
    justify-content: flex-end;
  }
`);

const SearchButtonAndQuery = styled.div`
  flex: 1;
  display: flex;
  align-items: flex-start;
`;

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
  const { userTimezone } = useContext(UserDateTimeContext);
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
          <FlatContentRow>
            <FormWarningsProvider>
              <DashboardSearchForm initialValues={initialValues}
                                   limitDuration={limitDuration}
                                   onSubmit={submitForm}
                                   validateQueryString={(values) => _validateQueryString(values, pluggableSearchBarControls, userTimezone)}>
                {({ dirty, errors, isSubmitting, isValid, isValidating, handleSubmit, values, setFieldValue, validateForm }) => {
                  const disableSearchSubmit = isSubmitting || isValidating || !isValid;

                  return (
                    <Container>
                      <ValidateOnParameterChange parameters={parameters} />
                      <TopRow>
                        <StyledTimeRangeInput onChange={(nextTimeRange) => setFieldValue('timerange', nextTimeRange)}
                                              value={values?.timerange}
                                              limitDuration={limitDuration}
                                              hasErrorOnMount={!!errors.timerange}
                                              noOverride />
                        <RefreshControlsWrapper>
                          <RefreshControls />
                        </RefreshControlsWrapper>
                      </TopRow>

                      <BottomRow>
                        <SearchButtonAndQuery>
                          <SearchButton disabled={disableSearchSubmit}
                                        glyph="filter"
                                        displaySpinner={isSubmitting}
                                        dirty={dirty} />

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
                        </SearchButtonAndQuery>

                        {!editing && (
                          <ViewActionsWrapper>
                            <ViewActionsMenu />
                          </ViewActionsWrapper>
                        )}
                      </BottomRow>
                      <PluggableSearchBarControls showLeftControls={false} />
                    </Container>
                  );
                }}
              </DashboardSearchForm>
            </FormWarningsProvider>
          </FlatContentRow>
        </ScrollToHint>
      )}
    </WidgetFocusContext.Consumer>
  );
};

export default DashboardSearchBar;
