// @flow strict
import * as React from 'react';
import { useState, useContext, useEffect } from 'react';
import styled, { type StyledComponent } from 'styled-components';

import type { ThemeInterface } from 'theme';
import type { LdapCreate } from 'logic/authentication/ldap/types';
import { PanelGroup, Panel } from 'components/graylog';

import { STEP_KEY as SERVER_CONFIG_KEY } from './ServerConfigStep';
import { STEP_KEY as USER_SYNC_KEY } from './UserSyncStep';
import { STEP_KEY as GROUP_SYNC_KEY } from './GroupSyncStep';
import BackendWizardContext from './contexts/BackendWizardContext';
import ServerConnectionTest from './ServerConnectionTest';
import UserLoginTest from './UserLoginTest';

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
type Props = {
  prepareSubmitPayload: () => LdapCreate,
};

const Sidebar = ({ prepareSubmitPayload }: Props) => {
  const [activeKey, setActiveKey] = useState(SERVER_CONFIG_KEY);
  const { setStepsState, ...stepsState } = useContext(BackendWizardContext);

  useEffect(() => {
    setActiveKey(stepsState.activeStepKey);
  }, [stepsState.activeStepKey]);

  return (
    <StyledPanelGroup accordion
                      activeKey={activeKey}
                      id="sidebar-server-response"
                      onSelect={setActiveKey}>
      <Panel eventKey={SERVER_CONFIG_KEY}>
        <Panel.Heading>
          <Panel.Title toggle>Connection Check</Panel.Title>
        </Panel.Heading>
        <Panel.Body collapsible>
          <ServerConnectionTest prepareSubmitPayload={prepareSubmitPayload} />
        </Panel.Body>
      </Panel>
      <Panel eventKey={USER_SYNC_KEY}>
        <Panel.Heading>
          <Panel.Title toggle>User Login Test</Panel.Title>
        </Panel.Heading>
        <Panel.Body collapsible>
          <UserLoginTest prepareSubmitPayload={prepareSubmitPayload} />
        </Panel.Body>
      </Panel>
      <Panel eventKey={GROUP_SYNC_KEY}>
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

export default Sidebar;
