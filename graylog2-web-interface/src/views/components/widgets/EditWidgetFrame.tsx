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
import { useContext } from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import moment from 'moment';

import { useStore } from 'stores/connect';
import Spinner from 'components/common/Spinner';
import WidgetContext from 'views/components/contexts/WidgetContext';
import QueryEditModeContext from 'views/components/contexts/QueryEditModeContext';
import { createElasticsearchQueryString } from 'views/logic/queries/Query';
import Widget from 'views/logic/widgets/Widget';
import { WidgetActions } from 'views/stores/WidgetStore';
import { DEFAULT_TIMERANGE } from 'views/Constants';
import { SearchConfigStore } from 'views/stores/SearchConfigStore';

import WidgetQueryControls from '../WidgetQueryControls';
import IfDashboard from '../dashboard/IfDashboard';
import HeaderElements from '../HeaderElements';
import WidgetOverrideElements from '../WidgetOverrideElements';
import SearchBarForm from '../searchbar/SearchBarForm';

const Container = styled.div`
  display: grid;
  display: -ms-grid;
  height: 100%;
  grid-template-columns: 1fr;
  -ms-grid-columns: 1fr;
  grid-template-rows: auto minmax(200px, 1fr) auto;
  -ms-grid-rows: auto minmax(200px, 1fr) auto;
  grid-template-areas: "Query-Controls" "Visualization" "Footer";
`;

const QueryControls = styled.div`
  margin-bottom: 10px;
  grid-area: Query-Controls;
  grid-column: 1;
  -ms-grid-column: 1;
  grid-row: 1;
  -ms-grid-row: 1;
`;

const Visualization = styled.div`
  grid-area: Visualization;
  overflow: hidden;
  grid-column: 1;
  -ms-grid-column: 1;
  grid-row: 2;
  -ms-grid-row: 2;
`;

const Footer = styled.div`
  grid-area: Footer;
  grid-column: 1;
  -ms-grid-column: 1;
  grid-row: 3;
  -ms-grid-row: 3;
`;

type Props = {
  children: Array<React.ReactNode>,
};

const onSubmit = (values, widget: Widget) => {
  const { timerange, streams, queryString } = values;
  const newWidget = widget.toBuilder()
    .timerange(timerange)
    .query(createElasticsearchQueryString(queryString))
    .streams(streams)
    .build();

  return WidgetActions.update(widget.id, newWidget);
};

const EditWidgetFrame = ({ children }: Props) => {
  const config = useStore(SearchConfigStore, ({ searchesClusterConfig }) => searchesClusterConfig);

  const widget = useContext(WidgetContext);

  if (!widget) {
    return <Spinner text="Loading widget ..." />;
  }

  const limitDuration = moment.duration(config?.query_time_range_limit).asSeconds() ?? 0;
  const { streams } = widget;
  const timerange = widget.timerange ?? DEFAULT_TIMERANGE;
  const { query_string: queryString } = widget.query ?? createElasticsearchQueryString('');
  const _onSubmit = (values) => onSubmit(values, widget);

  return (
    <SearchBarForm initialValues={{ timerange, streams, queryString }}
                   limitDuration={limitDuration}
                   onSubmit={_onSubmit}
                   validateOnMount={false}>
      <Container>
        <IfDashboard>
          <QueryControls>
            <QueryEditModeContext.Provider value="widget">
              <HeaderElements />
              <WidgetQueryControls />
            </QueryEditModeContext.Provider>
          </QueryControls>
        </IfDashboard>
        <Visualization>
          <div role="presentation" style={{ height: '100%' }}>
            <WidgetOverrideElements>
              {children[0]}
            </WidgetOverrideElements>
          </div>
        </Visualization>
        <Footer>
          {children[1]}
        </Footer>
      </Container>
    </SearchBarForm>
  );
};

EditWidgetFrame.propTypes = {
  children: PropTypes.node.isRequired,
};

export default EditWidgetFrame;
