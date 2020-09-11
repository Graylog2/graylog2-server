// @flow strict
import * as React from 'react';
import { useState, useContext, useEffect } from 'react';
import styled, { type StyledComponent } from 'styled-components';

import { PanelGroup, Panel } from 'components/graylog';
import type { ThemeInterface } from 'theme';

import ServerConnectionCheck from './ServerConnectionCheck';
import UserLoginCheck from './UserLoginCheck';
import BackendWizardContext from './contexts/BackendWizardContext';

const StyledPanelGroup: StyledComponent<{}, ThemeInterface, PanelGroup> = styled(PanelGroup)`
  &.panel-group .panel {
    margin-top: 0;
    border-color: ${(props) => props.theme.colors.input.border};
    background-color: ${(props) => props.theme.colors.global.contentBackground};

    .panel-heading {
      background-color: ${(props) => props.theme.colors.table.backgroundAlt};
    }

    &:not(:first-child) {
      border-top: 0;
      border-top-left-radius: 0;
      border-top-right-radius: 0;
    }

    &:not(:last-child) {
      border-bottom-left-radius: 0;
      border-bottom-right-radius: 0;
    }
  }
`;

const Sidebar = () => {
  const [activeKey, setActiveKey] = useState('serverConfig');
  const { setStepsState, ...stepsState } = useContext(BackendWizardContext);

  useEffect(() => {
    setActiveKey(stepsState.activeStepKey);
  }, [stepsState.activeStepKey]);

  return (
    <StyledPanelGroup accordion
                      activeKey={activeKey}
                      id="sidebar-server-response"
                      onSelect={setActiveKey}>
      <Panel eventKey="serverConfig">
        <Panel.Heading>
          <Panel.Title toggle>Connection Check</Panel.Title>
        </Panel.Heading>
        <Panel.Body collapsible>
          <ServerConnectionCheck />
        </Panel.Body>
      </Panel>
      <Panel eventKey="userSync">
        <Panel.Heading>
          <Panel.Title toggle>User Login Test</Panel.Title>
        </Panel.Heading>
        <Panel.Body collapsible>
          <UserLoginCheck />
        </Panel.Body>
      </Panel>
      <Panel eventKey="groupSync">
        <Panel.Heading>
          <Panel.Title toggle>Grouping Review</Panel.Title>
        </Panel.Heading>
        <Panel.Body collapsible>
          You will find information about grouping here.
        </Panel.Body>
      </Panel>
    </StyledPanelGroup>
  );
};

Sidebar.defaultProps = {
  children: 'Hello World!',
};

export default Sidebar;
