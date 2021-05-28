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
import { mount } from 'wrappedEnzyme';

import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import Viewport from 'views/logic/aggregationbuilder/visualizations/Viewport';
import Series from 'views/logic/aggregationbuilder/Series';
import RenderCompletionCallback from 'views/components/widgets/RenderCompletionCallback';
import { AbsoluteTimeRange } from 'views/logic/queries/Query';
import { Rows } from 'views/logic/searchtypes/pivot/PivotHandler';

import WorldMapVisualization from '../WorldMapVisualization';

jest.mock('../MapVisualization', () => 'map-visualization');

type MapVisualizationProps = HTMLAttributes & {
  onChange: (viewPort: Viewport) => void;
  onRenderComplete: () => void;
};

describe('WorldMapVisualization', () => {
  const config = AggregationWidgetConfig.builder().visualization(WorldMapVisualization.type).build();
  const effectiveTimerange: AbsoluteTimeRange = {
    from: '2019-07-04T13:37:00Z',
    to: '2019-07-05T13:37:00Z',
    type: 'absolute',
  };

  it('does not call onChange when not editing', () => {
    const onChange = jest.fn();
    const wrapper = mount(<WorldMapVisualization config={config}
                                                 data={{ chart: [] }}
                                                 editing={false}
                                                 effectiveTimerange={effectiveTimerange}
                                                 fields={Immutable.List()}
                                                 onChange={onChange}
                                                 toggleEdit={() => {}}
                                                 height={1024}
                                                 width={800} />);
    const mapVisualization = wrapper.find('map-visualization');

    const { onChange: _onChange } = mapVisualization.at(0).props() as MapVisualizationProps;

    const viewport = Viewport.create([0, 0], 0);

    _onChange(viewport);

    expect(onChange).not.toHaveBeenCalled();
  });

  it('does call onChange when editing', () => {
    const onChange = jest.fn();
    const wrapper = mount(<WorldMapVisualization config={config}
                                                 data={{ chart: [] }}
                                                 editing
                                                 effectiveTimerange={effectiveTimerange}
                                                 fields={Immutable.List()}
                                                 onChange={onChange}
                                                 toggleEdit={() => {}}
                                                 height={1024}
                                                 width={800} />);
    const mapVisualization = wrapper.find('map-visualization');

    const { onChange: _onChange } = mapVisualization.at(0).props() as MapVisualizationProps;

    const viewport = Viewport.create([0, 0], 0);

    _onChange(viewport);

    expect(onChange).toHaveBeenCalledWith({
      zoom: 0,
      centerX: 0,
      centerY: 0,
    });
  });

  it('calls render completion callback after first render', () => {
    const renderCompletionCallback = jest.fn();
    const wrapper = mount((
      <RenderCompletionCallback.Provider value={renderCompletionCallback}>
        <WorldMapVisualization config={config}
                               data={{ chart: [] }}
                               editing
                               effectiveTimerange={effectiveTimerange}
                               fields={Immutable.List()}
                               onChange={() => {}}
                               toggleEdit={() => {}}
                               height={1024}
                               width={800} />
      </RenderCompletionCallback.Provider>
    ));

    const { onRenderComplete } = wrapper.find('map-visualization').props() as MapVisualizationProps;

    onRenderComplete();

    expect(renderCompletionCallback).toHaveBeenCalled();
  });

  it('renders Map component with correct data, when a metric is defined', () => {
    const series = new Series('count()');
    const configWithMetric = AggregationWidgetConfig.builder().series([series]).visualization(WorldMapVisualization.type).build();
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
    const mapData = [{
      keys: [{}, {}],
      name: 'count()',
      values: { '37.751,-97.822': 25, '35.69,139.69': 6 },
    }];
    const wrapper = mount((
      <WorldMapVisualization config={configWithMetric}
                             data={data}
                             editing
                             effectiveTimerange={effectiveTimerange}
                             fields={Immutable.List()}
                             onChange={() => {}}
                             toggleEdit={() => {}}
                             height={1024}
                             width={800} />
    ));
    const mapVisualization = wrapper.find('map-visualization');

    expect(mapVisualization).toHaveProp('data', mapData);
  });

  it('renders Map component with correct data, when no metric is defined', () => {
    const configWithoutMetric = AggregationWidgetConfig.builder().visualization(WorldMapVisualization.type).build();
    const data: Record<string, Rows> = {
      chart: [
        { key: ['37.751,-97.822'], values: [], source: 'leaf' },
        { key: ['35.69,139.69'], values: [], source: 'leaf' },
      ],
    };
    const mapData = [{
      keys: [{}, {}],
      name: 'No metric defined',
      values: { '37.751,-97.822': null, '35.69,139.69': null },
    }];
    const wrapper = mount((
      <WorldMapVisualization config={configWithoutMetric}
                             data={data}
                             editing
                             effectiveTimerange={effectiveTimerange}
                             fields={Immutable.List()}
                             onChange={() => {}}
                             toggleEdit={() => {}}
                             height={1024}
                             width={800} />
    ));
    const mapVisualization = wrapper.find('map-visualization');

    expect(mapVisualization).toHaveProp('data', mapData);
  });
});
