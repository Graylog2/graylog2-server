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

import BarVisualizationConfiguration from './BarVisualizationConfiguration';

import BarVisualizationConfig from '../../logic/aggregationbuilder/visualizations/BarVisualizationConfig';

describe('BarVisualizationConfiguration', () => {
  // NOTE: Why is this testing `HoverForHelp` component?

  it('should render without props', () => {
    const wrapper = mount(<BarVisualizationConfiguration />);

    expect(wrapper).toExist();
  });

  // NOTE: Why is this testing `HoverForHelp` component?

  it('should render with props', () => {
    const wrapper = mount(<BarVisualizationConfiguration config={BarVisualizationConfig.create('stack')} />);

    expect(wrapper).toExist();
  });
});
