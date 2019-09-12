// @flow strict
import * as React from 'react';
import * as Immutable from 'immutable';

import connect from 'stores/connect';
import { Col, Row } from 'components/graylog/index';
import DocumentationLink from 'components/support/DocumentationLink';
import DocsHelper from 'util/DocsHelper';

import Widget from 'views/logic/widgets/Widget';
import { QueriesActions } from 'views/actions/QueriesActions';
import { QueryFiltersActions } from 'views/stores/QueryFiltersStore';
import { StreamsStore } from 'views/stores/StreamsStore';
import { SearchConfigStore } from 'views/stores/SearchConfigStore';
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

const WidgetQueryControls = ({ availableStreams, config, widget }: Props) => {
  const { query, timerange } = widget;
  const disableSearch = false;
  const rangeType = timerange ? timerange.type : 'relative';
  const rangeParams = Immutable.Map(timerange || { range: 300 });
  const streams = widget.filter
    .filter(f => f.get('type') === 'stream')
    .map(f => f.get('id'))
    .toJS();
  const { id } = widget;
  const performSearch = () => {};
  return (
    <React.Fragment>
      <Row className="no-bm extended-search-query-metadata">
        <Col md={4}>
          <TimeRangeTypeSelector onSelect={newRangeType => QueriesActions.rangeType(id, newRangeType).then(performSearch)}
                                 value={rangeType} />
          <TimeRangeInput onChange={(key, value) => QueriesActions.rangeParams(id, key, value).then(performSearch)}
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
                      onChange={value => QueriesActions.query(id, value).then(performSearch).then(() => value)}
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
