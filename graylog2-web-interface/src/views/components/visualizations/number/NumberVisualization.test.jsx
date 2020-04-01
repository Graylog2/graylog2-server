// @flow strict
import * as React from 'react';
import { mount } from 'wrappedEnzyme';
import { List } from 'immutable';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import { FieldTypes } from 'views/logic/fieldtypes/FieldType';
import RenderCompletionCallback from 'views/components/widgets/RenderCompletionCallback';
import NumberVisualization from './NumberVisualization';
import AggregationWidgetConfig from '../../../logic/aggregationbuilder/AggregationWidgetConfig';

jest.mock('react-sizeme', () => ({
  SizeMe: ({ children: fn }) => fn({ size: { width: 320, height: 240 } }),
}));
jest.mock('./AutoFontSizer', () => ({ children }) => children);
jest.mock('stores/connect', () => (x) => x);
jest.mock('views/components/messagelist/CustomHighlighting', () => {
  /* eslint-disable-next-line react/prop-types */
  return ({ children }) => {
    return <div>{children}</div>;
  };
});

jest.mock('views/components/Value', () => {
  /* eslint-disable-next-line react/prop-types */
  return ({ value }) => {
    return <div>{value}</div>;
  };
});

describe('NumberVisualization', () => {
  const data = {
    chart:
      [{
        key: [],
        source: 'leaf',
        values: [
          {
            key: ['sum(lines_add)'],
            rollup: true,
            source: 'row-leaf',
            value: 2134342,
          },
        ],
      }],
  };
  const currentView = { activeQuery: 'dead-beef' };
  const fields = List([FieldTypeMapping.create('lines_add', FieldTypes.INT())]);

  const SimplifiedNumberVisualization = (props = {}) => (
    <NumberVisualization data={data}
                         width={200}
                         height={200}
                         fields={fields}
                         currentView={currentView}
                         onChange={() => {}}
                         toggleEdit={() => {}}
                         effectiveTimerange={{
                           type: 'relative',
                           range: 300,
                         }}
                         config={AggregationWidgetConfig.builder().build()}
                         {...props} />
  );

  it('should render a number visualization', () => {
    const wrapper = mount(<SimplifiedNumberVisualization />);
    expect(wrapper.find(NumberVisualization)).toMatchSnapshot();
  });

  it('calls render completion callback after first render', (done) => {
    const onRenderComplete = jest.fn(done);
    mount((
      <RenderCompletionCallback.Provider value={onRenderComplete}>
        <SimplifiedNumberVisualization />
      </RenderCompletionCallback.Provider>
    ));
  });

  it('renders 0 if value is 0', () => {
    const dataWithZeroValue = {
      chart: [{
        key: [],
        source: 'leaf',
        values: [
          {
            key: ['count()'],
            rollup: true,
            source: 'row-leaf',
            value: 0,
          },
        ],
      }],
    };
    const wrapper = mount(<SimplifiedNumberVisualization data={dataWithZeroValue} />);
    expect(wrapper).toHaveText('0');
  });
  it('renders N/A if value is null', () => {
    const dataWithZeroValue = {
      chart: [{
        key: [],
        source: 'leaf',
        values: [
          {
            key: ['count()'],
            rollup: true,
            source: 'row-leaf',
            value: null,
          },
        ],
      }],
    };
    const wrapper = mount(<SimplifiedNumberVisualization data={dataWithZeroValue} />);
    expect(wrapper).toHaveText('N/A');
  });
});
