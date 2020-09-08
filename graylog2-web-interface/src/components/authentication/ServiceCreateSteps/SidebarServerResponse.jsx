// @flow strict
import * as React from 'react';
import { useState } from 'react';

import { PanelGroup, Panel } from 'components/graylog';

import ServerConnectionCheck from './ServerConnectionCheck';
import UserLoginCheck from './UserLoginCheck';

const SidebarConnectionCheck = () => {
  const [activeKey, setActiveKey] = useState();

  return (
    <PanelGroup accordion
                activeKey={activeKey}
                id="sidebar-server-response"
                onSelect={setActiveKey}>
      <Panel eventKey="1">
        <Panel.Heading>
          <Panel.Title toggle>Connection Check</Panel.Title>
        </Panel.Heading>
        <Panel.Body collapsible>
          <ServerConnectionCheck />
        </Panel.Body>
      </Panel>
      <Panel eventKey="2">
        <Panel.Heading>
          <Panel.Title toggle>User Login Test</Panel.Title>
        </Panel.Heading>
        <Panel.Body collapsible>
          <UserLoginCheck />
        </Panel.Body>
      </Panel>
    </PanelGroup>
  );
};

SidebarConnectionCheck.defaultProps = {
  children: 'Hello World!',
};

export default SidebarConnectionCheck;
