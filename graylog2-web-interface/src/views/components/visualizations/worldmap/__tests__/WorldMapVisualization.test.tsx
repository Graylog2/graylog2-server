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
import * as Immutable from 'immutable';
import type { HTMLAttributes } from 'enzyme';
import { render, screen } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import MockViewport from 'views/logic/aggregationbuilder/visualizations/Viewport';
import Series from 'views/logic/aggregationbuilder/Series';
import RenderCompletionCallback from 'views/components/widgets/RenderCompletionCallback';
import type { AbsoluteTimeRange } from 'views/logic/queries/Query';
import type { Rows } from 'views/logic/searchtypes/pivot/PivotHandler';
import type FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import MapVisualization from 'views/components/visualizations/worldmap/MapVisualization';

import WorldMapVisualization from '../WorldMapVisualization';

jest.mock('../MapVisualization', () =>
  jest.fn(({ onChange, onRenderComplete }: MapVisualizationProps) => {
    onRenderComplete();

    return (
      <button type="button" onClick={() => onChange(MockViewport.create([0, 0], 0))}>
        Trigger change
      </button>
    );
  }),
);

type MapVisualizationProps = HTMLAttributes & {
  onChange: (viewPort: MockViewport) => void;
  onRenderComplete: () => void;
};

describe('WorldMapVisualization', () => {
  const config = AggregationWidgetConfig.builder().visualization(WorldMapVisualization.type).build();
  const effectiveTimerange: AbsoluteTimeRange = {
    from: '2019-07-04T13:37:00Z',
    to: '2019-07-05T13:37:00Z',
    type: 'absolute',
  };

  const defaultProps = {
    config,
    data: { chart: [] },
    editing: false,
    effectiveTimerange: effectiveTimerange,
    fields: Immutable.List<FieldTypeMapping>(),
    onChange: () => {},
    toggleEdit: () => {},
    setLoadingState: () => {},
    height: 1024,
    width: 800,
  };

  it('does not call onChange when not editing', async () => {
    const onChange = jest.fn();
    render(<WorldMapVisualization {...defaultProps} onChange={onChange} />);
    await userEvent.click(await screen.findByRole('button', { name: 'Trigger change' }));

    expect(onChange).not.toHaveBeenCalled();
  });

  it('does call onChange when editing', async () => {
    const onChange = jest.fn();
    render(<WorldMapVisualization {...defaultProps} onChange={onChange} editing />);
    await userEvent.click(await screen.findByRole('button', { name: 'Trigger change' }));

    expect(onChange).toHaveBeenCalledWith({
      zoom: 0,
      centerX: 0,
      centerY: 0,
    });
  });

  it('calls render completion callback after first render', () => {
    const renderCompletionCallback = jest.fn();
    render(
      <RenderCompletionCallback.Provider value={renderCompletionCallback}>
        <WorldMapVisualization {...defaultProps} editing />
      </RenderCompletionCallback.Provider>,
    );

    expect(renderCompletionCallback).toHaveBeenCalled();
  });

  it('renders Map component with correct data, when a metric is defined', () => {
    const series = new Series('count()');
    const configWithMetric = AggregationWidgetConfig.builder()
      .series([series])
      .visualization(WorldMapVisualization.type)
      .build();
    const data: Record<string, Rows> = {
      chart: [
        {
          key: ['37.751,-97.822'],
          values: [{ key: ['count()'], value: 25, rollup: true, source: 'row-leaf' }],
          source: 'leaf',
        },
        {
          key: ['35.69,139.69'],
          values: [{ key: ['count()'], value: 6, rollup: true, source: 'row-leaf' }],
          source: 'leaf',
        },
      ],
    };
    const mapData = [
      {
        keys: [{}, {}],
        name: 'count()',
        values: { '37.751,-97.822': 25, '35.69,139.69': 6 },
      },
    ];
    render(<WorldMapVisualization {...defaultProps} config={configWithMetric} data={data} editing />);

    expect(MapVisualization).toHaveBeenCalledWith(expect.objectContaining({ data: mapData }), {});
  });

  it('renders Map component with correct data, when no metric is defined', () => {
    const configWithoutMetric = AggregationWidgetConfig.builder().visualization(WorldMapVisualization.type).build();
    const data: Record<string, Rows> = {
      chart: [
        { key: ['37.751,-97.822'], values: [], source: 'leaf' },
        { key: ['35.69,139.69'], values: [], source: 'leaf' },
      ],
    };
    const mapData = [
      {
        keys: [{}, {}],
        name: 'No metric defined',
        values: { '37.751,-97.822': null, '35.69,139.69': null },
      },
    ];
    render(<WorldMapVisualization {...defaultProps} config={configWithoutMetric} data={data} editing />);

    expect(MapVisualization).toHaveBeenCalledWith(expect.objectContaining({ data: mapData }), {});
  });
});
