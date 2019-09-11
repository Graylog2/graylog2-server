// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import * as Immutable from 'immutable';

import { Col, Row } from '../../components/graylog/index';
import TimeRangeTypeSelector from './searchbar/TimeRangeTypeSelector';
import { QueriesActions } from '../actions/QueriesActions';
import TimeRangeInput from './searchbar/TimeRangeInput';
import StreamsFilter from './searchbar/StreamsFilter';
import { QueryFiltersActions } from '../stores/QueryFiltersStore';
import DocumentationLink from '../../components/support/DocumentationLink';
import DocsHelper from '../../util/DocsHelper';
import SearchButton from './searchbar/SearchButton';
import QueryInput from './searchbar/AsyncQueryInput';
import { StreamsStore } from '../stores/StreamsStore';
import connect from '../../stores/connect';
import { SearchConfigStore } from '../stores/SearchConfigStore';

const WidgetQueryControls = ({ availableStreams, config }) => {
  const query = { type: 'elasticsearch', query_string: 'Hello!' };
  const disableSearch = false;
  const rangeType = 'relative';
  const rangeParams = Immutable.Map({ range: 300 });
  const streams = [];
  const id = 'deadbeef';
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

          <QueryInput value={query.query_string}
                      placeholder={'Type your search query here and press enter. E.g.: ("not found" AND http) OR http_response_code:[400 TO 404]'}
                      onChange={value => QueriesActions.query(id, value).then(performSearch).then(() => value)}
                      onExecute={performSearch} />
        </Col>
      </Row>
    </React.Fragment>
  );
};

WidgetQueryControls.propTypes = {};

export default connect(WidgetQueryControls,
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
