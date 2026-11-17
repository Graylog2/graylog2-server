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
import userEvent from '@testing-library/user-event';
import { render, screen } from 'wrappedTestingLibrary';
import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';

import asMock from 'helpers/mocking/AsMock';
import selectEvent from 'helpers/selectEvent';
import usePermissions from 'hooks/usePermissions';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import type { EventNotification } from 'components/event-notifications/hooks/useEventNotifications';

import EventNotificationForm from './EventNotificationForm';

jest.mock('hooks/usePermissions');
jest.mock('logic/telemetry/useSendTelemetry');
jest.mock('components/permissions/EntityCreateShareFormGroup', () => () => null);

const NoopForm = () => null;
const notificationType = (type: string, displayName: string, extra = {}) => ({
  type,
  displayName,
  formComponent: NoopForm,
  summaryComponent: NoopForm,
  detailsComponent: NoopForm,
  defaultConfig: {},
  ...extra,
});

const plugin = new PluginManifest(
  {},
  {
    eventNotificationTypes: [
      notificationType('open-type', 'Open type'),
      notificationType('gated-type', 'Gated type', { useCondition: () => false }),
      notificationType('restricted-type', 'Restricted type', { requiredPermissions: ['restricted:edit'] }),
    ],
  },
);

const notification = (type: string) =>
  ({ id: 'notification-id', title: 'Notification', description: '', config: { type } }) as unknown as EventNotification;

const renderForm = (action: 'create' | 'edit', type: string, onSubmit = jest.fn()) =>
  render(
    <EventNotificationForm
      action={action}
      embedded={false}
      notification={notification(type)}
      validation={{ errors: {} }}
      testResult={{}}
      onChange={jest.fn()}
      onCancel={jest.fn()}
      onSubmit={onSubmit}
      onTest={jest.fn()}
    />,
  );

// The type select is the only combobox in the form.
const openTypeMenu = async () => selectEvent.openMenu(await screen.findByRole('combobox'));

describe('EventNotificationForm', () => {
  const sendTelemetry = jest.fn();

  beforeAll(() => PluginStore.register(plugin));
  afterAll(() => PluginStore.unregister(plugin));

  beforeEach(() => {
    asMock(usePermissions).mockReturnValue({
      isPermitted: (permissions) => !permissions.includes('restricted:edit'),
      isAnyPermitted: () => true,
      isAdmin: false,
    });
    asMock(useSendTelemetry).mockReturnValue(sendTelemetry);
  });

  it('hides types whose condition or permissions are not met when creating', async () => {
    renderForm('create', 'open-type');

    await openTypeMenu();

    await screen.findByRole('option', { name: /open type/i });
    expect(screen.queryByRole('option', { name: /gated type/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('option', { name: /restricted type/i })).not.toBeInTheDocument();
  });

  it('keeps the type of the notification being edited when its condition is not met', async () => {
    renderForm('edit', 'gated-type');

    await openTypeMenu();

    await screen.findByRole('option', { name: /gated type/i });
    expect(screen.queryByRole('option', { name: /restricted type/i })).not.toBeInTheDocument();
  });

  it('sends the notification type with the submit telemetry', async () => {
    const onSubmit = jest.fn();
    renderForm('edit', 'open-type', onSubmit);

    await userEvent.click(await screen.findByRole('button', { name: /update notification/i }));

    expect(onSubmit).toHaveBeenCalled();
    expect(sendTelemetry).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({ notification_type: 'open-type' }),
    );
  });
});
