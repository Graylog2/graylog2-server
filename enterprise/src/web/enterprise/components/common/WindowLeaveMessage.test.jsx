import React from 'react';
import { mount } from 'enzyme';
import { isFunction } from 'lodash';

import AppConfig from 'util/AppConfig';
import WindowLeaveMessage from './WindowLeaveMessage';

jest.mock('react-router', () => ({ withRouter: x => x }));
jest.mock('stores/connect', () => x => x);
jest.mock('util/AppConfig');

const mockRouter = () => ({
  setRouteLeaveHook: jest.fn(),
});

const lastCall = (fn) => {
  const callCount = fn.mock.calls.length;
  return fn.mock.calls[callCount - 1];
};

describe('WindowLeaveMessage', () => {
  const addEventListener = window.addEventListener;

  beforeEach(() => {
    window.addEventListener = jest.fn(addEventListener);
  });

  afterEach(() => {
    window.addEventListener = addEventListener;
    jest.resetAllMocks();
  });

  it('registers window beforeunload handler', () => {
    const router = mockRouter();

    mount(<WindowLeaveMessage dirty={false} route={{}} router={router} />);

    const [event, fn] = lastCall(window.addEventListener);
    expect(event).toEqual('beforeunload');
    expect(isFunction(fn)).toBeTruthy();
  });

  it('registers route leave handler', () => {
    const router = mockRouter();

    mount(<WindowLeaveMessage dirty={false} route={{ __id__: 42 }} router={router} />);

    expect(router.setRouteLeaveHook).toHaveBeenCalledTimes(1);
    const [route, func] = lastCall(router.setRouteLeaveHook);
    expect(route).toEqual({ __id__: 42 });
    expect(isFunction(func)).toBeTruthy();
  });

  it('returns prompt if window is closed and view is dirty', () => {
    const router = mockRouter();

    mount(<WindowLeaveMessage dirty route={{}} router={router} />);
    const [_, fn] = lastCall(window.addEventListener);
    const e = {};

    const result = fn(e);

    expect(result).toEqual('Are you sure you want to leave the page? Any unsaved changes will be lost.');
    expect(e).toEqual({ returnValue: 'Are you sure you want to leave the page? Any unsaved changes will be lost.' });
  });

  it('does not return prompt if window is closed and view is not dirty', () => {
    const router = mockRouter();

    mount(<WindowLeaveMessage dirty={false} route={{}} router={router} />);
    const [_, fn] = lastCall(window.addEventListener);
    const e = {};

    const result = fn(e);

    expect(result).toBeNull();
    expect(e).toEqual({});
  });

  it('does not return prompt if window is closed and view is dirty but development mode is on', () => {
    const router = mockRouter();
    AppConfig.gl2DevMode = jest.fn(() => true);

    mount(<WindowLeaveMessage dirty route={{}} router={router} />);
    const [_, fn] = lastCall(window.addEventListener);
    const e = {};

    const result = fn(e);

    expect(result).toBeNull();
    expect(e).toEqual({});
  });

  it('returns prompt if route is left view is dirty', () => {
    const router = mockRouter();

    mount(<WindowLeaveMessage dirty route={{}} router={router} />);
    const [_, fn] = lastCall(router.setRouteLeaveHook);

    const result = fn();

    expect(result).toEqual('Are you sure you want to leave the page? Any unsaved changes will be lost.');
  });

  it('does not return prompt if route is left and view is not dirty', () => {
    const router = mockRouter();

    mount(<WindowLeaveMessage dirty={false} route={{}} router={router} />);
    const [_, fn] = lastCall(router.setRouteLeaveHook);

    const result = fn();

    expect(result).toBeNull();
  });

  it('does not return prompt if route is left and view is dirty but development mode is on', () => {
    const router = mockRouter();
    AppConfig.gl2DevMode = jest.fn(() => true);

    mount(<WindowLeaveMessage dirty route={{}} router={router} />);
    const [_, fn] = lastCall(router.setRouteLeaveHook);

    const result = fn();

    expect(result).toBeNull();
  });
});