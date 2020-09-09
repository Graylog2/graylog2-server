// @flow strict
import * as React from 'react';
import { useState, useContext, useEffect } from 'react';

import { PanelGroup, Panel } from 'components/graylog';

import ServerConnectionCheck from './ServerConnectionCheck';
import UserLoginCheck from './UserLoginCheck';

import ServiceStepsContext from '../contexts/ServiceStepsContext';

const SidebarConnectionCheck = () => {
  const [activeKey, setActiveKey] = useState('serverConfig');
  const { setStepsState, ...stepsState } = useContext(ServiceStepsContext);

  useEffect(() => {
    setActiveKey(stepsState.activeStepKey);
  }, [stepsState.activeStepKey]);

  return (
    <PanelGroup accordion
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
    </PanelGroup>
  );
};

SidebarConnectionCheck.defaultProps = {
  children: 'Hello World!',
};

export default SidebarConnectionCheck;
