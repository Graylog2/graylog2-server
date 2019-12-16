import React from 'react';
import Immutable from 'immutable';
import { mount } from 'wrappedEnzyme';
import 'helpers/mocking/react-dom_mock';

import PermissionSelector from 'components/users/PermissionSelector';

describe('<PermissionSelector />', () => {
  const streams = Immutable.List([
    { id: '01', description: 'stream1' },
    { id: '02', description: 'stream2' },
  ]);
  const dashboards = Immutable.List([
    { id: '01', description: 'dashboard1' },
    { id: '02', description: 'dashboard2' },
  ]);

  it('should render with empty permissions', () => {
    const wrapper = mount(<PermissionSelector streams={streams}
                                              permissions={Immutable.Set([])}
                                              dashboards={dashboards}
                                              onChange={() => {}} />);
    expect(wrapper).toMatchSnapshot();
  });

  it('should render with set permissions', () => {
    const permissions = Immutable.Set([
      'streams:read:01',
      'streams:edit:02',
      'dashboards:read:02',
    ]);
    const wrapper = mount(<PermissionSelector streams={streams}
                                              permissions={permissions}
                                              dashboards={dashboards}
                                              onChange={() => {}} />);
    expect(wrapper).toMatchSnapshot();
  });

  it('should allow reading when clicked on "Allow reading"', () => {
    const onButtonClick = jest.fn((added) => {
      expect(added).toEqual(Immutable.Set(['streams:read:01']));
    });
    const wrapper = mount(<PermissionSelector streams={streams}
                                              permissions={Immutable.Set([])}
                                              dashboards={dashboards}
                                              onChange={onButtonClick} />);
    wrapper.find('button[children="Allow reading"]').at(0).simulate('click');
    expect(onButtonClick.mock.calls.length).toBe(1);
  });

  it('should allow reading and editing when clicked on "Allow editing"', () => {
    const onButtonClick = jest.fn((added) => {
      expect(added).toEqual(Immutable.Set(['streams:read:01', 'streams:edit:01']));
    });
    const wrapper = mount(<PermissionSelector streams={streams}
                                              permissions={Immutable.Set([])}
                                              dashboards={dashboards}
                                              onChange={onButtonClick} />);
    wrapper.find('button[children="Allow editing"]').at(0).simulate('click');
    expect(onButtonClick.mock.calls.length).toBe(1);
  });

  it('should not allow reading when "Allow reading" was unselected', () => {
    const onButtonClick = jest.fn((_, deleted) => {
      expect(deleted).toEqual(Immutable.Set(['streams:read:01', 'streams:edit:01']));
    });
    const wrapper = mount(<PermissionSelector streams={streams}
                                              permissions={Immutable.Set(['streams:read:01'])}
                                              dashboards={dashboards}
                                              onChange={onButtonClick} />);
    wrapper.find('button[children="Allow reading"]').at(0).simulate('click');
    expect(onButtonClick.mock.calls.length).toBe(1);
  });

  it('should bulk set "Allow reading" for selected streams', () => {
    const onButtonClick = jest.fn((added, deleted) => {
      expect(added).toEqual(Immutable.Set(['streams:read:01', 'streams:read:02']));
      expect(deleted).toEqual(Immutable.Set([]));
    });
    const wrapper = mount(<PermissionSelector streams={streams}
                                              permissions={Immutable.Set(['streams:read:01'])}
                                              dashboards={dashboards}
                                              onChange={onButtonClick} />);
    wrapper.find('input[label="Select all"]').at(0).simulate('change', { target: { checked: true } });
    expect(wrapper.find('button').at(2).text()).toEqual('Set read permissions');
    wrapper.find('button').at(2).simulate('click');
    expect(onButtonClick.mock.calls.length).toBe(1);
  });

  it('should bulk set "Allow editing" and "Allow reading" for selected streams', () => {
    const onButtonClick = jest.fn((added, deleted) => {
      expect(added).toEqual(Immutable.Set(['streams:read:01', 'streams:edit:01', 'streams:read:02', 'streams:edit:02']));
      expect(deleted).toEqual(Immutable.Set([]));
    });
    const wrapper = mount(<PermissionSelector streams={streams}
                                              permissions={Immutable.Set(['streams:read:01', 'stream:edit:01'])}
                                              dashboards={dashboards}
                                              onChange={onButtonClick} />);
    wrapper.find('input[label="Select all"]').at(0).simulate('change', { target: { checked: true } });
    expect(wrapper.find('button').at(3).text()).toEqual('Set edit permissions');
    wrapper.find('button').at(3).simulate('click');
    expect(onButtonClick.mock.calls.length).toBe(1);
  });

  it('should bulk clear "Allow reading" for selected streams', () => {
    const onButtonClick = jest.fn((added, deleted) => {
      expect(added).toEqual(Immutable.Set([]));
      expect(deleted).toEqual(Immutable.Set(['streams:read:01', 'streams:edit:01', 'streams:read:02', 'streams:edit:02']));
    });
    const wrapper = mount(<PermissionSelector streams={streams}
                                              permissions={Immutable.Set(['streams:read:01', 'streams:read:02'])}
                                              dashboards={dashboards}
                                              onChange={onButtonClick} />);
    wrapper.find('input[label="Select all"]').at(0).simulate('change', { target: { checked: true } });
    expect(wrapper.find('button').at(2).text()).toEqual('Clear read permissions');
    wrapper.find('button').at(2).simulate('click');
    expect(onButtonClick.mock.calls.length).toBe(1);
  });

  it('should bulk set "Allow editing" for selected streams', () => {
    const onButtonClick = jest.fn((added, deleted) => {
      expect(added).toEqual(Immutable.Set(['streams:read:01', 'streams:edit:01',
        'streams:read:02', 'streams:edit:02']));
      expect(deleted).toEqual(Immutable.Set([]));
    });
    const wrapper = mount(<PermissionSelector streams={streams}
                                              permissions={Immutable.Set(['streams:read:01', 'streams:edit:01'])}
                                              dashboards={dashboards}
                                              onChange={onButtonClick} />);
    wrapper.find('input[label="Select all"]').at(0).simulate('change', { target: { checked: true } });
    expect(wrapper.find('button').at(3).text()).toEqual('Set edit permissions');
    wrapper.find('button').at(3).simulate('click');
    expect(onButtonClick.mock.calls.length).toBe(1);
  });
});
