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
import { useState, useEffect, useCallback, useMemo } from 'react';
import { render, waitFor } from 'wrappedTestingLibrary';
import * as Immutable from 'immutable';

import mockComponent from 'helpers/mocking/MockComponent';
import Pivot from 'views/logic/aggregationbuilder/Pivot';
import Series from 'views/logic/aggregationbuilder/Series';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import FieldType from 'views/logic/fieldtypes/FieldType';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import type { FieldTypeMappingsList } from 'views/logic/fieldtypes/types';
import type { AbsoluteTimeRange } from 'views/logic/queries/Query';
import TestStoreProvider from 'views/test/TestStoreProvider';
import useViewsPlugin from 'views/test/testViewsPlugin';
import { usePlugin } from 'views/test/testPlugins';
import asMock from 'helpers/mocking/AsMock';
import GenericPlot from 'views/components/visualizations/GenericPlot';
import KeyMapperProvider from 'views/components/visualizations/KeyMapperProvider';

import * as fixtures from './fixtures';

import SankeyVisualization from '../SankeyVisualization';

jest.mock('../../GenericPlot', () => jest.fn(mockComponent('GenericPlot')));

// Async asset-style resolver: ids resolve to names AFTER mount, mirroring getTitles.
const asyncAssetPlugin = {
  exports: {
    visualizationKeyMappers: [
      {
        type: 'associated-assets',
        useKeyMapper: (keys: Array<string>) => {
          const [titles, setTitles] = useState<Record<string, string>>({});
          const joined = useMemo(() => keys.join('|'), [keys]);

          useEffect(() => {
            setTitles(Object.fromEntries(keys.map((k) => [k, `Asset ${k}`])));
            // eslint-disable-next-line react-hooks/exhaustive-deps
          }, [joined]);

          return useCallback((k: string) => titles[k] ?? k, [titles]);
        },
      },
    ],
  },
};

const effectiveTimerange: AbsoluteTimeRange = {
  type: 'absolute',
  from: '2022-04-27T12:15:59.633Z',
  to: '2022-04-27T12:20:59.633Z',
};

const fields = Immutable.List([
  new FieldTypeMapping('a', FieldType.create('streams', [])),
  new FieldTypeMapping('b', FieldType.create('associated-assets', [])),
]) as FieldTypeMappingsList;

const config = AggregationWidgetConfig.builder()
  .rowPivots([Pivot.createValues(['a']), Pivot.createValues(['b'])])
  .series([Series.forFunction('count()')])
  .visualization('sankey')
  .build();

const baseProps = {
  effectiveTimerange,
  fields,
  height: 800,
  width: 600,
  setLoadingState: () => {},
  onChange: () => {},
  toggleEdit: () => {},
  config,
};

const lastTrace = () => {
  const { calls } = asMock(GenericPlot).mock;

  return calls[calls.length - 1][0].chartData[0];
};

describe('SankeyVisualization async asset labels', () => {
  useViewsPlugin();
  usePlugin(asyncAssetPlugin);

  beforeEach(() => {
    asMock(GenericPlot).mockClear();
  });

  it('passes asset names (not raw ids) to the plot once titles resolve', async () => {
    render(
      <TestStoreProvider>
        <KeyMapperProvider data={fixtures.twoRowPivots} config={config} fields={fields}>
          <SankeyVisualization {...baseProps} data={fixtures.twoRowPivots} />
        </KeyMapperProvider>
      </TestStoreProvider>,
    );

    await waitFor(() => expect(lastTrace().node.label).toEqual(['a1', 'Asset b1', 'Asset b2', 'a2']));
  });
});
