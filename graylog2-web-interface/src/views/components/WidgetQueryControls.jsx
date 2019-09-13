// @flow strict
import * as React from 'react';
import * as Immutable from 'immutable';
import moment from 'moment';

import connect from 'stores/connect';
import { Col, Row } from 'components/graylog/index';
import DocumentationLink from 'components/support/DocumentationLink';
import DocsHelper from 'util/DocsHelper';

import Widget from 'views/logic/widgets/Widget';
import { QueryFiltersActions } from 'views/stores/QueryFiltersStore';
import { StreamsStore } from 'views/stores/StreamsStore';
import { SearchConfigStore } from 'views/stores/SearchConfigStore';
import { WidgetActions } from 'views/stores/WidgetStore';
import type { TimeRange, TimeRangeTypes } from 'views/logic/queries/Query';
import TimeRangeTypeSelector from './searchbar/TimeRangeTypeSelector';
import TimeRangeInput from './searchbar/TimeRangeInput';
import StreamsFilter from './searchbar/StreamsFilter';
import SearchButton from './searchbar/SearchButton';
import QueryInput from './searchbar/AsyncQueryInput';

type Props = {
  availableStreams: Array<any>,
  config: any,
  widget: Widget,
};

const _updateQuery = (id: string, queryString: string) => WidgetActions.query(id, { type: 'elasticsearch', query_string: queryString });
const _updateRangeType = (oldTimerange: TimeRange, id: string, newRangeType: TimeRangeTypes) => {
  const { type } = oldTimerange || {};
  if (type === newRangeType) {
    return Promise.resolve();
  }
  let newTimerange: TimeRange;
  // eslint-disable-next-line default-case
  switch (newRangeType) {
    case 'absolute':
      newTimerange = {
        type: newRangeType,
        from: moment().subtract(oldTimerange ? oldTimerange.range : 300, 'seconds').toISOString(),
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

const _updateRangeParams = (currentTimerange: TimeRange, id: string, delta: Delta) => WidgetActions.timerange(id, { ...currentTimerange, ...delta });

const WidgetQueryControls = ({ availableStreams, config, widget }: Props) => {
  const { query, timerange } = widget;
  const disableSearch = false;
  const { type: rangeType = 'relative', ...rest } = timerange || {};
  const rangeParams = Immutable.Map(rest || { range: 300 });
  const streams = [];
  const { id } = widget;
  const performSearch = () => {};
  return (
    <React.Fragment>
      <Row className="no-bm extended-search-query-metadata">
        <Col md={4}>
          <TimeRangeTypeSelector onSelect={newRangeType => _updateRangeType(timerange, id, newRangeType).then(performSearch)}
                                 value={rangeType} />
          <TimeRangeInput onChange={(key, value) => _updateRangeParams(timerange, id, { [key]: value }).then(performSearch)}
                          rangeType={rangeType}
                          rangeParams={rangeParams}
                          config={config} />
        </Col>

        <Col md={8}>
          <StreamsFilter value={streams}
                         streams={availableStreams}
                         onChange={value => QueryFiltersActions.streams(id, value)} />
        </Col>
      </Row>

      <Row className="no-bm">
        <Col md={12}>
          <div className="pull-right search-help">
            <DocumentationLink page={DocsHelper.PAGES.SEARCH_QUERY_LANGUAGE}
                               title="Search query syntax documentation"
                               text={<i className="fa fa-lightbulb-o" />} />
          </div>
          <SearchButton disabled={disableSearch} />

          <QueryInput value={query ? query.query_string : undefined}
                      placeholder={'Type your search query here and press enter. E.g.: ("not found" AND http) OR http_response_code:[400 TO 404]'}
                      onChange={value => _updateQuery(id, value).then(performSearch).then(() => value)}
                      onExecute={performSearch} />
        </Col>
      </Row>
    </React.Fragment>
  );
};

WidgetQueryControls.propTypes = {};

export default connect(
  WidgetQueryControls,
  {
    availableStreams: StreamsStore,
    configurations: SearchConfigStore,
  },
  ({ availableStreams: { streams }, configurations, ...rest }) => ({
    ...rest,
    availableStreams: streams.map(stream => ({ key: stream.title, value: stream.id })),
    config: configurations.searchesClusterConfig,
  }),
);
