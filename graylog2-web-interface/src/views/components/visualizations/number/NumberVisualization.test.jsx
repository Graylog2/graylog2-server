import React from 'react';
import { mount } from 'enzyme';
import { List } from 'immutable';
import renderer from 'react-test-renderer';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import { FieldTypes } from 'views/logic/fieldtypes/FieldType';
import NumberVisualization from './NumberVisualization';

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
  const data = [{
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
  }];
  const currentView = { activeQuery: 'dead-beef' };
  const fields = List([FieldTypeMapping.create('lines_add', FieldTypes.INT)]);

  it('should render a number visualization', () => {
    const wrapper = renderer.create(<NumberVisualization data={data}
                                                         width={200}
                                                         height={200}
                                                         fields={fields}
                                                         currentView={currentView} />);
    expect(wrapper.toJSON()).toMatchSnapshot();
  });

  it('changes font size upon resize', () => {
    const wrapper = mount(<NumberVisualization data={data}
                                               width={300}
                                               height={300}
                                               fields={fields}
                                               currentView={currentView} />);
    expect(wrapper.state().fontSize).toBe(20);

    wrapper.instance().getContainer = jest
      .fn()
      .mockImplementationOnce(() => ({ childNodes: [{ offsetHeight: 90, offsetWidth: 90 }] }))
      .mockImplementationOnce(() => ({ childNodes: [{ offsetHeight: 100, offsetWidth: 100 }] }));

    wrapper.setProps({ height: 125, width: 125 });

    expect(wrapper.state().fontSize).toBe(22);
  });
  it('renders 0 if value is 0', () => {
    const dataWithZeroValue = [{
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
    }];
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
