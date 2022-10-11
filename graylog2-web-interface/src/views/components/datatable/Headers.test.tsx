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
import { mount } from 'wrappedEnzyme';
import { OrderedMap } from 'immutable';

import Series from 'views/logic/aggregationbuilder/Series';
import type Pivot from 'views/logic/aggregationbuilder/Pivot';
import { FieldTypes } from 'views/logic/fieldtypes/FieldType';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import SeriesConfig from 'views/logic/aggregationbuilder/SeriesConfig';
import SortConfig from 'views/logic/aggregationbuilder/SortConfig';
import Direction from 'views/logic/aggregationbuilder/Direction';

import Headers from './Headers';

jest.mock('components/common/Timestamp', () => 'Timestamp');
const onSortChange = jest.fn();
const seriesWithName = (fn, name) => Series.forFunction(fn)
  .toBuilder()
  .config(SeriesConfig.empty()
    .toBuilder()
    .name(name)
    .build())
  .build();

describe('Headers', () => {
  /* eslint-disable react/require-default-props */
  type RenderHeadersProps = {
    columnPivots?: Array<Pivot>,
    rowPivots?: Array<Pivot>,
    series?: Array<Series>,
    rollup?: boolean,
    actualColumnPivotFields?: Array<Array<string>>,
    fields?: Array<FieldTypeMapping>,
    sortConfigMap?: OrderedMap<string, SortConfig>
  };
  /* eslint-enable react/require-default-props */

  const RenderHeaders = ({
    columnPivots = [],
    rowPivots = [],
    series = [],
    rollup = true,
    actualColumnPivotFields = [],
    fields = [],
    sortConfigMap = OrderedMap([]),
  }: RenderHeadersProps) => (
    <table>
      <thead>
        <Headers activeQuery="queryId"
                 columnPivots={columnPivots}
                 rowPivots={rowPivots}
                 series={series}
                 rollup={rollup}
                 actualColumnPivotFields={actualColumnPivotFields}
                 fields={Immutable.List(fields)}
                 sortConfigMap={sortConfigMap}
                 onSortChange={onSortChange}
                 onSetColumnsWidth={() => {}}
                 togglePin={() => {}} />
      </thead>
    </table>
  );

  it('renders a header for every series', () => {
    const wrapper = mount(<RenderHeaders series={[
      Series.forFunction('count()'),
      Series.forFunction('avg(foo)'),
    ]} />);

    expect(wrapper).not.toBeEmptyRender();

    const fields = wrapper.find('Field');

    expect(fields).toHaveLength(2);
  });

  describe('infers types properly', () => {
    const expectCorrectTypes = (wrapper) => {
      const countField = wrapper.find('Field[name="count()"]');

      expect(countField).toHaveProp('type', FieldTypes.LONG());

      const avgField = wrapper.find('Field[name="avg(foo)"]');

      expect(avgField).toHaveProp('type', FieldTypes.DATE());

      const minField = wrapper.find('Field[name="min(foo)"]');

      expect(minField).toHaveProp('type', FieldTypes.DATE());
    };

    it('for non-renamed series', () => {
      const series = [
        Series.forFunction('count()'),
        Series.forFunction('avg(foo)'),
        Series.forFunction('min(foo)'),
      ];
      const wrapper = mount((
        <RenderHeaders series={series}
                       fields={[FieldTypeMapping.create('foo', FieldTypes.DATE())]} />
      ));

      expectCorrectTypes(wrapper);
    });

    it('for renamed series', () => {
      const series = [
        seriesWithName('count()', 'Total Count'),
        seriesWithName('avg(foo)', 'Average Foness'),
        seriesWithName('min(foo)', 'Minimal Fooness'),
      ];
      const wrapper = mount((
        <RenderHeaders series={series}
                       fields={[FieldTypeMapping.create('foo', FieldTypes.DATE())]} />
      ));

      expectCorrectTypes(wrapper);
    });

    it('renders with `null` fields', () => {
      const series = [
        seriesWithName('foo', 'Total Count'),
        seriesWithName('avg(foo)', 'Average Foness'),
      ];

      const wrapper = mount((
        <RenderHeaders series={series}
                       fields={null} />
      ));

      expect(wrapper).toExist();
    });
  });

  describe('render sort icon', () => {
    const series = [
      seriesWithName('foo', 'Total Count'),
      seriesWithName('avg(foo)', 'Average Foness'),
      seriesWithName('bar', 'Bar'),
    ];
    const mountWrapper = () => mount((
      <RenderHeaders series={series}
                     fields={null}
                     sortConfigMap={OrderedMap({
                       foo: new SortConfig('pivot', 'foo', Direction.Ascending),
                       bar: new SortConfig('pivot', 'bar', Direction.Descending),
                     })} />
    ));

    it('active ascend', () => {
      const wrapper = mountWrapper();

      const ascIcon = wrapper
        .find('FieldSortIcon[fieldName="foo"]')
        .find('button[data-testid="sort-icon-foo"].active')
        .find('Icon[name="sort-amount-down"]');

      expect(ascIcon).toExist();
    });

    it('active descent', () => {
      const wrapper = mountWrapper();

      const dscIcon = wrapper
        .find('FieldSortIcon[fieldName="bar"]')
        .find('button[data-testid="sort-icon-bar"].active')
        .find('Icon[name="sort-amount-up"]');

      expect(dscIcon).toExist();
    });

    it('inactive ascend', () => {
      const wrapper = mountWrapper();

      const inactiveIcon = wrapper
        .find('FieldSortIcon[fieldName="avg(foo)"]')
        .find('button[data-testid="sort-icon-avg(foo)"]:not(.active)')
        .find('Icon[name="sort-amount-down"]');

      expect(inactiveIcon).toExist();
    });

    it('with sequence numbers', () => {
      const wrapper = mountWrapper();

      const fooButton = wrapper
        .find('FieldSortIcon[fieldName="foo"]')
        .find('button[data-testid="sort-icon-foo"].active')
        .find('span').text();
      const barButton = wrapper
        .find('FieldSortIcon[fieldName="bar"]')
        .find('button[data-testid="sort-icon-bar"].active')
        .find('span').text();

      expect(fooButton).toBe('1');
      expect(barButton).toBe('2');
    });
  });
});
