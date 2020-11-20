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
import { useState, useContext, useEffect } from 'react';
import styled, { StyledComponent } from 'styled-components';
import { $PropertyType } from 'utility-types';

import { ThemeInterface } from 'theme';
import { WizardSubmitPayload } from 'logic/authentication/directoryServices/types';
import { PanelGroup, Panel } from 'components/graylog';
import { Step } from 'components/common/Wizard';

import { STEP_KEY as SERVER_CONFIG_KEY } from './ServerConfigStep';
import { STEP_KEY as USER_SYNC_KEY } from './UserSyncStep';
import BackendWizardContext, { WizardFormValues } from './BackendWizardContext';
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
  prepareSubmitPayload: (fromValues: WizardFormValues | null | undefined) => WizardSubmitPayload,
};

const Sidebar = ({ prepareSubmitPayload }: Props) => {
  const [activeKey, setActiveKey] = useState<$PropertyType<Step, 'key'>>(SERVER_CONFIG_KEY);
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
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
          <Panel.Title toggle>Server Connection Check</Panel.Title>
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
    </StyledPanelGroup>
  );
};

export default Sidebar;
