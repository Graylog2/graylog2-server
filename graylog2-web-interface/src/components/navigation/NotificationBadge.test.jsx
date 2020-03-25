import React from 'react';
import { mount } from 'wrappedEnzyme';
import { CombinedProviderMock, StoreMock } from 'helpers/mocking';

jest.mock('stores/connect', () => x => x);

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
    NotificationBadge = require('./NotificationBadge');
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

    expect(wrapper).toMatchSnapshot();
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
