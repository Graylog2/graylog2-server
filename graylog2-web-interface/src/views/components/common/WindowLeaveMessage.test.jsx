// @flow strict
import * as React from 'react';
import { mount } from 'enzyme';

import { last } from 'lodash';

// $FlowFixMe: imports from core need to be fixed in flow
import AppConfig from 'util/AppConfig';
import WindowLeaveMessage from './WindowLeaveMessage';

jest.mock('react-router', () => ({ withRouter: x => x }));
jest.mock('stores/connect', () => x => x);
jest.mock('util/AppConfig');

const mockRouter = () => ({
  setRouteLeaveHook: jest.fn(),
});

const lastCall = (fn, filter = () => true) => last(fn.mock.calls.filter(filter));

describe('WindowLeaveMessage', () => {
  const { addEventListener } = window;
  const { removeEventListener } = window;

  beforeEach(() => {
    window.addEventListener = jest.fn(addEventListener);
    window.removeEventListener = jest.fn(removeEventListener);
  });

  afterEach(() => {
    window.addEventListener = addEventListener;
    window.removeEventListener = removeEventListener;
    jest.resetAllMocks();
  });

  it('registers window beforeunload handler', () => {
    const router = mockRouter();

    mount(<WindowLeaveMessage dirty={false} route={{}} router={router} />);

    expect(window.addEventListener).toHaveBeenCalledWith('beforeunload', expect.any(Function));
  });

  it('unregisters window beforeunload handler upon unmount', () => {
    const router = mockRouter();
    const unsubscribe = jest.fn();
    router.setRouteLeaveHook.mockReturnValue(unsubscribe);

    const wrapper = mount(<WindowLeaveMessage dirty={false} route={{}} router={router} />);

    wrapper.unmount();

    expect(window.removeEventListener).toHaveBeenCalledWith('beforeunload', expect.any(Function));
  });

  it('registers route leave handler', () => {
    const router = mockRouter();

    mount(<WindowLeaveMessage dirty={false} route={{ __id__: 42 }} router={router} />);

    expect(router.setRouteLeaveHook).toHaveBeenCalledTimes(1);
    expect(router.setRouteLeaveHook).toHaveBeenCalledWith({ __id__: 42 }, expect.any(Function));
  });

  it('unregisters route leave handler upon unmount', () => {
    const router = mockRouter();
    const unsubscribe = jest.fn();
    router.setRouteLeaveHook.mockReturnValue(unsubscribe);

    const wrapper = mount(<WindowLeaveMessage dirty={false} route={{ __id__: 42 }} router={router} />);

    wrapper.unmount();
    expect(unsubscribe).toHaveBeenCalled();
  });

  it('returns prompt if window is closed and view is dirty', () => {
    const router = mockRouter();

    mount(<WindowLeaveMessage dirty route={{}} router={router} />);
    const [, fn] = lastCall(window.addEventListener, ([key]) => (key === 'beforeunload'));
    const e = {};

    const result = fn(e);

    expect(result).toEqual('Are you sure you want to leave the page? Any unsaved changes will be lost.');
    expect(e).toEqual({ returnValue: 'Are you sure you want to leave the page? Any unsaved changes will be lost.' });
  });

  it('does not return prompt if window is closed and view is not dirty', () => {
    const router = mockRouter();

    mount(<WindowLeaveMessage dirty={false} route={{}} router={router} />);
    const [, fn] = lastCall(window.addEventListener, ([key]) => (key === 'beforeunload'));
    const e = {};

    const result = fn(e);

    expect(result).toBeNull();
    expect(e).toEqual({});
  });

  it('does not return prompt if window is closed and view is dirty but development mode is on', () => {
    const router = mockRouter();
    AppConfig.gl2DevMode = jest.fn(() => true);

    mount(<WindowLeaveMessage dirty route={{}} router={router} />);
    const [, fn] = lastCall(window.addEventListener, ([key]) => (key === 'beforeunload'));
    const e = {};

    const result = fn(e);

    expect(result).toBeNull();
    expect(e).toEqual({});
  });

  it('returns prompt if route is left view is dirty', () => {
    const router = mockRouter();

    mount(<WindowLeaveMessage dirty route={{}} router={router} />);
    const [, fn] = lastCall(router.setRouteLeaveHook);

    const result = fn();

    expect(result).toEqual('Are you sure you want to leave the page? Any unsaved changes will be lost.');
  });

  it('does not return prompt if route is left and view is not dirty', () => {
    const router = mockRouter();

    mount(<WindowLeaveMessage dirty={false} route={{}} router={router} />);
    const [, fn] = lastCall(router.setRouteLeaveHook);

    const result = fn();

    expect(result).toBeNull();
  });

  it('does not return prompt if route is left and view is dirty but development mode is on', () => {
    const router = mockRouter();
    AppConfig.gl2DevMode = jest.fn(() => true);

    mount(<WindowLeaveMessage dirty route={{}} router={router} />);
    const [, fn] = lastCall(router.setRouteLeaveHook);

    const result = fn();

    expect(result).toBeNull();
  });
});
