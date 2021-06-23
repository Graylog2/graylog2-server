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
import { useEffect, useContext, useRef } from 'react';
import { Field } from 'formik';
import moment from 'moment';
import styled from 'styled-components';

import WidgetEditApplyAllChangesContext from 'views/components/contexts/WidgetEditApplyAllChangesContext';
import { StreamsStore } from 'views/stores/StreamsStore';
import connect, { useStore } from 'stores/connect';
import { createElasticsearchQueryString } from 'views/logic/queries/Query';
import Widget from 'views/logic/widgets/Widget';
import { WidgetActions } from 'views/stores/WidgetStore';
import { DEFAULT_TIMERANGE } from 'views/Constants';
import { SearchConfigStore } from 'views/stores/SearchConfigStore';
import { Row, Col } from 'components/graylog';
import { Icon } from 'components/common';
import DocumentationLink from 'components/support/DocumentationLink';
import DocsHelper from 'util/DocsHelper';
import GlobalOverride from 'views/logic/search/GlobalOverride';
import WidgetContext from 'views/components/contexts/WidgetContext';
import { GlobalOverrideStore, GlobalOverrideActions } from 'views/stores/GlobalOverrideStore';
import { SearchActions } from 'views/stores/SearchStore';
import PropagateValidationState from 'views/components/aggregationwizard/PropagateValidationState';

import TimeRangeOverrideInfo from './searchbar/WidgetTimeRangeOverride';
import TimeRangeInput from './searchbar/TimeRangeInput';
import StreamsFilter from './searchbar/StreamsFilter';
import SearchButton from './searchbar/SearchButton';
import QueryInput from './searchbar/AsyncQueryInput';
import SearchBarForm, { normalizeSearchBarFormValues } from './searchbar/SearchBarForm';
import WidgetQueryOverride from './WidgetQueryOverride';

const SecondRow = styled.div`
  display: flex;

  .query {
    flex: 1;
  }
`;

const WidgetTopRow = styled(Row)`
  margin-top: 10px;
  margin-bottom: 10px;
`;

type Props = {
  availableStreams: Array<any>,
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

const WidgetQueryControls = ({ availableStreams, globalOverride }: Props) => {
  const widget = useContext(WidgetContext);
  const config = useStore(SearchConfigStore, ({ searchesClusterConfig }) => searchesClusterConfig);
  const limitDuration = moment.duration(config?.query_time_range_limit).asSeconds() ?? 0;
  const { streams } = widget;
  const timerange = widget.timerange ?? DEFAULT_TIMERANGE;
  const { query_string: queryString } = widget.query ?? createElasticsearchQueryString('');
  const hasTimeRangeOverride = globalOverride?.timerange !== undefined;
  const hasQueryOverride = globalOverride?.query !== undefined;
  const formRef = useRef(null);

  useBindApplySearchControlsChanges(formRef);

  return (
    <>
      <SearchBarForm initialValues={{ timerange, streams, queryString }}
                     limitDuration={limitDuration}
                     formRef={formRef}
                     onSubmit={(values) => _onSubmit(values, widget)}
                     validateOnMount={false}>
        {({ dirty, isValid, isSubmitting, handleSubmit, values, setFieldValue }) => (
          <>
            <PropagateValidationState formKey="widget-query-controls" />
            <WidgetTopRow>
              <Col md={6}>
                {!hasTimeRangeOverride && (
                  <TimeRangeInput disabled={hasTimeRangeOverride}
                                  onChange={(nextTimeRange) => setFieldValue('timerange', nextTimeRange)}
                                  value={values?.timerange}
                                  hasErrorOnMount={!isValid}
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
              <SearchButton disabled={isSubmitting || !isValid}
                            dirty={dirty} />

              <Field name="queryString">
                {({ field: { name, value, onChange } }) => (
                  <QueryInput value={value}
                              placeholder={'Type your search query here and press enter. E.g.: ("not found" AND http) OR http_response_code:[400 TO 404]'}
                              onChange={(newQuery) => {
                                onChange({ target: { value: newQuery, name } });

                                return Promise.resolve(newQuery);
                              }}
                              onExecute={handleSubmit as () => void} />
                )}
              </Field>

              <div className="pull-right search-help">
                <DocumentationLink page={DocsHelper.PAGES.SEARCH_QUERY_LANGUAGE}
                                   title="Search query syntax documentation"
                                   text={<Icon name="lightbulb" type="regular" />} />
              </div>
              {hasQueryOverride && <WidgetQueryOverride value={globalOverride?.query} onReset={_resetQueryOverride} />}
            </SecondRow>
          </>
        )}
      </SearchBarForm>
    </>
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
