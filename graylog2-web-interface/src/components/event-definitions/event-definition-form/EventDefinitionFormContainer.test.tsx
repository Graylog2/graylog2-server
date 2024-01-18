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
import { render, screen } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';
import { defaultUser as mockDefaultUser } from 'defaultMockValues';

import { simpleEventDefinition as mockEventDefinition } from 'fixtures/eventDefinition';

import EventDefinitionFormContainer from './EventDefinitionFormContainer';

type entityScope = {
  is_mutable: boolean;
};

type getPermissionsByScopeReturnType = {
  loadingScopePermissions: boolean;
  scopePermissions: entityScope;
};

const mockAggregationEventDefinition = {
  ...mockEventDefinition,
  config: {
    ...mockEventDefinition.config,
    query: 'http_response_code:400',
  },
};

const exampleEntityScopeMutable: getPermissionsByScopeReturnType = {
  loadingScopePermissions: false,
  scopePermissions: { is_mutable: true },
};

const exampleEntityScopeImmutable: getPermissionsByScopeReturnType = {
  loadingScopePermissions: false,
  scopePermissions: { is_mutable: false },
};

const mockNavigate = jest.fn();

jest.mock('react-router-dom', () => {
  const original = jest.requireActual('react-router-dom');

  return {
    ...original,
    useNavigate: () => mockNavigate,
  };
});

jest.mock('routing/useLocation', () => {
  const original = jest.requireActual('routing/useLocation');

  return {
    __esModule: true,
    ...original,
    default: jest.fn(() => ({ pathname: '/event-definitions' })),
  };
});

jest.mock('logic/telemetry/useSendTelemetry', () => {
  const original = jest.requireActual('logic/telemetry/useSendTelemetry');

  return {
    __esModule: true,
    ...original,
    default: () => jest.fn(),
  };
});

const mockUseScopePermissions = jest.fn(() => exampleEntityScopeMutable);

jest.mock('hooks/useScopePermissions', () => {
  const original = jest.requireActual('hooks/useScopePermissions');

  return {
    __esModule: true,
    ...original,
    default: () => mockUseScopePermissions(),
  };
});

jest.mock('hooks/useCurrentUser', () => {
  const original = jest.requireActual('hooks/useCurrentUser');

  return {
    __esModule: true,
    ...original,
    default: jest.fn(() => mockDefaultUser),
  };
});

jest.mock('components/event-definitions/hooks/useEventDefinitionConfigFromLocalStorage', () => {
  const original = jest.requireActual('components/event-definitions/hooks/useEventDefinitionConfigFromLocalStorage');

  return {
    __esModule: true,
    ...original,
    default: jest.fn(() => ({ hasLocalStorageConfig: false, configFormLocalStorage: {} })),
  };
});

jest.mock('stores/connect', () => ({
  __esModule: true,
  useStore: jest.fn((store) => store.getInitialState()),
  default: jest.fn((
    Component: React.ComponentType<React.ComponentProps<any>>,
    stores: { [key: string]: any },
    _mapProps: (args: { [key: string]: any }) => any,
  ) => {
    const storeProps = Object.entries(stores).reduce((acc, [key, store]) => ({ ...acc, [key]: store.getInitialState() }), {});
    const componentProps = { ...storeProps, eventDefinition: { ...mockEventDefinition, config: { ...mockEventDefinition.config, query: 'http_response_code:400' } } };

    const ConnectStoreWrapper = () => (<Component {...componentProps} />);

    return ConnectStoreWrapper;
  }),
}));

jest.mock('stores/event-definitions/AvailableEventDefinitionTypesStore', () => ({
  AvailableEventDefinitionTypesStore: {
    getInitialState: () => ({
      aggregation_functions: ['avg', 'card', 'count', 'max', 'min', 'sum', 'stddev', 'sumofsquares', 'variance', 'percentage', 'percentile', 'latest'],
      field_provider_types: ['template-v1', 'lookup-v1'],
      processor_types: ['aggregation-v1', 'system-notifications-v1', 'correlation-v1', 'anomaly-v1', 'sigma-v1'],
      storage_handler_types: ['persist-to-streams-v1'],
    }),
  },
}));

const mockEventNotifications = [{
  id: 'mock-notification-id',
  title: 'mock-notification-title',
  description: 'mock-notification-description',
  config: {
    body_template: '',
    email_recipients: ['test-user@graylog.com'],
    html_body_template: '',
    lookup_recipient_emails: false,
    lookup_reply_to_email: false,
    lookup_sender_email: false,
    recipients_lut_key: null,
    recipients_lut_name: null,
    reply_to: '',
    reply_to_lut_key: null,
    reply_to_lut_name: null,
    sender: 'info-test@graylog.com',
    sender_lut_key: null,
    sender_lut_name: null,
    subject: 'Mock test email notification subject',
    time_zone: 'UTC',
    type: 'email-notification-v1',
    user_recipients: [],
  },
}];

jest.mock('stores/event-notifications/EventNotificationsStore', () => ({
  EventNotificationsActions: { listAll: jest.fn() },
  EventNotificationsStore: {
    listen: () => jest.fn(),
    getInitialState: () => ({
      all: mockEventNotifications,
      allLegacyTypes: [],
      notifications: mockEventNotifications,
      query: '',
      pagination: {
        count: 1,
        page: 1,
        pageSize: 10,
        total: 1,
        grandTotal: 1,
      },
    }),
  },
}));

jest.mock('stores/configurations/ConfigurationsStore', () => ({
  ConfigurationsActions: {
    listEventsClusterConfig: jest.fn(() => Promise.resolve({
      events_catchup_window: 3600000,
      events_notification_default_backlog: 50,
      events_notification_retry_period: 300000,
      events_notification_tcp_keepalive: false,
      events_search_timeout: 60000,
    })),
  },
}));

jest.mock('stores/users/CurrentUserStore', () => ({
  __esModule: true,
  CurrentUserStore: {
    listen: () => jest.fn(),
    getInitialState: () => ({ currentUser: mockDefaultUser.toJSON() }),
  },
}));

jest.mock('../event-definition-types/withStreams', () => ({
  __esModule: true,
  default: (Component: React.FC) => (props: any) => (<Component {...props} streams={[{ id: 'stream-id', title: 'stream-title' }]} />),
}));

jest.mock('logic/telemetry/withTelemetry', () => ({
  __esModule: true,
  default: (Component: React.FC) => (props: any) => (
    <Component {...props}
               streams={[{ id: 'stream-id', title: 'stream-title' }]}
               sendTelemetry={() => {}}
               onChange={() => {}}
               currentUser={mockDefaultUser}
               validation={{ errors: {} }} />
  ),
}));

describe('EventDefinitionFormContainer', () => {
  it('should render Event Details form enabled', async () => {
    render(<EventDefinitionFormContainer action="edit" eventDefinition={mockAggregationEventDefinition} />);

    const titles = await screen.findAllByText(/event details/i);
    titles.forEach((title) => expect(title).toBeInTheDocument());

    expect(screen.getByRole('textbox', { name: /title/i })).toBeEnabled();
    expect(screen.getByRole('textbox', { name: /description/i })).toBeEnabled();
  });

  it('should render Event Details form disabled', async () => {
    mockUseScopePermissions.mockImplementation(() => exampleEntityScopeImmutable);
    render(<EventDefinitionFormContainer action="edit" eventDefinition={mockAggregationEventDefinition} />);

    const titles = await screen.findAllByText(/event details/i);
    titles.forEach((title) => expect(title).toBeInTheDocument());

    expect(screen.getByRole('textbox', { name: /title/i })).toHaveAttribute('readonly');
    expect(screen.getByRole('textbox', { name: /description/i })).toHaveAttribute('readonly');
  });

  it('should render Filters & Aggregation form enabled', async () => {
    mockUseScopePermissions.mockImplementation(() => exampleEntityScopeMutable);
    render(<EventDefinitionFormContainer action="edit" eventDefinition={mockAggregationEventDefinition} />);

    const tab = await screen.findByRole('button', { name: /filter & aggregation/i });
    userEvent.click(tab);

    expect(screen.getByRole('textbox', { name: /search query/i })).toBeEnabled();
  });
});
