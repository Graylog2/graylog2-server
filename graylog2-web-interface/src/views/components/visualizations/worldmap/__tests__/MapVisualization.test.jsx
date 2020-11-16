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

import * as fixtures from './MapVisualization.fixtures';

import MapVisualization from '../MapVisualization';

describe('MapVisualization', () => {
  it('renders with minimal props', () => {
    const wrapper = mount(<MapVisualization id="somemap"
                                            onChange={() => {}}
                                            data={[]}
                                            height={1600}
                                            width={900} />);

    expect(wrapper.find('Map')).toExist();
  });

  it('does not render circle markers for invalid data', () => {
    const wrapper = mount(<MapVisualization id="somemap"
                                            onChange={() => {}}
                                            data={fixtures.invalidData}
                                            height={1600}
                                            width={900} />);

    expect(wrapper.find('Map')).toExist();
    expect(wrapper.find('CircleMarker')).not.toExist();
  });
});
