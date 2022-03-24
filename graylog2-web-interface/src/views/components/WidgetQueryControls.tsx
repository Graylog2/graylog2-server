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
import { useCallback, useEffect, useContext, useRef } from 'react';
import { Field } from 'formik';
import moment from 'moment';
import styled from 'styled-components';
import { isEmpty } from 'lodash';
import { useIsFetching } from 'react-query';

import WidgetEditApplyAllChangesContext from 'views/components/contexts/WidgetEditApplyAllChangesContext';
import { StreamsStore } from 'views/stores/StreamsStore';
import connect, { useStore } from 'stores/connect';
import { createElasticsearchQueryString } from 'views/logic/queries/Query';
import type Widget from 'views/logic/widgets/Widget';
import { WidgetActions } from 'views/stores/WidgetStore';
import type { SearchBarFormValues } from 'views/Constants';
import { DEFAULT_TIMERANGE } from 'views/Constants';
import { SearchConfigStore } from 'views/stores/SearchConfigStore';
import { Row, Col } from 'components/bootstrap';
import type GlobalOverride from 'views/logic/search/GlobalOverride';
import WidgetContext from 'views/components/contexts/WidgetContext';
import { GlobalOverrideStore, GlobalOverrideActions } from 'views/stores/GlobalOverrideStore';
import { SearchActions } from 'views/stores/SearchStore';
import { PropagateDisableSubmissionState } from 'views/components/aggregationwizard';
import QueryValidation from 'views/components/searchbar/queryvalidation/QueryValidation';
import FormWarningsContext from 'contexts/FormWarningsContext';
import FormWarningsProvider from 'contexts/FormWarningsProvider';
import useParameters from 'views/hooks/useParameters';
import debounceWithPromise from 'views/logic/debounceWithPromise';
import validateQuery from 'views/components/searchbar/queryvalidation/validateQuery';
import ValidateOnParameterChange from 'views/components/searchbar/ValidateOnParameterChange';

import TimeRangeOverrideInfo from './searchbar/WidgetTimeRangeOverride';
import TimeRangeInput from './searchbar/TimeRangeInput';
import StreamsFilter from './searchbar/StreamsFilter';
import SearchButton from './searchbar/SearchButton';
import QueryInput from './searchbar/queryinput/AsyncQueryInput';
import SearchBarForm, { normalizeSearchBarFormValues } from './searchbar/SearchBarForm';
import WidgetQueryOverride from './WidgetQueryOverride';

const SecondRow = styled.div`
  display: flex;
  align-items: flex-start;

  .query {
    flex: 1;
  }
`;

const WidgetTopRow = styled(Row)`
  margin-top: 10px;
  margin-bottom: 10px;
`;

type Props = {
  availableStreams: Array<{ key: string, value: string }>,
  globalOverride: GlobalOverride | undefined | null,
};

export const updateWidgetSearchControls = (widget, { timerange, streams, queryString }) => {
  return widget.toBuilder()
    .timerange(timerange)
    .query(createElasticsearchQueryString(queryString))
    .streams(streams)
    .build();
};

const _onSubmit = (values, widget: Widget) => {
  const { timerange, streams, queryString } = values;
  const newWidget = updateWidgetSearchControls(widget, { timerange, streams, queryString });

  return WidgetActions.update(widget.id, newWidget);
};

const _resetTimeRangeOverride = () => GlobalOverrideActions.resetTimeRange().then(SearchActions.refresh);
const _resetQueryOverride = () => GlobalOverrideActions.resetQuery().then(SearchActions.refresh);

const useBindApplySearchControlsChanges = (formRef) => {
  const { bindApplySearchControlsChanges } = useContext(WidgetEditApplyAllChangesContext);

  useEffect(() => {
    bindApplySearchControlsChanges((newWidget: Widget) => {
      if (formRef.current) {
        const { dirty, values, isValid } = formRef.current;

        if (dirty && isValid) {
          const normalizedFormValues = normalizeSearchBarFormValues(values);

          return updateWidgetSearchControls(newWidget, normalizedFormValues);
        }
      }

      return undefined;
    });
  }, [formRef, bindApplySearchControlsChanges]);
};

const debouncedValidateQuery = debounceWithPromise(validateQuery, 350);

const WidgetQueryControls = ({ availableStreams, globalOverride }: Props) => {
  const widget = useContext(WidgetContext);
  const config = useStore(SearchConfigStore, ({ searchesClusterConfig }) => searchesClusterConfig);
  const isValidatingQuery = !!useIsFetching('validateSearchQuery');
  const limitDuration = moment.duration(config?.query_time_range_limit).asSeconds() ?? 0;
  const { streams } = widget;
  const timerange = widget.timerange ?? DEFAULT_TIMERANGE;
  const { query_string: queryString } = widget.query ?? createElasticsearchQueryString('');
  const hasTimeRangeOverride = globalOverride?.timerange !== undefined;
  const hasQueryOverride = globalOverride?.query !== undefined;
  const formRef = useRef(null);
  const { parameters, parameterBindings } = useParameters();
  const _validateQueryString = useCallback((values: SearchBarFormValues) => {
    const request = {
      queryString: values?.queryString,
      timeRange: !isEmpty(globalOverride?.timerange) ? globalOverride.timerange : values?.timerange,
      filter: globalOverride?.query ? globalOverride.query : undefined,
      streams: values?.streams,
      parameters,
      parameterBindings,
    };

    return debouncedValidateQuery(request);
  }, [globalOverride?.query, globalOverride?.timerange, parameterBindings, parameters]);

  useBindApplySearchControlsChanges(formRef);

  return (
    <FormWarningsProvider>
      <SearchBarForm initialValues={{ timerange, streams, queryString }}
                     limitDuration={limitDuration}
                     formRef={formRef}
                     onSubmit={(values) => _onSubmit(values, widget)}
                     validateOnMount={false}
                     validateQueryString={_validateQueryString}>
        {({ dirty, errors, isValid, isSubmitting, handleSubmit, values, setFieldValue, validateForm }) => {
          const disableSearchSubmit = isSubmitting || isValidatingQuery || !isValid;

          return (
            <>
              <PropagateDisableSubmissionState formKey="widget-query-controls" disableSubmission={disableSearchSubmit} />
              <ValidateOnParameterChange parameters={parameters} parameterBindings={parameterBindings} />
              <WidgetTopRow>
                <Col md={6}>
                  {!hasTimeRangeOverride && (
                    <TimeRangeInput disabled={hasTimeRangeOverride}
                                    limitDuration={limitDuration}
                                    onChange={(nextTimeRange) => setFieldValue('timerange', nextTimeRange)}
                                    value={values?.timerange}
                                    hasErrorOnMount={!!errors.timerange}
                                    position="right" />
                  )}
                  {hasTimeRangeOverride && (
                    <TimeRangeOverrideInfo value={globalOverride?.timerange} onReset={_resetTimeRangeOverride} />
                  )}
                </Col>

                <Col md={6}>
                  <Field name="streams">
                    {({ field: { name, value, onChange } }) => (
                      <StreamsFilter value={value}
                                     streams={availableStreams}
                                     onChange={(newStreams) => onChange({ target: { value: newStreams, name } })} />
                    )}
                  </Field>
                </Col>
              </WidgetTopRow>

              <SecondRow>
                <SearchButton disabled={disableSearchSubmit}
                              dirty={dirty} />

                <Field name="queryString">
                  {({ field: { name, value, onChange }, meta: { error } }) => (
                    <FormWarningsContext.Consumer>
                      {({ warnings }) => (
                        <QueryInput value={value}
                                    timeRange={!isEmpty(globalOverride?.timerange) ? globalOverride.timerange : values?.timerange}
                                    streams={values?.streams}
                                    placeholder={'Type your search query here and press enter. E.g.: ("not found" AND http) OR http_response_code:[400 TO 404]'}
                                    error={error}
                                    disableExecution={disableSearchSubmit}
                                    isValidating={isValidatingQuery}
                                    warning={warnings.queryString}
                                    validate={validateForm}
                                    onChange={(newQuery) => {
                                      onChange({ target: { value: newQuery, name } });

                                      return Promise.resolve(newQuery);
                                    }}
                                    onExecute={handleSubmit as () => void} />
                      )}
                    </FormWarningsContext.Consumer>
                  )}
                </Field>

                <QueryValidation />

                {hasQueryOverride
                  && <WidgetQueryOverride value={globalOverride?.query} onReset={_resetQueryOverride} />}
              </SecondRow>
            </>
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
    globalOverride: GlobalOverrideStore,
  },
  ({ availableStreams: { streams = [] }, ...rest }) => ({
    ...rest,
    availableStreams: streams.map((stream) => ({ key: stream.title, value: stream.id })),
  }),
);
