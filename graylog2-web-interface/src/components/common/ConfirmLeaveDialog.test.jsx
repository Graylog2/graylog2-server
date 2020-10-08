// @flow strict
import * as React from 'react';
import { mount } from 'wrappedEnzyme';

import ConfirmLeaveDialog from './ConfirmLeaveDialog';

jest.mock('util/AppConfig');

jest.mock('react-router-dom', () => ({ Prompt: () => null }));

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
    mount(<ConfirmLeaveDialog />);

    expect(window.addEventListener).toHaveBeenCalledWith('beforeunload', expect.any(Function));
  });

  it('unregisters window beforeunload handler upon unmount', () => {
    const wrapper = mount(<ConfirmLeaveDialog />);

    wrapper.unmount();

    expect(window.removeEventListener).toHaveBeenCalledWith('beforeunload', expect.any(Function));
  });
});
