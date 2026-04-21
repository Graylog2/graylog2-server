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
import * as React from 'react';
import { render } from 'wrappedTestingLibrary';

import asMock from 'helpers/mocking/AsMock';
import mockComponent from 'helpers/mocking/MockComponent';
import GenericPlot from 'views/components/visualizations/GenericPlot';

import TrafficGraph from './TrafficGraph';

jest.mock('views/components/visualizations/GenericPlot', () => jest.fn(mockComponent('GenericPlot')));

jest.mock('util/AppConfig', () => ({
  isCloud: jest.fn(() => false),
}));

const sampleTraffic = {
  '2026-04-07T00:00:00.000Z': 1024,
  '2026-04-08T00:00:00.000Z': 2048,
  '2026-04-09T00:00:00.000Z': 4096,
};

describe('TrafficGraph', () => {
  it('configures xaxis with tickformat and hoverformat to prevent millisecond display', () => {
    render(<TrafficGraph traffic={sampleTraffic} width={600} />);

    const { layout } = asMock(GenericPlot).mock.calls[0][0];

    expect(layout.xaxis).toMatchObject({
      type: 'date',
      tickformat: '%b %d',
      hoverformat: '%b %d, %Y',
    });
  });

  it('sets xaxis title to Date (UTC)', () => {
    render(<TrafficGraph traffic={sampleTraffic} width={600} />);

    const { layout } = asMock(GenericPlot).mock.calls[0][0];

    expect(layout.xaxis.title).toEqual({ text: 'Date (UTC)' });
  });
});
