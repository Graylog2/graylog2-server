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
import type { EventNotificationTypes } from 'components/event-notifications/types';

import notificationTypeOptions from './notificationTypeOptions';

const pluginType = (type: string, requiredPermissions?: Array<string>): EventNotificationTypes => ({
  type,
  displayName: `${type} display`,
  formComponent: () => null,
  summaryComponent: () => null,
  detailsComponent: () => null,
  defaultConfig: {},
  requiredPermissions,
});

const types = [pluginType('email'), pluginType('watchlist', ['watchlist:edit', 'watchlist:delete'])];

describe('notificationTypeOptions', () => {
  it('lists every type when the user has all permissions', () => {
    expect(notificationTypeOptions(types, () => true, undefined)).toEqual([
      { label: 'email display', value: 'email' },
      { label: 'watchlist display', value: 'watchlist' },
    ]);
  });

  it('hides a type whose required permissions are missing', () => {
    expect(notificationTypeOptions(types, () => false, undefined)).toEqual([{ label: 'email display', value: 'email' }]);
  });

  it('keeps the type of the notification being edited even without permissions', () => {
    expect(notificationTypeOptions(types, () => false, 'watchlist')).toEqual([
      { label: 'email display', value: 'email' },
      { label: 'watchlist display', value: 'watchlist' },
    ]);
  });

  it('checks the declared permissions', () => {
    const isPermitted = jest.fn(() => true);

    notificationTypeOptions(types, isPermitted, undefined);

    expect(isPermitted).toHaveBeenCalledWith(['watchlist:edit', 'watchlist:delete']);
    expect(isPermitted).toHaveBeenCalledTimes(1);
  });
});
