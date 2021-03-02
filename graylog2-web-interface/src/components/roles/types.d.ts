import * as React from 'react';

import Role from 'logic/roles/Role';
import User from 'logic/users/User';

interface RoleTeamsAssignmentProps {
  role: Role;
  readOnly?: boolean;
}

interface UserTeamsAssignmentProps {
  user: User;
  readOnly?: boolean;
}

interface TeamsPlugin {
  RoleTeamsAssignment?: React.ComponentType<RoleTeamsAssignmentProps>;
  UserTeamsAssignment?: React.ComponentType<UserTeamsAssignmentProps>;
}
declare module 'graylog-web-plugin/plugin' {
  interface PluginExports {
    teams?: TeamsPlugin;
  }
}
