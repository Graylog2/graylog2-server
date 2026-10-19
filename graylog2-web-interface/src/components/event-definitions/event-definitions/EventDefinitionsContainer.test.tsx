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
import { render, screen, within } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';
import { OrderedMap } from 'immutable';
import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';

import { asMock } from 'helpers/mocking';
import useFetchEntities from 'components/common/PaginatedEntityTable/useFetchEntities';
import useUserLayoutPreferences from 'components/common/EntityDataTable/hooks/useUserLayoutPreferences';
import useScopePermissions from 'hooks/useScopePermissions';
import useNotificationsByIds from 'components/event-notifications/hooks/useNotificationsByIds';
import { fetchEventDefinitions } from 'components/event-definitions/hooks/useEventDefinitions';
import { layoutPreferences } from 'fixtures/entityListLayoutPreferences';
import { simpleEventDefinition } from 'fixtures/eventDefinition';
import type { EventDefinition } from 'components/event-definitions/event-definitions-types';
import type { SearchParams } from 'stores/PaginationTypes';

import EventDefinitionsContainer from './EventDefinitionsContainer';

jest.mock('components/common/PaginatedEntityTable/useFetchEntities');
jest.mock('components/common/EntityDataTable/hooks/useUserLayoutPreferences');
jest.mock('hooks/useScopePermissions');
jest.mock('components/event-notifications/hooks/useNotificationsByIds');
jest.mock('components/event-definitions/hooks/useEventDefinitions', () => ({
  ...jest.requireActual('components/event-definitions/hooks/useEventDefinitions'),
  fetchEventDefinitions: jest.fn(),
}));

const attributes = [{ id: 'title', title: 'Title', sortable: true }];

const paginatedEventDefinitions = (eventDefinition: EventDefinition = simpleEventDefinition) => ({
  data: {
    pagination: { total: 1, page: 1, perPage: 5, count: 1 },
    list: [eventDefinition],
    attributes,
  },
  refetch: () => {},
  isInitialLoading: false,
});

const buildNotification = (id: string) => ({
  type: 'http-notification-v1',
  notification_id: id,
  notification_parameters: { type: 'http-notification-v1' },
});

describe('EventDefinitionsContainer', () => {
  beforeEach(() => {
    asMock(useUserLayoutPreferences).mockReturnValue({
      data: {
        ...layoutPreferences,
        attributes: {
          title: { status: 'show' },
          notifications: { status: 'show' },
        },
      },
      isInitialLoading: false,
      refetch: () => {},
    });

    asMock(useScopePermissions).mockReturnValue({
      loadingScopePermissions: false,
      scopePermissions: { is_mutable: true, is_deletable: true },
      checkPermissions: () => true,
    });

    asMock(useNotificationsByIds).mockReturnValue({
      data: [],
      notPermittedIds: [],
      isLoading: false,
    });
  });

  it('shows the count of notifications attached to an event definition in its row', async () => {
    const definitionWithNotifications: EventDefinition = {
      ...simpleEventDefinition,
      notifications: [
        buildNotification('n1'),
        buildNotification('n2'),
        buildNotification('n3'),
        buildNotification('n4'),
        buildNotification('n5'),
      ],
    };
    asMock(useFetchEntities).mockReturnValue(paginatedEventDefinitions(definitionWithNotifications));

    render(<EventDefinitionsContainer />);

    const row = await screen.findByTestId(`table-row-${definitionWithNotifications.id}`);

    expect(await within(row).findByText('5')).toBeInTheDocument();
  });

  it('renders nothing in the notifications cell for an event definition with no notifications', async () => {
    asMock(useFetchEntities).mockReturnValue(paginatedEventDefinitions());

    render(<EventDefinitionsContainer />);

    const row = await screen.findByTestId(`table-row-${simpleEventDefinition.id}`);

    expect(within(row).queryByText('0')).not.toBeInTheDocument();
  });

  it('lists each attached notification as a link to its detail page when the count badge is expanded', async () => {
    asMock(useNotificationsByIds).mockReturnValue({
      data: [
        { id: 'n1', title: 'PagerDuty Alert' },
        { id: 'n2', title: 'Slack Channel' },
      ],
      notPermittedIds: [],
      isLoading: false,
    });

    const definition: EventDefinition = {
      ...simpleEventDefinition,
      notifications: [buildNotification('n1'), buildNotification('n2')],
    };
    asMock(useFetchEntities).mockReturnValue(paginatedEventDefinitions(definition));

    render(<EventDefinitionsContainer />);

    const row = await screen.findByTestId(`table-row-${definition.id}`);
    await userEvent.click(within(row).getByTitle(/show notifications/i));

    const pagerDutyLink = await screen.findByRole('link', { name: 'PagerDuty Alert' });
    expect(pagerDutyLink).toHaveAttribute('href', '/alerts/notifications/n1');

    const slackLink = await screen.findByRole('link', { name: 'Slack Channel' });
    expect(slackLink).toHaveAttribute('href', '/alerts/notifications/n2');
  });

  it('shows a loading indicator instead of notification ids while resolving notifications', async () => {
    asMock(useNotificationsByIds).mockReturnValue({
      data: undefined,
      notPermittedIds: [],
      isLoading: true,
    });

    const definition: EventDefinition = {
      ...simpleEventDefinition,
      notifications: [buildNotification('n1')],
    };
    asMock(useFetchEntities).mockReturnValue(paginatedEventDefinitions(definition));

    render(<EventDefinitionsContainer />);

    const row = await screen.findByTestId(`table-row-${definition.id}`);
    await userEvent.click(within(row).getByTitle(/show notifications/i));

    expect(await screen.findByText('Loading notifications...')).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'n1' })).not.toBeInTheDocument();
  });

  it('renders an entry with the notification id when a referenced notification no longer exists', async () => {
    asMock(useNotificationsByIds).mockReturnValue({
      data: [{ id: 'n1', title: 'PagerDuty Alert' }],
      notPermittedIds: [],
      isLoading: false,
    });

    const definition: EventDefinition = {
      ...simpleEventDefinition,
      notifications: [buildNotification('n1'), buildNotification('missing-id')],
    };
    asMock(useFetchEntities).mockReturnValue(paginatedEventDefinitions(definition));

    render(<EventDefinitionsContainer />);

    const row = await screen.findByTestId(`table-row-${definition.id}`);
    await userEvent.click(within(row).getByTitle(/show notifications/i));

    const fallbackLink = await screen.findByRole('link', { name: 'missing-id' });
    expect(fallbackLink).toHaveAttribute('href', '/alerts/notifications/missing-id');
  });

  it('warns about notifications the user is not permitted to view', async () => {
    asMock(useNotificationsByIds).mockReturnValue({
      data: [{ id: 'n1', title: 'PagerDuty Alert' }],
      notPermittedIds: ['secret-id'],
      isLoading: false,
    });

    const definition: EventDefinition = {
      ...simpleEventDefinition,
      notifications: [buildNotification('n1'), buildNotification('secret-id')],
    };
    asMock(useFetchEntities).mockReturnValue(paginatedEventDefinitions(definition));

    render(<EventDefinitionsContainer />);

    const row = await screen.findByTestId(`table-row-${definition.id}`);
    await userEvent.click(within(row).getByTitle(/show notifications/i));

    const warning = await screen.findByRole('alert');
    expect(warning).toHaveTextContent(/Missing Notifications Permissions for/i);
    expect(warning).toHaveTextContent('secret-id');
    expect(screen.queryByRole('link', { name: 'secret-id' })).not.toBeInTheDocument();
  });

  it('shows the priority name instead of its numeric value', async () => {
    asMock(useUserLayoutPreferences).mockReturnValue({
      data: {
        ...layoutPreferences,
        attributes: {
          title: { status: 'show' },
          priority: { status: 'show' },
        },
      },
      isInitialLoading: false,
      refetch: () => {},
    });
    const definition: EventDefinition = { ...simpleEventDefinition, priority: 3 };
    const fetchedEntities = paginatedEventDefinitions(definition);
    asMock(useFetchEntities).mockReturnValue({
      ...fetchedEntities,
      data: {
        ...fetchedEntities.data,
        attributes: [...attributes, { id: 'priority', title: 'Priority', sortable: true }],
      },
    });

    render(<EventDefinitionsContainer />);

    const row = await screen.findByTestId(`table-row-${definition.id}`);

    await within(row).findByText('High');
  });

  it('lists notifications among the default visible columns', async () => {
    asMock(useUserLayoutPreferences).mockReturnValue({
      data: {
        ...layoutPreferences,
        attributes: undefined,
      },
      isInitialLoading: false,
      refetch: () => {},
    });
    asMock(useFetchEntities).mockReturnValue(paginatedEventDefinitions());

    render(<EventDefinitionsContainer />);

    expect(await screen.findByRole('columnheader', { name: /Notifications/ })).toBeInTheDocument();
  });

  it('adds the active tactics/techniques plugin attribute to the fetched attributes', async () => {
    const tacticsTechniquesAttribute = { id: 'tactics_techniques', title: 'Tactics/Techniques', filterable: true };
    const manifest = new PluginManifest(
      {},
      {
        'eventDefinitions.components.tacticsTechniquesColumn': [
          { attribute: tacticsTechniquesAttribute, component: () => null, useCondition: () => true },
        ],
      },
    );
    asMock(useFetchEntities).mockReturnValue(paginatedEventDefinitions());
    asMock(fetchEventDefinitions).mockResolvedValue({ list: [], pagination: { total: 0 }, attributes });

    PluginStore.register(manifest);

    try {
      render(<EventDefinitionsContainer />);
      await screen.findByTestId(`table-row-${simpleEventDefinition.id}`);

      const { fetchEntities } = asMock(useFetchEntities).mock.calls.at(-1)[0];
      const result = await fetchEntities({ filters: OrderedMap() } as SearchParams);

      expect(result.attributes).toEqual([...attributes, tacticsTechniquesAttribute]);
    } finally {
      PluginStore.unregister(manifest);
    }
  });
});
