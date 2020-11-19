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
// @flow strict
import * as React from 'react';
import { mount } from 'wrappedEnzyme';
import { List } from 'immutable';

import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import { FieldTypes } from 'views/logic/fieldtypes/FieldType';
import RenderCompletionCallback from 'views/components/widgets/RenderCompletionCallback';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import Series from 'views/logic/aggregationbuilder/Series';
import { Rows } from 'views/logic/searchtypes/pivot/PivotHandler';
import { CurrentViewType } from 'views/components/CustomPropTypes';

import NumberVisualization from './NumberVisualization';

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

type Data = Record<string, Rows>;
type SUTProps = {
  data?: Data;
};

describe('NumberVisualization', () => {
  const data: Data = {
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
  const currentView: CurrentViewType = { activeQuery: 'dead-beef' };
  const fields = List([FieldTypeMapping.create('lines_add', FieldTypes.INT())]);

  const SimplifiedNumberVisualization = (props: SUTProps = {}) => (
    <NumberVisualization data={data}
                         width={200}
                         height={200}
                         fields={fields}
                         // @ts-ignore
                         currentView={currentView}
                         onChange={() => {}}
                         toggleEdit={() => {}}
                         effectiveTimerange={{
                           from: '2020-01-10T13:23:42.000Z',
                           to: '2020-01-10T14:23:42.000Z',
                           type: 'absolute',
                         }}
                         config={AggregationWidgetConfig.builder()
                           .series([Series.forFunction('count()')])
                           .build()}
                         {...props} />
  );

  it('should render a number visualization', () => {
    const wrapper = mount(<SimplifiedNumberVisualization />);

    expect(wrapper.find(NumberVisualization)).toExist();
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
    const dataWithZeroValue: { chart: Rows } = {
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
    const dataWithZeroValue: Data = {
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
