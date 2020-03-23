// @flow strict
import * as React from 'react';
import * as Immutable from 'immutable';
import moment from 'moment';
import styled from 'styled-components';

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
import type { TimeRange, TimeRangeTypes } from 'views/logic/queries/Query';
import { GlobalOverrideActions, GlobalOverrideStore } from 'views/stores/GlobalOverrideStore';
import GlobalOverride from 'views/logic/search/GlobalOverride';
import SearchActions from 'views/actions/SearchActions';
import TimeRangeTypeSelector from './searchbar/TimeRangeTypeSelector';
import TimeRangeInput from './searchbar/TimeRangeInput';
import StreamsFilter from './searchbar/StreamsFilter';
import SearchButton from './searchbar/SearchButton';
import QueryInput from './searchbar/AsyncQueryInput';

type Props = {
  availableStreams: Array<any>,
  config: any,
  globalOverride: ?GlobalOverride,
  widget: Widget,
};

const _updateQuery = (id: string, queryString: string) => WidgetActions.query(id, { type: 'elasticsearch', query_string: queryString });
const _updateRangeType = (oldTimerange: ?TimeRange, id: string, newRangeType: TimeRangeTypes) => {
  const { type } = oldTimerange || {};
  if (type === newRangeType) {
    return Promise.resolve();
  }
  let newTimerange: TimeRange = { type: 'relative', range: 300 };
  // eslint-disable-next-line default-case
  switch (newRangeType) {
    case 'absolute':
      newTimerange = {
        type: newRangeType,
        from: moment().subtract(oldTimerange && oldTimerange.type === 'relative' ? oldTimerange.range : 300, 'seconds').toISOString(),
        to: moment().toISOString(),
      };
      break;
    case 'relative':
      newTimerange = {
        type: newRangeType,
        range: 300,
      };
      break;
    case 'keyword':
      newTimerange = {
        type: newRangeType,
        keyword: 'Last five Minutes',
      };
      break;
  }
  return WidgetActions.timerange(id, newTimerange);
};

type Delta = {| range: number |} | {| from: string |} | {| to: string |} | {| keyword: string |};

// $FlowFixMe: Resulting time range could actually be inconsistent/incomplete at this point. Need to fix and improve.
const _updateRangeParams = (rangeType: string, currentTimerange: ?TimeRange, id: string, delta: Delta) => WidgetActions.timerange(id, { ...currentTimerange, type: rangeType, ...delta });

const _updateStreams = (id: string, streams: Array<string>) => WidgetActions.streams(id, streams);

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

const WidgetQueryControls = ({ availableStreams, config, globalOverride = {}, widget }: Props) => {
  const { query, timerange, streams } = widget;
  const disableSearch = false;
  const { type: rangeType = 'relative', ...rest } = timerange || {};
  const rangeParams = Immutable.Map(rest || { range: 300 });
  const { id } = widget;
  const performSearch = () => { };
  const isGloballyOverridden: boolean = globalOverride !== undefined && globalOverride !== null
    && (globalOverride.query !== undefined || globalOverride.timerange !== undefined);
  const Wrapper = isGloballyOverridden ? BlurredWrapper : React.Fragment;
  return (
    <>
      {isGloballyOverridden && <ResetOverrideHint />}
      <Wrapper>
        <Row className="no-bm extended-search-query-metadata">
          <Col md={4}>
            <TimeRangeTypeSelector onSelect={(newRangeType) => _updateRangeType(timerange, id, newRangeType)}
                                   disabled={isGloballyOverridden}
                                   value={rangeType} />
            <TimeRangeInput disabled={isGloballyOverridden}
                            type={rangeType}
                            config={config} />
          </Col>

          <Col md={8}>
            <StreamsFilter value={streams}
                           disabled={isGloballyOverridden}
                           streams={availableStreams}
                           onChange={(value) => _updateStreams(id, value)} />
          </Col>
        </Row>

        <Row className="no-bm">
          <Col md={12}>
            <div className="pull-right search-help">
              <DocumentationLink page={DocsHelper.PAGES.SEARCH_QUERY_LANGUAGE}
                                 title="Search query syntax documentation"
                                 text={<Icon name="lightbulb-o" />} />
            </div>
            <SearchButton disabled={disableSearch || isGloballyOverridden} />

            <QueryInput value={query ? query.query_string : undefined}
                        disabled={isGloballyOverridden}
                        placeholder={'Type your search query here and press enter. E.g.: ("not found" AND http) OR http_response_code:[400 TO 404]'}
                        onChange={(value) => _updateQuery(id, value).then(() => value)}
                        onExecute={performSearch} />
          </Col>
        </Row>
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
