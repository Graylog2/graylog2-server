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

import TimeHistogramPivot from './TimeHistogramPivot';

describe('TimeHistogramPivot', () => {
  it('renders for auto interval', () => {
    const wrapper = mount(<TimeHistogramPivot onChange={() => {}} value={{ interval: { type: 'auto' } }} />);

    expect(wrapper).not.toBeEmptyRender();
  });

  it('renders for invalid pivot config', () => {
    /* eslint-disable no-console */
    const oldConsoleError = console.error;

    console.error = () => {};

    const wrapper = mount(<TimeHistogramPivot onChange={() => {}} value={{}} />);

    expect(wrapper).not.toBeEmptyRender();

    console.error = oldConsoleError;
    /* eslint-enable no-console */
  });
});
