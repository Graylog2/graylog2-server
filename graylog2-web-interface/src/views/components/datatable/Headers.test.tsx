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
import { render, screen } from 'wrappedTestingLibrary';
import { OrderedMap } from 'immutable';

import Series from 'views/logic/aggregationbuilder/Series';
import type Pivot from 'views/logic/aggregationbuilder/Pivot';
import type FieldType from 'views/logic/fieldtypes/FieldType';
import { FieldTypes } from 'views/logic/fieldtypes/FieldType';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import SeriesConfig from 'views/logic/aggregationbuilder/SeriesConfig';
import SortConfig from 'views/logic/aggregationbuilder/SortConfig';
import Direction from 'views/logic/aggregationbuilder/Direction';
import useViewsPlugin from 'views/test/testViewsPlugin';

import Headers from './Headers';

jest.mock('components/common/Timestamp', () => 'Timestamp');
jest.mock('views/hooks/useActiveQueryId', () => () => 'foobar');
jest.mock(
  'views/components/Field',
  () =>
    ({ children = undefined, type }: React.PropsWithChildren<{ type: FieldType }>) => (
      <span>
        {children} - {type.type}
      </span>
    ),
);

const onSortChange = jest.fn();
const seriesWithName = (fn: string, name: string) =>
  Series.forFunction(fn).toBuilder().config(SeriesConfig.empty().toBuilder().name(name).build()).build();

describe('Headers', () => {
  type RenderHeadersProps = {
    columnPivots?: Array<Pivot>;
    rowPivots?: Array<Pivot>;
    series?: Array<Series>;
    rollup?: boolean;
    actualColumnPivotFields?: Array<Array<string>>;
    fields?: Array<FieldTypeMapping>;
    sortConfigMap?: OrderedMap<string, SortConfig>;
  };

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
        <Headers
          columnPivots={columnPivots}
          rowPivots={rowPivots}
          series={series}
          borderedHeader={false}
          rollup={rollup}
          actualColumnPivotFields={actualColumnPivotFields}
          fields={Immutable.List(fields)}
          setLoadingState={() => {}}
          sortConfigMap={sortConfigMap}
          onSortChange={onSortChange}
          onSetColumnsWidth={() => {}}
          togglePin={() => {}}
        />
      </thead>
    </table>
  );

  useViewsPlugin();

  it('renders a header for every series', async () => {
    render(<RenderHeaders series={[Series.forFunction('count()'), Series.forFunction('avg(foo)')]} />);
    await screen.findByText(/count\(\)/);
    await screen.findByText(/avg\(foo\)/);
  });

  describe('infers types properly', () => {
    it('for non-renamed series', async () => {
      const series = [Series.forFunction('count()'), Series.forFunction('avg(foo)'), Series.forFunction('min(foo)')];
      render(<RenderHeaders series={series} fields={[FieldTypeMapping.create('foo', FieldTypes.DATE())]} />);

      await screen.findByText(/count\(\) - long/);
      await screen.findByText(/avg\(foo\) - date/);
      await screen.findByText(/min\(foo\) - date/);
    });

    it('for renamed series', async () => {
      const series = [
        seriesWithName('count()', 'Total Count'),
        seriesWithName('avg(foo)', 'Average Fooness'),
        seriesWithName('min(foo)', 'Minimal Fooness'),
      ];
      render(<RenderHeaders series={series} fields={[FieldTypeMapping.create('foo', FieldTypes.DATE())]} />);
      await screen.findByText(/Total Count - long/);
      await screen.findByText(/Average Fooness - date/);
      await screen.findByText(/Minimal Fooness - date/);
    });

    it('renders with `null` fields', async () => {
      const series = [seriesWithName('foo', 'Total Count'), seriesWithName('avg(foo)', 'Average Fooness')];

      render(<RenderHeaders series={series} fields={null} />);

      await screen.findByText(/Total Count/);
      await screen.findByText(/Average Fooness/);
    });
  });

  describe('renders sort icon', () => {
    const series = [
      seriesWithName('foo', 'Total Count'),
      seriesWithName('avg(foo)', 'Average Fooness'),
      seriesWithName('bar', 'Bar'),
    ];
    const mountWrapper = () =>
      render(
        <RenderHeaders
          series={series}
          fields={null}
          sortConfigMap={OrderedMap({
            foo: new SortConfig('pivot', 'foo', Direction.Ascending),
            bar: new SortConfig('pivot', 'bar', Direction.Descending),
          })}
        />,
      );

    it('active ascend', async () => {
      mountWrapper();

      expect(await screen.findByRole('button', { name: /sort foo descending/i })).toHaveClass('active');
    });

    it('active descent', async () => {
      mountWrapper();

      expect(await screen.findByRole('button', { name: /Remove bar sort/i })).toHaveClass('active');
    });

    it('inactive ascend', async () => {
      mountWrapper();

      expect(await screen.findByRole('button', { name: /Sort avg\(foo\) Ascending/i })).not.toHaveClass('active');
    });

    it('with sequence numbers', async () => {
      mountWrapper();

      const fooButton = await screen.findByRole('button', { name: /Sort foo descending/i });
      const barButton = await screen.findByRole('button', { name: /Remove bar sort/i });

      expect(fooButton).toHaveClass('active');
      expect(fooButton.innerHTML).toContain('1');

      expect(barButton).toHaveClass('active');
      expect(barButton.innerHTML).toContain('2');
    });
  });
});
