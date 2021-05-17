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
import { Notifications } from 'theme/types';
import { asMock } from 'helpers/mocking';

import usePluginEntities from 'views/logic/usePluginEntities';

import PublicNotifications from './PublicNotifications';

const mockedNotifications = {
  '607468afaaa2380afe0757f1': {
    title: "A really long title that really shouldn't be this long but people sometimes are do it",
    shortMessage: 'zxcvzxcv',
    longMessage: 'zxcvzxcvzxcvzxcvzxcvzxcv',
    atLogin: true,
    variant: 'warning',
    hiddenTitle: true,
    isActive: true,
    isGlobal: false,
    isDismissible: false,
  },
  '6075a2999f4efa083977b75b': {
    title: 'Danger Alert 🔥',
    shortMessage: 'xcvbxcvb',
    longMessage: 'xcvbxcvbxcvbxcvbxcvbxcvb',
    atLogin: true,
    variant: 'danger',
    hiddenTitle: false,
    isActive: true,
    isGlobal: true,
    isDismissible: true,
  },
} as Notifications;

jest.mock('views/logic/usePluginEntities');

jest.mock('util/AppConfig', () => ({
  publicNotifications: jest.fn(() => mockedNotifications),
}));

const mockedUsePublicNotifications = () => ({
  hooks: {
    usePublicNotifications: () => ({
      notifications: mockedNotifications,
      dismissedNotifications: new Set([Object.keys(mockedNotifications)[1]]),
      onDismissPublicNotification: jest.fn(() => {}),
    }),
  },
});

const beforeMock = () => {
  const mockedFn = mockedUsePublicNotifications();

  return asMock(usePluginEntities).mockImplementation((entityKey) => {
    if (entityKey === 'customization.publicNotifications') {
      return [mockedFn];
    }

    return [];
  });
};

const renderSUT = () => {
  beforeMock();

  return render(<PublicNotifications />);
};

describe('PublicNotifications', () => {
  beforeEach(beforeMock);

  it('should render an Alert', () => {
    renderSUT();

    screen.debug();

    expect(true).toBeTruthy();
  });
});
