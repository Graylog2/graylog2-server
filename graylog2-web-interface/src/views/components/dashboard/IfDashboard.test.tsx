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
import 'helpers/mocking/react-dom_mock';

import View from 'views/logic/views/View';
import ViewTypeContext from 'views/components/contexts/ViewTypeContext';

import IfDashboard from './IfDashboard';

describe('IfDashboard', () => {
  it('should render children with dashboard context', () => {
    const wrapper = mount(
      <ViewTypeContext.Provider value={View.Type.Dashboard}>
        <span>I must not fear.</span>
        <IfDashboard>
          <span>Fear is the mind-killer.</span>
        </IfDashboard>
      </ViewTypeContext.Provider>,
    );

    expect(wrapper).toMatchSnapshot();
  });

  it('should not render children without dashboard context', () => {
    const wrapper = mount(
      <ViewTypeContext.Provider value={View.Type.Search}>
        <span>I must not fear.</span>
        <IfDashboard>
          <span>Fear is the mind-killer.</span>
        </IfDashboard>
      </ViewTypeContext.Provider>,
    );

    expect(wrapper).toMatchSnapshot();
  });

  it('should not render children without context', () => {
    const wrapper = mount(
      <div>
        <span>I must not fear.</span>
        <IfDashboard>
          <span>Fear is the mind-killer.</span>
        </IfDashboard>
      </div>,
    );

    expect(wrapper).toMatchSnapshot();
  });
});
