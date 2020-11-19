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
// @flow strict
import * as React from 'react';
import { mount } from 'wrappedEnzyme';

import ConfirmLeaveDialog from './ConfirmLeaveDialog';

jest.mock('util/AppConfig');

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
