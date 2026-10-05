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
import Plot from 'views/components/visualizations/plotly/AsyncPlot';

import GenericPlot from './GenericPlot';

jest.mock('views/components/visualizations/plotly/AsyncPlot', () => jest.fn(() => <div data-testid="plot" />));

const lastLayout = () => {
  const { calls } = asMock(Plot).mock;

  return calls[calls.length - 1][0].layout;
};

describe('GenericPlot datarevision', () => {
  beforeEach(() => {
    asMock(Plot).mockClear();
  });

  it('bumps layout.datarevision when the chart data changes so Plotly repaints in-trace labels', () => {
    const initial = [{ type: 'sankey', node: { label: ['id-1'] } }];
    const { rerender } = render(<GenericPlot chartData={initial} />);

    const firstRevision = lastLayout().datarevision;

    // Same data reference on re-render: revision must stay stable (no needless replots).
    rerender(<GenericPlot chartData={initial} />);

    expect(lastLayout().datarevision).toBe(firstRevision);

    // New data (e.g. an asset id resolved to a name): revision must change.
    const resolved = [{ type: 'sankey', node: { label: ['Asset id-1'] } }];
    rerender(<GenericPlot chartData={resolved} />);

    expect(lastLayout().datarevision).not.toBe(firstRevision);
  });
});
