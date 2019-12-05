// @flow strict
import * as React from 'react';
import { mount } from 'enzyme';
import { List } from 'immutable';
import renderer from 'react-test-renderer';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import { FieldTypes } from 'views/logic/fieldtypes/FieldType';
import RenderCompletionCallback from 'views/components/widgets/RenderCompletionCallback';
import NumberVisualization from './NumberVisualization';

jest.mock('react-sizeme', () => ({
  SizeMe: ({ children: fn }) => fn({ size: { width: 320, height: 240 } }),
}));
jest.mock('./AutoFontSizer', () => ({ children }) => children);
jest.mock('stores/connect', () => x => x);
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

  it('should render a number visualization', () => {
    const wrapper = renderer.create(<NumberVisualization data={data}
                                                         width={200}
                                                         height={200}
                                                         fields={fields}
                                                         currentView={currentView} />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });

  it('calls render completion callback after first render', (done) => {
    const Component = () => (
      <NumberVisualization data={data}
                           width={200}
                           height={200}
                           fields={fields}
                           currentView={currentView} />
    );
    const onRenderComplete = jest.fn(done);
    renderer.create((
      <RenderCompletionCallback.Provider value={onRenderComplete}>
        <Component />
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
    const wrapper = mount(<NumberVisualization data={dataWithZeroValue}
                                               width={300}
                                               height={300}
                                               fields={fields}
                                               currentView={currentView} />);
    expect(wrapper).toHaveText('0');
  });
  it('renders N/A if value is null', () => {
    const dataWithZeroValue = [{
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
    }];
    const wrapper = mount(<NumberVisualization data={dataWithZeroValue}
                                               width={300}
                                               height={300}
                                               fields={fields}
                                               currentView={currentView} />);
    expect(wrapper).toHaveText('N/A');
  });
});
