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
import { mount } from 'wrappedEnzyme';
import { CombinedProviderMock, StoreMock } from 'helpers/mocking';

jest.mock('stores/connect', () => (x) => x);

describe('NotificationBadge', () => {
  let notifications;
  let NotificationBadge;

  beforeEach(() => {
    notifications = {
      NotificationsActions: { list: jest.fn() },
      NotificationsStore: StoreMock('listen'),
    };

    const combinedProviderMock = new CombinedProviderMock({
      Notifications: notifications,
    });

    jest.doMock('injection/CombinedProvider', () => combinedProviderMock);

    // eslint-disable-next-line global-require
    NotificationBadge = require('./NotificationBadge').default;
  });

  it('triggers update of notifications', () => {
    const { NotificationsActions } = notifications;

    mount(<NotificationBadge />);

    expect(NotificationsActions.list).toHaveBeenCalled();
  });

  it('renders nothing when there are no notifications', () => {
    const wrapper = mount(<NotificationBadge total={0} />);

    expect(wrapper).toMatchSnapshot();
  });

  it('renders count when there are notifications', () => {
    const wrapper = mount(<NotificationBadge total={42} />);

    expect(wrapper.find('#notification-badge')).toExist();
  });

  it('updates notification count when triggered by store', () => {
    const wrapper = mount(<NotificationBadge total={42} />);

    expect(wrapper.find('span#notification-badge')).toHaveText('42');

    wrapper.setProps({ total: 23 });

    expect(wrapper.find('span#notification-badge')).toHaveText('23');
  });

  it('does not rerender when number of notifications does not change', () => {
    NotificationBadge.prototype.render = jest.fn(NotificationBadge.prototype.render);
    const wrapper = mount(<NotificationBadge total={42} />);

    expect(wrapper.find('span#notification-badge')).toHaveText('42');

    wrapper.setProps({ total: 42 });

    expect(wrapper.find('span#notification-badge')).toHaveText('42');
    expect(wrapper.find('NotificationBadge').get(0).type.prototype.render).toHaveBeenCalledTimes(1);
  });
});
