// @flow strict
import * as React from 'react';
import * as Immutable from 'immutable';
import { mount } from 'wrappedEnzyme';

import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import Viewport from 'views/logic/aggregationbuilder/visualizations/Viewport';
import Series from 'views/logic/aggregationbuilder/Series';
import WorldMapVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/WorldMapVisualizationConfig';
import RenderCompletionCallback from 'views/components/widgets/RenderCompletionCallback';
import WorldMapVisualization from '../WorldMapVisualization';

jest.mock('../MapVisualization', () => 'map-visualization');

describe('WorldMapVisualization', () => {
  // $FlowFixMe: type is always defined
  const config = AggregationWidgetConfig.builder().visualization(WorldMapVisualization.type).build();
  const effectiveTimerange = {
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

    const { onChange: _onChange } = mapVisualization.at(0).props();

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

    const { onChange: _onChange } = mapVisualization.at(0).props();

    const viewport = Viewport.create([0, 0], 0);
    _onChange(viewport);

    expect(onChange).toHaveBeenCalledWith(WorldMapVisualizationConfig.create(viewport));
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

    const { onRenderComplete } = wrapper.find('map-visualization').props();
    onRenderComplete();

    expect(renderCompletionCallback).toHaveBeenCalled();
  });

  it('renders Map component with correct data, when a metric is defined', () => {
    const series = new Series('count()');
    // $FlowFixMe: type is always defined
    const configWithMetric = AggregationWidgetConfig.builder().series([series]).visualization(WorldMapVisualization.type).build();
    const data = {
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
    // $FlowFixMe: type is always defined
    const configWithoutMetric = AggregationWidgetConfig.builder().visualization(WorldMapVisualization.type).build();
    const data = {
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
