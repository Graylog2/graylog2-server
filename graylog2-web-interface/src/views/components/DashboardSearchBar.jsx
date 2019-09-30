// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';
import * as Immutable from 'immutable';
import { Col, Row } from 'react-bootstrap';

import connect from 'stores/connect';
import DocumentationLink from 'components/support/DocumentationLink';
import DocsHelper from 'util/DocsHelper';
import Spinner from 'components/common/Spinner';
import ScrollToHint from 'views/components/common/ScrollToHint';
import TimeRangeOverrideTypeSelector from 'views/components/searchbar/TimeRangeOverrideTypeSelector';
import TimeRangeOverrideInput from 'views/components/searchbar/TimeRangeOverrideInput';
import SearchButton from 'views/components/searchbar/SearchButton';
import QueryInput from 'views/components/searchbar/AsyncQueryInput';
import View from 'views/logic/views/View';
import { ViewStore } from 'views/stores/ViewStore';
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
  onExecute: (View) => void,
};

const _performSearch = (onExecute) => {
  const { view } = ViewStore.getInitialState();
  onExecute(view);
};

const DashboardSearchBar = ({ config, currentQuery, disableSearch = false, onExecute }: Props) => {
  if (!config) {
    return <Spinner />;
  }
  const performSearch = () => _performSearch(onExecute);
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
      <Row className="content" style={{ marginRight: 0, marginLeft: 0 }}>
        <Col md={12}>
          <Row className="no-bm">
            <form method="GET" onSubmit={submitForm}>
              <Col md={9}>
                <div className="pull-right search-help">
                  <DocumentationLink page={DocsHelper.PAGES.SEARCH_QUERY_LANGUAGE}
                                     title="Search query syntax documentation"
                                     text={<i className="fa fa-lightbulb-o" />} />
                </div>
                <SearchButton disabled={disableSearch} glyph="filter" />

                <QueryInput value={query.query_string}
                            placeholder="Apply filter to all widgets"
                            onChange={value => GlobalOverrideActions.query(value).then(performSearch).then(() => value)}
                            onExecute={performSearch} />
              </Col>
              <Col md={3}>
                <TimeRangeOverrideTypeSelector onSelect={newRangeType => GlobalOverrideActions.rangeType(newRangeType).then(performSearch)}
                                               value={rangeType} />
                <TimeRangeOverrideInput onChange={(key, value) => GlobalOverrideActions.rangeParams(key, value).then(performSearch)}
                                        rangeType={rangeType}
                                        rangeParams={rangeParams}
                                        config={config} />
              </Col>
            </form>
          </Row>
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
