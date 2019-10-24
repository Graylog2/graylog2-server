// @flow strict
import * as React from 'react';
import * as Immutable from 'immutable';
import { mount } from 'enzyme';

import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import Viewport from 'views/logic/aggregationbuilder/visualizations/Viewport';
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
                                                 data={[]}
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
                                                 data={[]}
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
                               data={[]}
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
});
