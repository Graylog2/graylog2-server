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

import SidebarNavigationLink from 'components/layout/RightSidebar/SidebarNavigationLink';
import Button from 'components/bootstrap/Button';
import useSidebarNavigation from 'hooks/useSidebarNavigation';

type Props = {
  message?: string;
  userId?: string;
};

const ContentSection = styled.div`
  margin-bottom: 20px;
`;

const NavigationDemo = styled.div`
  margin-top: 20px;
  padding: 15px;
  background-color: ${({ theme }) => theme.colors.global.contentBackground};
  border-radius: 4px;
`;

const NavList = styled.ul`
  list-style: none;
  padding: 0;
  margin: 10px 0;

  li {
    margin: 8px 0;
  }
`;

const ButtonGroup = styled.div`
  margin-top: 15px;
  display: flex;
  gap: 10px;
`;

const UserDetailsContent = ({ userId }: { userId: string }) => {
  const { navigateTo } = useSidebarNavigation();

  const showSettings = () => {
    navigateTo({
      id: 'user-settings',
      title: 'User Settings',
      component: UserSettingsContent,
      props: { userId },
    });
  };

  const showActivity = () => {
    navigateTo({
      id: 'user-activity',
      title: 'User Activity',
      component: UserActivityContent,
      props: { userId },
    });
  };

  return (
    <div>
      <h4>User Details</h4>
      <p>User ID: {userId}</p>
      <p>This page shows detailed information about the user.</p>

      <NavigationDemo>
        <h5>Navigate to related pages:</h5>
        <NavList>
          <li>
            <SidebarNavigationLink
              content={{
                id: 'user-settings',
                title: 'User Settings',
                component: UserSettingsContent,
                props: { userId },
              }}>
              View User Settings
            </SidebarNavigationLink>
          </li>
          <li>
            <SidebarNavigationLink
              content={{
                id: 'user-activity',
                title: 'User Activity',
                component: UserActivityContent,
                props: { userId },
              }}>
              View Activity Log
            </SidebarNavigationLink>
          </li>
        </NavList>

        <h5>Or use buttons:</h5>
        <ButtonGroup>
          <Button bsSize="small" onClick={showSettings}>
            Settings
          </Button>
          <Button bsSize="small" onClick={showActivity}>
            Activity
          </Button>
        </ButtonGroup>
      </NavigationDemo>
    </div>
  );
};

const UserSettingsContent = ({ userId }: { userId: string }) => {
  const { navigateTo } = useSidebarNavigation();

  return (
    <div>
      <h4>User Settings</h4>
      <p>User ID: {userId}</p>
      <p>Configure user preferences and settings here.</p>

      <NavigationDemo>
        <h5>Settings Categories:</h5>
        <NavList>
          <li>
            <SidebarNavigationLink
              content={{
                id: 'notification-settings',
                title: 'Notification Settings',
                component: NotificationSettingsContent,
                props: { userId },
              }}>
              Notification Preferences
            </SidebarNavigationLink>
          </li>
          <li>
            <SidebarNavigationLink
              content={{
                id: 'security-settings',
                title: 'Security Settings',
                component: SecuritySettingsContent,
                props: { userId },
              }}>
              Security & Privacy
            </SidebarNavigationLink>
          </li>
          <li>
            <Button
              bsStyle="link"
              onClick={() =>
                navigateTo({
                  id: 'user-details',
                  title: 'User Details',
                  component: UserDetailsContent,
                  props: { userId },
                })
              }>
              Back to User Details
            </Button>
          </li>
        </NavList>
      </NavigationDemo>
    </div>
  );
};

const UserActivityContent = ({ userId }: { userId: string }) => (
  <div>
    <h4>User Activity Log</h4>
    <p>User ID: {userId}</p>
    <p>Recent activity and actions performed by this user.</p>

    <NavigationDemo>
      <h5>Activity Details:</h5>
      <NavList>
        <li>Login: 2 hours ago</li>
        <li>Created dashboard: 5 hours ago</li>
        <li>Updated search query: 1 day ago</li>
      </NavList>

      <h5>Related Pages:</h5>
      <NavList>
        <li>
          <SidebarNavigationLink
            content={{
              id: 'user-details',
              title: 'User Details',
              component: UserDetailsContent,
              props: { userId },
            }}>
            Back to User Details
          </SidebarNavigationLink>
        </li>
      </NavList>
    </NavigationDemo>
  </div>
);

const NotificationSettingsContent = ({ userId }: { userId: string }) => (
  <div>
    <h4>Notification Settings</h4>
    <p>User ID: {userId}</p>
    <p>Configure how and when you receive notifications.</p>

    <NavigationDemo>
      <h5>Notification Types:</h5>
      <NavList>
        <li>Email notifications: Enabled</li>
        <li>Browser notifications: Disabled</li>
        <li>Alert notifications: Enabled</li>
      </NavList>

      <h5>Navigate:</h5>
      <NavList>
        <li>
          <SidebarNavigationLink
            content={{
              id: 'user-settings',
              title: 'User Settings',
              component: UserSettingsContent,
              props: { userId },
            }}>
            Back to Settings
          </SidebarNavigationLink>
        </li>
      </NavList>
    </NavigationDemo>
  </div>
);

const SecuritySettingsContent = ({ userId }: { userId: string }) => (
  <div>
    <h4>Security Settings</h4>
    <p>User ID: {userId}</p>
    <p>Manage security settings and privacy preferences.</p>

    <NavigationDemo>
      <h5>Security Options:</h5>
      <NavList>
        <li>Two-factor authentication: Enabled</li>
        <li>Session timeout: 30 minutes</li>
        <li>Last password change: 30 days ago</li>
      </NavList>

      <h5>Navigate:</h5>
      <NavList>
        <li>
          <SidebarNavigationLink
            content={{
              id: 'user-settings',
              title: 'User Settings',
              component: UserSettingsContent,
              props: { userId },
            }}>
            Back to Settings
          </SidebarNavigationLink>
        </li>
      </NavList>
    </NavigationDemo>
  </div>
);

const ExampleSidebarContent = ({ message = 'Test Sidebar Content', userId = undefined }: Props) => (
  <div>
    <ContentSection>
      <h4>Example Sidebar</h4>
      <p>{message}</p>
      {userId && <p>User ID: {userId}</p>}
      <p>This is a test component to verify the sidebar functionality works correctly.</p>
    </ContentSection>

    {userId && (
      <NavigationDemo>
        <h5>Try the Navigation History:</h5>
        <p>Click the links below to navigate to different content. Use the back/forward arrows in the sidebar header to navigate through your history.</p>
        <NavList>
          <li>
            <SidebarNavigationLink
              content={{
                id: 'user-details',
                title: 'User Details',
                component: UserDetailsContent,
                props: { userId },
              }}>
              View User Details
            </SidebarNavigationLink>
            {' - Explore user information with nested navigation'}
          </li>
          <li>
            <SidebarNavigationLink
              content={{
                id: 'user-settings',
                title: 'User Settings',
                component: UserSettingsContent,
                props: { userId },
              }}>
              View User Settings
            </SidebarNavigationLink>
            {' - Configure preferences with multiple sub-pages'}
          </li>
          <li>
            <SidebarNavigationLink
              content={{
                id: 'user-activity',
                title: 'User Activity',
                component: UserActivityContent,
                props: { userId },
              }}>
              View Activity Log
            </SidebarNavigationLink>
            {' - See recent user actions'}
          </li>
        </NavList>
      </NavigationDemo>
    )}
  </div>
);

export default ExampleSidebarContent;
