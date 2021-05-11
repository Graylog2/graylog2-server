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
import PropTypes from 'prop-types';
import React from 'react';
import styled, { css } from 'styled-components';
import chroma from 'chroma-js';

import { ScratchpadProvider } from 'contexts/ScratchpadProvider';
import { Scratchpad, Icon, Spinner } from 'components/common';
import CurrentUserContext from 'contexts/CurrentUserContext';
import Navigation from 'components/navigation/Navigation';
import ReportedErrorBoundary from 'components/errors/ReportedErrorBoundary';
import RuntimeErrorBoundary from 'components/errors/RuntimeErrorBoundary';
import DefaultQueryClientProvider from 'contexts/DefaultQueryClientProvider';

import 'stylesheets/typeahead.less';

const AppLayout = styled.div`
  display: flex;
  flex-direction: column;
  height: 100%;
`;

const PageContent = styled.div`
  height: 100%;
  overflow: auto;
`;

const ScrollToHint = styled.div(({ theme }) => css`
  position: fixed;
  left: 50%;
  margin-left: -125px;
  top: 50px;
  color: ${theme.utils.readableColor(chroma(theme.colors.brand.tertiary).alpha(0.8).css())};
  font-size: 80px;
  padding: 25px;
  z-index: 2000;
  width: 200px;
  text-align: center;
  cursor: pointer;
  border-radius: 10px;
  display: none;
  background: ${chroma(theme.colors.brand.tertiary).alpha(0.8).css()};
`);

const App = ({ children }) => (
  <CurrentUserContext.Consumer>
    {(currentUser) => {
      if (!currentUser) {
        return <Spinner />;
      }

      return (
        <DefaultQueryClientProvider>
          <ScratchpadProvider loginName={currentUser.username}>
            <AppLayout>
              <Navigation />
              <ScrollToHint id="scroll-to-hint">
                <Icon name="arrow-up" />
              </ScrollToHint>
              <Scratchpad />
              <ReportedErrorBoundary>
                <RuntimeErrorBoundary>
                  <PageContent>
                    {children}
                  </PageContent>
                </RuntimeErrorBoundary>
              </ReportedErrorBoundary>
            </AppLayout>
          </ScratchpadProvider>
        </DefaultQueryClientProvider>
      );
    }}
  </CurrentUserContext.Consumer>
);

App.propTypes = {
  children: PropTypes.oneOfType([
    PropTypes.arrayOf(PropTypes.element),
    PropTypes.element,
  ]).isRequired,
};

export default App;
