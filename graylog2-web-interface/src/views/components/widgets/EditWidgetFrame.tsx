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

import Spinner from 'components/common/Spinner';
import WidgetContext from 'views/components/contexts/WidgetContext';
import QueryEditModeContext from 'views/components/contexts/QueryEditModeContext';

import WidgetQueryControls from '../WidgetQueryControls';
import IfDashboard from '../dashboard/IfDashboard';
import HeaderElements from '../HeaderElements';
import WidgetOverrideElements from '../WidgetOverrideElements';

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

const EditWidgetFrame = ({ children }: Props) => {
  const widget = useContext(WidgetContext);

  if (!widget) {
    return <Spinner text="Loading widget ..." />;
  }

  return (
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
  );
};

EditWidgetFrame.propTypes = {
  children: PropTypes.node.isRequired,
};

export default EditWidgetFrame;
