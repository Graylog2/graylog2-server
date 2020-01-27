// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import * as Immutable from 'immutable';

import { Col, Row } from 'components/graylog';
import connect from 'stores/connect';
import DocumentationLink from 'components/support/DocumentationLink';
import DocsHelper from 'util/DocsHelper';
import RefreshControls from 'views/components/searchbar/RefreshControls';
import { Icon, Spinner } from 'components/common';
import ScrollToHint from 'views/components/common/ScrollToHint';
import TimeRangeOverrideTypeSelector from 'views/components/searchbar/TimeRangeOverrideTypeSelector';
import TimeRangeOverrideInput from 'views/components/searchbar/TimeRangeOverrideInput';
import SearchButton from 'views/components/searchbar/SearchButton';
import QueryInput from 'views/components/searchbar/AsyncQueryInput';
import ViewActionsMenu from 'views/components/ViewActionsMenu';
import { GlobalOverrideActions, GlobalOverrideStore } from '../stores/GlobalOverrideStore';

type Props = {
  config: any,
  currentQuery: {
    id: string,
    timerange: any,
    query: {
      query_string: string,
    },
  },
  disableSearch: boolean,
  onExecute: () => void,
};

const DashboardSearchBar = ({ config, currentQuery, disableSearch = false, onExecute: performSearch }: Props) => {
  if (!config) {
    return <Spinner />;
  }
  const submitForm = (event) => {
    event.preventDefault();
    performSearch();
  };
  const { timerange = {}, query = {} } = currentQuery || {};
  const { type, ...rest } = timerange;
  const rangeParams = Immutable.Map(rest);
  const rangeType = type;

  return (
    <ScrollToHint value={query.query_string || ''}>
      <Row className="content">
        <Col md={12}>
          <form method="GET" onSubmit={submitForm}>
            <Row className="no-bm extended-search-query-metadata">
              <Col lg={4} md={6} xs={8}>
                <TimeRangeOverrideTypeSelector onSelect={newRangeType => GlobalOverrideActions.rangeType(newRangeType).then(performSearch)}
                                               value={rangeType} />
                <TimeRangeOverrideInput onChange={(key, value) => GlobalOverrideActions.rangeParams(key, value).then(performSearch)}
                                        rangeType={rangeType}
                                        rangeParams={rangeParams}
                                        config={config} />
              </Col>
              <Col lg={8} md={6} xs={4}>
                <RefreshControls />
              </Col>
            </Row>

            <Row className="no-bm">
              <Col md={9} xs={8}>
                <div className="pull-right search-help">
                  <DocumentationLink page={DocsHelper.PAGES.SEARCH_QUERY_LANGUAGE}
                                     title="Search query syntax documentation"
                                     text={<Icon name="lightbulb-o" />} />
                </div>
                <SearchButton disabled={disableSearch} glyph="filter" />

                <QueryInput value={query.query_string}
                            placeholder="Apply filter to all widgets"
                            onChange={value => GlobalOverrideActions.query(value).then(performSearch).then(() => value)}
                            onExecute={performSearch} />
              </Col>
              <Col md={3} xs={4}>
                <div className="pull-right">
                  <ViewActionsMenu />
                </div>
              </Col>
            </Row>
          </form>
        </Col>
      </Row>
    </ScrollToHint>
  );
};

DashboardSearchBar.propTypes = {
  config: PropTypes.object.isRequired,
  disableSearch: PropTypes.bool,
  onExecute: PropTypes.func.isRequired,
};

DashboardSearchBar.defaultProps = {
  disableSearch: false,
};

export default connect(
  DashboardSearchBar,
  {
    currentQuery: GlobalOverrideStore,
  },
);
