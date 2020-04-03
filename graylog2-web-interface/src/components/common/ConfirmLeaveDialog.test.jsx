// @flow strict
import * as React from 'react';
import { mount } from 'wrappedEnzyme';

import ConfirmLeaveDialog from './ConfirmLeaveDialog';

jest.mock('react-router', () => ({ withRouter: (x) => x }));
jest.mock('util/AppConfig');

const mockRouter = () => ({
  setRouteLeaveHook: jest.fn(),
});

describe('ConfirmLeaveDialog', () => {
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

    mount(<ConfirmLeaveDialog route={{}} router={router} />);

    expect(window.addEventListener).toHaveBeenCalledWith('beforeunload', expect.any(Function));
  });

  it('unregisters window beforeunload handler upon unmount', () => {
    const router = mockRouter();
    const unsubscribe = jest.fn();
    router.setRouteLeaveHook.mockReturnValue(unsubscribe);

    const wrapper = mount(<ConfirmLeaveDialog route={{}} router={router} />);

    wrapper.unmount();

    expect(window.removeEventListener).toHaveBeenCalledWith('beforeunload', expect.any(Function));
  });

  it('registers route leave handler', () => {
    const router = mockRouter();

    mount(<ConfirmLeaveDialog route={{ __id__: 42 }} router={router} />);

    expect(router.setRouteLeaveHook).toHaveBeenCalledTimes(1);
    expect(router.setRouteLeaveHook).toHaveBeenCalledWith({ __id__: 42 }, expect.any(Function));
  });

  it('unregisters route leave handler upon unmount', () => {
    const router = mockRouter();
    const unsubscribe = jest.fn();
    router.setRouteLeaveHook.mockReturnValue(unsubscribe);

    const wrapper = mount(<ConfirmLeaveDialog route={{ __id__: 42 }} router={router} />);

    wrapper.unmount();
    expect(unsubscribe).toHaveBeenCalled();
  });
});
