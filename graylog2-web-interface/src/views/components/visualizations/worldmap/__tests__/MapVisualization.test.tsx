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
import { render } from 'wrappedTestingLibrary';

import * as fixtures from './MapVisualization.fixtures';

import MapVisualization from '../MapVisualization';

describe('MapVisualization', () => {
  it('renders with minimal props', async () => {
    const { container } = render(<MapVisualization id="somemap"
                                                   onChange={() => {}}
                                                   data={[]}
                                                   height={1600}
                                                   width={900} />);

    // eslint-disable-next-line testing-library/no-container
    expect(container.querySelector('div.map#visualization-somemap')).toBeInTheDocument();
  });

  it('does not render circle markers for invalid data', async () => {
    const { container } = render(<MapVisualization id="somemap"
                                                   onChange={() => {}}
                                                   data={fixtures.invalidData}
                                                   height={1600}
                                                   width={900} />);

    /* eslint-disable testing-library/no-container */
    expect(container.querySelector('div.map#visualization-somemap')).toBeInTheDocument();
    expect(container.querySelector('CircleMarker')).not.toBeInTheDocument();
    /* eslint-enable testing-library/no-container */
  });
});
