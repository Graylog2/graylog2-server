// @flow strict
import * as React from 'react';
import { useCallback } from 'react';
import styled from 'styled-components';
import { Field } from 'formik';

import connect from 'stores/connect';
import { Col, Row } from 'components/graylog';
import { Icon } from 'components/common';
import DocumentationLink from 'components/support/DocumentationLink';
import DocsHelper from 'util/DocsHelper';
import Button from 'components/graylog/Button';

import Widget from 'views/logic/widgets/Widget';
import { StreamsStore } from 'views/stores/StreamsStore';
import { SearchConfigStore } from 'views/stores/SearchConfigStore';
import { WidgetActions } from 'views/stores/WidgetStore';
import { GlobalOverrideActions, GlobalOverrideStore } from 'views/stores/GlobalOverrideStore';
import GlobalOverride from 'views/logic/search/GlobalOverride';
import SearchActions from 'views/actions/SearchActions';
import { createElasticsearchQueryString } from 'views/logic/queries/Query';
import TimeRangeTypeSelector from './searchbar/TimeRangeTypeSelector';
import TimeRangeInput from './searchbar/TimeRangeInput';
import StreamsFilter from './searchbar/StreamsFilter';
import SearchButton from './searchbar/SearchButton';
import QueryInput from './searchbar/AsyncQueryInput';
import { DEFAULT_TIMERANGE } from '../Constants';
import SearchBarForm from './searchbar/SearchBarForm';

type Props = {
  availableStreams: Array<any>,
  config: any,
  globalOverride: ?GlobalOverride,
  widget: Widget,
};

const BlurredWrapper = styled.div`
  filter: blur(4px);
`;

const CenteredBox = styled.div`
  position: absolute;
  background: white;
  padding: 10px 15px 10px 15px;
  border-color: lightgray;
  border-radius: 2px;
  border-width: 1px;
  border-style: solid;
  box-shadow: 3px 3px 3px darkgrey;
  z-index: 1;
  left: 0;
  right: 0;
  width: max-content;
  margin: 0 auto;
`;

const ResetFilterButton = styled(Button)`
  margin-left: 5px;
  vertical-align: initial;
`;

const _resetOverride = () => GlobalOverrideActions.reset().then(SearchActions.refresh);

const ResetOverrideHint = () => (
  <CenteredBox>
    These controls are disabled, because a filter is applied to all widgets.{' '}
    <ResetFilterButton bsSize="xs" bsStyle="primary" data-testid="reset-filter" onClick={_resetOverride}>Reset filter</ResetFilterButton>
  </CenteredBox>
);

const onSubmit = (values, widget: Widget) => {
  const { timerange, streams, queryString } = values;
  const newWidget = widget.toBuilder()
    .timerange(timerange)
    .query(createElasticsearchQueryString(queryString))
    .streams(streams)
    .build();

  return WidgetActions.update(widget.id, newWidget);
};

const WidgetQueryControls = ({ availableStreams, config, globalOverride = {}, widget }: Props) => {
  const { streams } = widget;
  const timerange = widget.timerange || DEFAULT_TIMERANGE;
  const { query_string: queryString } = widget.query || createElasticsearchQueryString('');

  const isGloballyOverridden: boolean = globalOverride !== undefined
    && globalOverride !== null
    && (globalOverride.query !== undefined || globalOverride.timerange !== undefined);
  const Wrapper = isGloballyOverridden ? BlurredWrapper : React.Fragment;

  const _onSubmit = useCallback((values) => onSubmit(values, widget), [widget]);
  return (
    <>
      {isGloballyOverridden && <ResetOverrideHint />}
      <Wrapper>
        <SearchBarForm initialValues={{ timerange, streams, queryString }}
                       onSubmit={_onSubmit}>
          {({ isSubmitting, isValid, handleSubmit }) => (
            <>
              <Row className="no-bm extended-search-query-metadata">
                <Col md={4}>
                  <TimeRangeTypeSelector disabled={isGloballyOverridden} />
                  <TimeRangeInput disabled={isGloballyOverridden} config={config} />
                </Col>

                <Col md={8}>
                  <Field name="streams">
                    {({ field: { name, value, onChange } }) => (
                      <StreamsFilter value={value}
                                     disabled={isGloballyOverridden}
                                     streams={availableStreams}
                                     onChange={(newStreams) => onChange({ target: { value: newStreams, name } })} />
                    )}
                  </Field>
                </Col>
              </Row>

              <Row className="no-bm">
                <Col md={12}>
                  <div className="pull-right search-help">
                    <DocumentationLink page={DocsHelper.PAGES.SEARCH_QUERY_LANGUAGE}
                                       title="Search query syntax documentation"
                                       text={<Icon name="lightbulb-o" />} />
                  </div>
                  <SearchButton disabled={isGloballyOverridden || isSubmitting || !isValid} />

                  <Field name="queryString">
                    {({ field: { name, value, onChange } }) => (
                      <QueryInput value={value}
                                  disabled={isGloballyOverridden}
                                  placeholder={'Type your search query here and press enter. E.g.: ("not found" AND http) OR http_response_code:[400 TO 404]'}
                                  onChange={(newQuery) => onChange({ target: { value: newQuery, name } })}
                                  onExecute={handleSubmit} />
                    )}
                  </Field>
                </Col>
              </Row>
            </>
          )}
        </SearchBarForm>
      </Wrapper>
    </>
  );
};

WidgetQueryControls.propTypes = {};

export default connect(
  WidgetQueryControls,
  {
    availableStreams: StreamsStore,
    configurations: SearchConfigStore,
    globalOverride: GlobalOverrideStore,
  },
  ({ availableStreams: { streams = [] }, configurations, ...rest }) => ({
    ...rest,
    availableStreams: streams.map((stream) => ({ key: stream.title, value: stream.id })),
    config: configurations.searchesClusterConfig,
  }),
);
