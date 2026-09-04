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
import { render, screen } from 'wrappedTestingLibrary';
import { renderHook } from 'wrappedTestingLibrary/hooks';
import { PluginStore } from 'graylog-web-plugin/plugin';
import { defaultUser } from 'defaultMockValues';

import { alice } from 'fixtures/users';
import { asMock } from 'helpers/mocking';
import usePluginEntities from 'hooks/usePluginEntities';
import usePluggableLicenseCheck from 'hooks/usePluggableLicenseCheck';

import {
  useEventProcedureSummaryComponents,
  renderCondition,
  renderField,
  renderFields,
  renderNotifications,
  useTechniquesSummary,
} from './useEventDefinitionSummaryRenders';

jest.mock('graylog-web-plugin/plugin', () => ({
  PluginStore: { exports: jest.fn(() => []) },
}));

jest.mock('hooks/usePluginEntities');
jest.mock('hooks/usePluggableLicenseCheck');

jest.mock('components/common/MarkdownEditor', () => ({
  MarkdownPreview: ({ value }: { value: string }) => <div data-testid="markdown-preview">{value}</div>,
}));

// Alert renders a block element; the source wraps it in <p> which causes a DOM nesting warning.
// Use a span to stay inline-safe inside that <p>.
jest.mock('components/bootstrap', () => ({
  Alert: ({ children }: { children: React.ReactNode }) => <span role="alert">{children}</span>,
}));

const licenseCheck = (valid: boolean): ReturnType<typeof usePluggableLicenseCheck> => ({
  data: { valid, expired: false, violated: false },
  isInitialLoading: false,
  refetch: () => {},
});

describe('useEventProcedureSummaryComponents', () => {
  beforeEach(() => {
    asMock(usePluggableLicenseCheck).mockReturnValue(licenseCheck(false));
    asMock(usePluginEntities).mockReturnValue([]);
  });

  it('returns Remediation Steps label and "No remediation steps given" message when no security license and no remediationSteps', () => {
    const { result } = renderHook(() => useEventProcedureSummaryComponents({}));

    expect(result.current.label).toBe('Remediation Steps');
    render(<>{result.current.Component}</>);
    expect(screen.getByTestId('markdown-preview')).toHaveTextContent('No remediation steps given');
  });

  it('returns Remediation Steps label and MarkdownPreview when no security license but remediationSteps given', () => {
    const { result } = renderHook(() =>
      useEventProcedureSummaryComponents({ remediationSteps: 'Fix the issue by doing X.' }),
    );

    expect(result.current.label).toBe('Remediation Steps');
    render(<>{result.current.Component}</>);
    expect(screen.getByTestId('markdown-preview')).toHaveTextContent('Fix the issue by doing X.');
  });

  it('returns Event Procedure Summary label and "no procedures" message when valid license but no eventProcedureId', () => {
    asMock(usePluggableLicenseCheck).mockReturnValue(licenseCheck(true));

    const { result } = renderHook(() => useEventProcedureSummaryComponents({}));

    expect(result.current.label).toBe('Event Procedure Summary');
    render(<>{result.current.Component}</>);
    expect(screen.getByText(/this event does not have any event procedures/i)).toBeInTheDocument();
  });

  it('returns Event Procedure Summary label and pluggable component when valid license and eventProcedureId given', () => {
    asMock(usePluggableLicenseCheck).mockReturnValue(licenseCheck(true));
    asMock(usePluginEntities).mockImplementation((key) => {
      if (key === 'views.components.eventProcedureSummary') {
        return [
          {
            key: 'test-key',
            component: ({ eventProcedureId }: { eventProcedureId: string }) => <div>Procedure: {eventProcedureId}</div>,
          },
        ];
      }

      return [];
    });

    const { result } = renderHook(() => useEventProcedureSummaryComponents({ eventProcedureId: 'proc-123' }));

    expect(result.current.label).toBe('Event Procedure Summary');
    render(<>{result.current.Component}</>);
    expect(screen.getByText('Procedure: proc-123')).toBeInTheDocument();
  });
});

describe('renderCondition', () => {
  const baseEventDefinition = {
    config: { type: 'aggregation-v1' },
    _scope: 'DEFAULT',
  };

  beforeEach(() => {
    asMock(PluginStore.exports).mockReturnValue([]);
  });

  it('renders config.type in heading when plugin is not found', () => {
    render(<>{renderCondition(baseEventDefinition as any, 'def-id', defaultUser)}</>);

    expect(screen.getByRole('heading', { name: /aggregation-v1/i })).toBeInTheDocument();
  });

  it('renders "does not provide a summary" when plugin has no summaryComponent', () => {
    asMock(PluginStore.exports).mockReturnValue([
      { type: 'aggregation-v1', displayName: 'Aggregation', summaryComponent: undefined },
    ]);

    render(<>{renderCondition(baseEventDefinition as any, 'def-id', defaultUser)}</>);

    expect(screen.getByText(/does not provide a summary/i)).toBeInTheDocument();
  });

  it('renders the plugin summaryComponent when available', () => {
    const SummaryComponent = () => <div>Aggregation Summary</div>;
    asMock(PluginStore.exports).mockReturnValue([
      { type: 'aggregation-v1', displayName: 'Aggregation', summaryComponent: SummaryComponent },
    ]);

    render(<>{renderCondition(baseEventDefinition as any, 'def-id', defaultUser)}</>);

    expect(screen.getByText('Aggregation Summary')).toBeInTheDocument();
  });

  it('renders plugin displayName as heading when plugin is found', () => {
    const SummaryComponent = () => <div>Summary</div>;
    asMock(PluginStore.exports).mockReturnValue([
      { type: 'aggregation-v1', displayName: 'Aggregation', summaryComponent: SummaryComponent },
    ]);

    render(<>{renderCondition(baseEventDefinition as any, 'def-id', defaultUser)}</>);

    expect(screen.getByRole('heading', { name: 'Aggregation' })).toBeInTheDocument();
  });
});

describe('renderField', () => {
  beforeEach(() => {
    asMock(PluginStore.exports).mockReturnValue([]);
  });

  it('renders "No field value provider configured" when providers list is empty', () => {
    const config = { data_type: 'string', providers: [] };
    render(<>{renderField('my_field', config as any, [], defaultUser)}</>);

    expect(screen.getByText(/no field value provider configured/i)).toBeInTheDocument();
  });

  it('renders "does not provide a summary" when provider plugin has no summaryComponent', () => {
    asMock(PluginStore.exports).mockReturnValue([
      { type: 'template-v1', displayName: 'Template', summaryComponent: undefined },
    ]);

    const config = {
      data_type: 'string',
      providers: [{ type: 'template-v1', template: '', require_values: false, table_name: '', key_field: '' }],
    };
    render(<>{renderField('my_field', config as any, [], defaultUser)}</>);

    expect(screen.getByText(/does not provide a summary/i)).toBeInTheDocument();
  });

  it('renders the provider summaryComponent when available', () => {
    const ProviderSummary = () => <div>Provider Summary</div>;
    asMock(PluginStore.exports).mockReturnValue([
      { type: 'template-v1', displayName: 'Template', summaryComponent: ProviderSummary },
    ]);

    const config = {
      data_type: 'string',
      providers: [{ type: 'template-v1', template: '', require_values: false, table_name: '', key_field: '' }],
    };
    render(<>{renderField('my_field', config as any, [], defaultUser)}</>);

    expect(screen.getByText('Provider Summary')).toBeInTheDocument();
  });
});

describe('renderFields', () => {
  beforeEach(() => {
    asMock(PluginStore.exports).mockReturnValue([]);
  });

  it('renders "No Fields configured" when field_spec is empty', () => {
    render(<>{renderFields({}, [], defaultUser)}</>);

    expect(screen.getByText(/no fields configured for events based on this definition/i)).toBeInTheDocument();
  });

  it('renders the Fields heading', () => {
    render(<>{renderFields({}, [], defaultUser)}</>);

    expect(screen.getByRole('heading', { name: 'Fields' })).toBeInTheDocument();
  });

  it('renders keys and field entries when fields are present', () => {
    const fields = { my_field: { data_type: 'string', providers: [] } };
    render(<>{renderFields(fields as any, ['key1'], defaultUser)}</>);

    expect(screen.getByText(/key1/i)).toBeInTheDocument();
    expect(screen.getByText(/no field value provider configured/i)).toBeInTheDocument();
  });

  it('shows "No Keys configured" when key_spec is empty', () => {
    const fields = { my_field: { data_type: 'string', providers: [] } };
    render(<>{renderFields(fields as any, [], defaultUser)}</>);

    expect(screen.getByText(/no keys configured for events based on this definition/i)).toBeInTheDocument();
  });
});

describe('renderNotifications', () => {
  const notificationSettings = { grace_period_ms: 0, backlog_size: null };

  const definitionNotification = {
    notification_id: 'notif-1',
    type: 'email-notification-v1',
    notification_parameters: { type: 'test' },
  };

  const notification = {
    id: 'notif-1',
    title: 'Test Notification',
    config: { type: 'email-notification-v1' },
  };

  beforeEach(() => {
    asMock(PluginStore.exports).mockReturnValue([]);
  });

  it('renders "not configured to trigger any Notifications" when definitionNotifications is empty', () => {
    render(<>{renderNotifications([], notificationSettings as any, [], defaultUser)}</>);

    expect(screen.getByText(/not configured to trigger any notifications/i)).toBeInTheDocument();
  });

  it('renders "Could not find information" when notification is not in the notifications list', () => {
    render(<>{renderNotifications([definitionNotification], notificationSettings as any, [], defaultUser)}</>);

    expect(screen.getByText(/could not find information for notification/i)).toBeInTheDocument();
  });

  it('renders "does not provide a summary" when notification plugin has no summaryComponent', () => {
    asMock(PluginStore.exports).mockReturnValue([
      { type: 'email-notification-v1', displayName: 'Email', summaryComponent: undefined },
    ]);

    render(
      <>
        {renderNotifications([definitionNotification], notificationSettings as any, [notification as any], defaultUser)}
      </>,
    );

    expect(screen.getByText(/does not provide a summary/i)).toBeInTheDocument();
  });

  it('renders the notification summaryComponent when available', () => {
    const NotificationSummary = () => <div>Notification Summary</div>;
    asMock(PluginStore.exports).mockReturnValue([
      { type: 'email-notification-v1', displayName: 'Email', summaryComponent: NotificationSummary },
    ]);

    render(
      <>
        {renderNotifications([definitionNotification], notificationSettings as any, [notification as any], defaultUser)}
      </>,
    );

    expect(screen.getByText('Notification Summary')).toBeInTheDocument();
  });

  it('renders a warning for notifications with missing permissions', () => {
    // alice has reader permissions — no eventnotifications:read privilege
    render(<>{renderNotifications([definitionNotification], notificationSettings as any, [], alice)}</>);

    expect(screen.getByRole('alert')).toHaveTextContent(/missing notifications permissions for/i);
    expect(screen.getByRole('alert')).toHaveTextContent('notif-1');
  });

  it('renders grace period settings when notifications are present', () => {
    const settings = { grace_period_ms: 300000, backlog_size: 10 };
    asMock(PluginStore.exports).mockReturnValue([
      { type: 'email-notification-v1', displayName: 'Email', summaryComponent: undefined },
    ]);

    render(<>{renderNotifications([definitionNotification], settings as any, [notification as any], defaultUser)}</>);

    expect(screen.getByText(/grace period is set to/i)).toBeInTheDocument();
    expect(screen.getByText(/notifications will include 10 messages/i)).toBeInTheDocument();
  });
});

describe('useTechniquesSummary', () => {
  const eventDefinition = { id: 'def-1', config: { type: 'aggregation-v1' } };

  beforeEach(() => {
    asMock(usePluginEntities).mockReturnValue([]);
  });

  it('returns null when no tactics/techniques plugin is registered', () => {
    const { result } = renderHook(() => useTechniquesSummary(eventDefinition as any));

    expect(result.current).toBeNull();
  });

  it('returns null when plugin useCondition returns false', () => {
    asMock(usePluginEntities).mockReturnValue([
      {
        component: () => <div>Techniques</div>,
        useCondition: () => false,
      },
    ]);

    const { result } = renderHook(() => useTechniquesSummary(eventDefinition as any));

    expect(result.current).toBeNull();
  });

  it('renders the plugin component when registered and enabled', () => {
    const TechniquesComponent = ({ eventDefinition: ed }: { eventDefinition: any }) => (
      <div>Techniques for {ed.id}</div>
    );
    asMock(usePluginEntities).mockReturnValue([{ component: TechniquesComponent, useCondition: undefined }]);

    const { result } = renderHook(() => useTechniquesSummary(eventDefinition as any));

    render(<>{result.current}</>);
    expect(screen.getByText('Techniques for def-1')).toBeInTheDocument();
  });
});
