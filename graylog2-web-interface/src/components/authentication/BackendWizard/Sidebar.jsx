// @flow strict
import * as React from 'react';
import { useState, useContext, useEffect } from 'react';

import { PanelGroup, Panel } from 'components/graylog';

import ServerConnectionCheck from './ServerConnectionCheck';
import UserLoginCheck from './UserLoginCheck';
import BackendWizardContext from './contexts/BackendWizardContext';

const Sidebar = () => {
  const [activeKey, setActiveKey] = useState('serverConfig');
  const { setStepsState, ...stepsState } = useContext(BackendWizardContext);

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

Sidebar.defaultProps = {
  children: 'Hello World!',
};

export default Sidebar;
