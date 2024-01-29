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
import { useCallback, useEffect, useContext, useRef, useMemo } from 'react';
import { Field } from 'formik';
import moment from 'moment';
import styled from 'styled-components';
import isEmpty from 'lodash/isEmpty';
import { useIsFetching } from '@tanstack/react-query';

import WidgetEditApplyAllChangesContext from 'views/components/contexts/WidgetEditApplyAllChangesContext';
import { StreamsStore } from 'views/stores/StreamsStore';
import connect, { useStore } from 'stores/connect';
import { createElasticsearchQueryString } from 'views/logic/queries/Query';
import type Widget from 'views/logic/widgets/Widget';
import type { SearchBarFormValues } from 'views/Constants';
import { DEFAULT_TIMERANGE } from 'views/Constants';
import { SearchConfigStore } from 'views/stores/SearchConfigStore';
import type GlobalOverride from 'views/logic/search/GlobalOverride';
import WidgetContext from 'views/components/contexts/WidgetContext';
import { PropagateDisableSubmissionState } from 'views/components/aggregationwizard';
import QueryValidation from 'views/components/searchbar/queryvalidation/QueryValidation';
import FormWarningsContext from 'contexts/FormWarningsContext';
import FormWarningsProvider from 'contexts/FormWarningsProvider';
import useParameters from 'views/hooks/useParameters';
import debounceWithPromise from 'views/logic/debounceWithPromise';
import validateQuery from 'views/components/searchbar/queryvalidation/validateQuery';
import ValidateOnParameterChange from 'views/components/searchbar/ValidateOnParameterChange';
import {
  executeDashboardWidgetSubmitHandler as executePluggableSubmitHandler,
  useInitialDashboardWidgetValues as usePluggableInitialValues,
  pluggableValidationPayload,
} from 'views/logic/searchbar/pluggableSearchBarControlsHandler';
import type { CombinedSearchBarFormValues, SearchBarControl, HandlerContext } from 'views/types';
import usePluginEntities from 'hooks/usePluginEntities';
import useUserDateTime from 'hooks/useUserDateTime';
import {
  SEARCH_BAR_GAP,
  TimeRangeRow,
  SearchQueryRow,
} from 'views/components/searchbar/SearchBarLayout';
import PluggableCommands from 'views/components/searchbar/queryinput/PluggableCommands';
import useGlobalOverride from 'views/hooks/useGlobalOverride';
import type { AppDispatch } from 'stores/useAppDispatch';
import { updateWidget } from 'views/logic/slices/widgetActions';
import { execute, setGlobalOverrideQuery, setGlobalOverrideTimerange } from 'views/logic/slices/searchExecutionSlice';
import useAppDispatch from 'stores/useAppDispatch';
import useHandlerContext from 'views/components/useHandlerContext';
import useView from 'views/hooks/useView';
import { isNoTimeRangeOverride } from 'views/typeGuards/timeRange';
import { normalizeFromSearchBarForBackend } from 'views/logic/queries/NormalizeTimeRange';

import TimeRangeOverrideInfo from './searchbar/WidgetTimeRangeOverride';
import TimeRangeFilter from './searchbar/time-range-filter';
import StreamsFilter from './searchbar/StreamsFilter';
import SearchButton from './searchbar/SearchButton';
import QueryInput from './searchbar/queryinput/AsyncQueryInput';
import SearchBarForm from './searchbar/SearchBarForm';
import WidgetQueryOverride from './WidgetQueryOverride';
import PluggableSearchBarControls from './searchbar/PluggableSearchBarControls';

const Container = styled.div`
  display: flex;
  flex-direction: column;
  gap: ${SEARCH_BAR_GAP};
`;

const SearchInputAndValidation = styled.div`
  display: flex;
  flex: 1;
`;

type Props = {
  availableStreams: Array<{ key: string, value: string }>,
};

export const updateWidgetSearchControls = (widget, { timerange, streams, queryString }) => widget.toBuilder()
  .timerange(timerange)
  .query(createElasticsearchQueryString(queryString))
  .streams(streams)
  .build();

const onSubmit = async (dispatch: AppDispatch, values: CombinedSearchBarFormValues, pluggableSearchBarControls: Array<() => SearchBarControl>, widget: Widget) => {
  const { timerange, streams, queryString } = values;
  const widgetWithPluginData = await executePluggableSubmitHandler(dispatch, values, pluggableSearchBarControls, widget);
  const newWidget = updateWidgetSearchControls(widgetWithPluginData, { timerange, streams, queryString });

  if (!widget.equals(newWidget)) {
    return dispatch(updateWidget(widget.id, newWidget));
  }

  return dispatch(execute());
};

const resetTimeRangeOverride = (dispatch: AppDispatch) => dispatch(setGlobalOverrideTimerange(undefined))
  .then(() => dispatch(execute()));
const resetQueryOverride = (dispatch: AppDispatch) => dispatch(setGlobalOverrideQuery(undefined))
  .then(() => dispatch(execute()));

const useBindApplySearchControlsChanges = (formRef) => {
  const { bindApplySearchControlsChanges } = useContext(WidgetEditApplyAllChangesContext);
  const { userTimezone } = useUserDateTime();

  useEffect(() => {
    bindApplySearchControlsChanges((newWidget: Widget) => {
      if (formRef.current) {
        const { dirty, values: { timerange, ...rest }, isValid } = formRef.current;

        if (dirty && isValid) {
          const normalizedFormValues = {
            timerange: isNoTimeRangeOverride(timerange) ? undefined : normalizeFromSearchBarForBackend(timerange, userTimezone),
            ...rest,
          };

          return updateWidgetSearchControls(newWidget, normalizedFormValues);
        }
      }

      return undefined;
    });
  }, [formRef, bindApplySearchControlsChanges, userTimezone]);
};

const useInitialFormValues = (widget: Widget) => {
  const { streams } = widget;
  const timerange = widget.timerange ?? DEFAULT_TIMERANGE;
  const { query_string: queryString } = widget.query ?? createElasticsearchQueryString('');
  const initialValuesFromPlugins = usePluggableInitialValues(widget);

  return useMemo(() => ({ timerange, streams, queryString, ...initialValuesFromPlugins }), [timerange, streams, queryString, initialValuesFromPlugins]);
};

const debouncedValidateQuery = debounceWithPromise(validateQuery, 350);

const _validateQueryString = (values: SearchBarFormValues, globalOverride: GlobalOverride, pluggableSearchBarControls: Array<() => SearchBarControl>, userTimezone: string, context: HandlerContext) => {
  const request = {
    queryString: values?.queryString,
    timeRange: !isEmpty(globalOverride?.timerange) ? globalOverride.timerange : values?.timerange,
    filter: globalOverride?.query ? globalOverride.query : undefined,
    streams: values?.streams,
    ...pluggableValidationPayload(values, context, pluggableSearchBarControls),
  };

  return debouncedValidateQuery(request, userTimezone);
};

const WidgetQueryControls = ({ availableStreams }: Props) => {
  const view = useView();
  const globalOverride = useGlobalOverride();
  const widget = useContext(WidgetContext);
  const { userTimezone } = useUserDateTime();
  const config = useStore(SearchConfigStore, ({ searchesClusterConfig }) => searchesClusterConfig);
  const isValidatingQuery = !!useIsFetching(['validateSearchQuery']);
  const pluggableSearchBarControls = usePluginEntities('views.components.searchBar');
  const limitDuration = moment.duration(config?.query_time_range_limit).asSeconds() ?? 0;
  const hasTimeRangeOverride = globalOverride?.timerange !== undefined;
  const hasQueryOverride = globalOverride?.query !== undefined;
  const formRef = useRef(null);
  const { parameters } = useParameters();
  const handlerContext = useHandlerContext();
  const validate = useCallback((values: SearchBarFormValues) => _validateQueryString(values, globalOverride, pluggableSearchBarControls, userTimezone, handlerContext),
    [globalOverride, pluggableSearchBarControls, userTimezone, handlerContext]);
  const initialValues = useInitialFormValues(widget);
  const dispatch = useAppDispatch();
  const _onSubmit = useCallback((values: CombinedSearchBarFormValues) => onSubmit(dispatch, values, pluggableSearchBarControls, widget), [dispatch, pluggableSearchBarControls, widget]);
  const _resetTimeRangeOverride = useCallback(() => dispatch(resetTimeRangeOverride), [dispatch]);
  const _resetQueryOverride = useCallback(() => dispatch(resetQueryOverride), [dispatch]);

  useBindApplySearchControlsChanges(formRef);

  return (
    <FormWarningsProvider>
      <SearchBarForm initialValues={initialValues}
                     limitDuration={limitDuration}
                     formRef={formRef}
                     onSubmit={_onSubmit}
                     validateQueryString={validate}>
        {({ dirty, errors, isValid, isSubmitting, handleSubmit, values, setFieldValue, validateForm }) => {
          const disableSearchSubmit = isSubmitting || isValidatingQuery || !isValid;

          return (
            <Container>
              <PropagateDisableSubmissionState formKey="widget-query-controls" disableSubmission={disableSearchSubmit} />
              <ValidateOnParameterChange parameters={parameters} />
              <TimeRangeRow>
                {!hasTimeRangeOverride && (
                  <TimeRangeFilter disabled={hasTimeRangeOverride}
                                   limitDuration={limitDuration}
                                   onChange={(nextTimeRange) => setFieldValue('timerange', nextTimeRange)}
                                   value={values?.timerange}
                                   hasErrorOnMount={!!errors.timerange}
                                   position="right" />
                )}
                {hasTimeRangeOverride && (
                  <TimeRangeOverrideInfo value={globalOverride?.timerange}
                                         onReset={_resetTimeRangeOverride} />
                )}

                <Field name="streams">
                  {({ field: { name, value, onChange } }) => (
                    <StreamsFilter value={value}
                                   streams={availableStreams}
                                   onChange={(newStreams) => onChange({ target: { value: newStreams, name } })} />
                  )}
                </Field>
              </TimeRangeRow>

              <SearchQueryRow>
                <SearchButton disabled={disableSearchSubmit}
                              dirty={dirty}
                              displaySpinner={isSubmitting} />
                <SearchInputAndValidation>
                  <Field name="queryString">
                    {({ field: { name, value, onChange }, meta: { error } }) => (
                      <FormWarningsContext.Consumer>
                        {({ warnings }) => (
                          <PluggableCommands usage="widget_query">
                            {(customCommands) => (
                              <QueryInput value={value}
                                          view={view}
                                          timeRange={!isEmpty(globalOverride?.timerange) ? globalOverride.timerange : values?.timerange}
                                          streams={values?.streams}
                                          placeholder={'Type your search query here and press enter. E.g.: ("not found" AND http) OR http_response_code:[400 TO 404]'}
                                          error={error}
                                          disableExecution={disableSearchSubmit}
                                          isValidating={isValidatingQuery}
                                          warning={warnings.queryString}
                                          validate={validateForm}
                                          name={name}
                                          onChange={onChange}
                                          onExecute={handleSubmit as () => void}
                                          commands={customCommands} />
                            )}
                          </PluggableCommands>
                        )}
                      </FormWarningsContext.Consumer>
                    )}
                  </Field>

                  <QueryValidation />
                </SearchInputAndValidation>

                {hasQueryOverride && (
                  <WidgetQueryOverride value={globalOverride?.query}
                                       onReset={_resetQueryOverride} />
                )}
              </SearchQueryRow>
              <PluggableSearchBarControls />
            </Container>
          );
        }}
      </SearchBarForm>
    </FormWarningsProvider>
  );
};

WidgetQueryControls.propTypes = {};

export default connect(
  WidgetQueryControls,
  {
    availableStreams: StreamsStore,
  },
  ({ availableStreams: { streams = [] }, ...rest }) => ({
    ...rest,
    availableStreams: streams.map((stream) => ({ key: stream.title, value: stream.id })),
  }),
);
