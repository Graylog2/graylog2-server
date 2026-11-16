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
import React from 'react';
import styled from 'styled-components';
import { Outlet } from 'react-router-dom';

import { ScratchpadProvider } from 'contexts/ScratchpadProvider';
import { Spinner } from 'components/common';
import Scratchpad from 'components/scratchpad/Scratchpad';
import CurrentUserContext from 'contexts/CurrentUserContext';
import Navigation from 'components/navigation/Navigation';
import ReportedErrorBoundary from 'components/errors/ReportedErrorBoundary';
import RuntimeErrorBoundary from 'components/errors/RuntimeErrorBoundary';
import NavigationTelemetry from 'logic/telemetry/NavigationTelemetry';
import TelemetryDebugOverlay from 'components/telemetry-debug/TelemetryDebugOverlay';
import HotkeysProvider from 'contexts/HotkeysProvider';
import HotkeysModalContainer from 'components/hotkeys/HotkeysModalContainer';
import PageContextProviders from 'components/page/contexts/PageContextProviders';
import { singleton } from 'logic/singleton';
import DefaultQueryParamProvider from 'routing/DefaultQueryParamProvider';
import RightSidebarProvider from 'contexts/RightSidebarProvider';
import RightSidebar from 'components/layout/RightSidebar';
import RightSidebarContext from 'contexts/RightSidebarContext';
import { AssistantPageContextProvider } from 'components/assistant';
import usePluginEntities from 'hooks/usePluginEntities';

const AppLayout = styled.div`
  display: flex;
  flex-direction: column;
  height: 100%;
`;

const ContentRow = styled.div`
  display: flex;
  flex-direction: row;
  flex: 1;
  overflow: auto;
`;

const MainContentColumn = styled.div`
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
  min-height: 0;
`;

const PageContent = styled.div`
  flex: 1;
  min-height: 0;
`;

// Sidebars shown next to every page, e.g. the Assistant's chat, which stays open across pages.
const GlobalSidebars = () => {
  const sidebars = usePluginEntities('globalSidebars');

  return (
    <>
      {sidebars.map(({ key, component: Sidebar }) => (
        <Sidebar key={key} />
      ))}
    </>
  );
};

const App = () => (
  <DefaultQueryParamProvider>
    <CurrentUserContext.Consumer>
      {(currentUser) => {
        if (!currentUser) {
          return <Spinner />;
        }

        return (
          <AssistantPageContextProvider>
            <RightSidebarProvider>
              <HotkeysProvider>
                <ScratchpadProvider loginName={currentUser.username}>
                  <NavigationTelemetry />
                  <>
                    <AppLayout>
                      <Navigation />
                      <ReportedErrorBoundary>
                        <ContentRow>
                          <MainContentColumn>
                            <Scratchpad />
                            <RuntimeErrorBoundary>
                              <PageContextProviders>
                                <PageContent>
                                  <Outlet />
                                </PageContent>
                              </PageContextProviders>
                            </RuntimeErrorBoundary>
                          </MainContentColumn>
                          <RightSidebarContext.Consumer>
                            {({ isOpen }) => isOpen && <RightSidebar />}
                          </RightSidebarContext.Consumer>
                          <GlobalSidebars />
                        </ContentRow>
                      </ReportedErrorBoundary>
                    </AppLayout>
                    <HotkeysModalContainer />
                    <TelemetryDebugOverlay />
                  </>
                </ScratchpadProvider>
              </HotkeysProvider>
            </RightSidebarProvider>
          </AssistantPageContextProvider>
        );
      }}
    </CurrentUserContext.Consumer>
  </DefaultQueryParamProvider>
);

export default singleton('components.App', () => App);
