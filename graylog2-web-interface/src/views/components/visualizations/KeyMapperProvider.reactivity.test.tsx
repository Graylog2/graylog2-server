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
import { useContext, useState, useEffect, useCallback, useMemo } from 'react';
import { render, screen } from 'wrappedTestingLibrary';
import * as Immutable from 'immutable';

import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import FieldType from 'views/logic/fieldtypes/FieldType';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import Pivot from 'views/logic/aggregationbuilder/Pivot';
import { usePlugin } from 'views/test/testPlugins';

import KeyMapperProvider from './KeyMapperProvider';
import KeyMapperContext from './KeyMapperContext';

// Asset-style binding that resolves titles ASYNCHRONOUSLY (after mount), mirroring the real
// getTitles-backed resolver. This reproduces the live behaviour where ids render first and the
// resolved names arrive on a later render.
const testPlugin = {
  exports: {
    visualizationKeyMappers: [
      {
        type: 'associated-assets',
        useKeyMapper: (keys: Array<string>) => {
          const [titles, setTitles] = useState<Record<string, string>>({});
          const key = useMemo(() => keys.join('|'), [keys]);

          useEffect(() => {
            setTitles(Object.fromEntries(keys.map((k) => [k, `Asset ${k}`])));
            // eslint-disable-next-line react-hooks/exhaustive-deps
          }, [key]);

          return useCallback((k: string) => titles[k] ?? k, [titles]);
        },
      },
    ],
  },
};

const fields = Immutable.List([new FieldTypeMapping('b', FieldType.create('associated-assets', []))]);

const Consumer = ({ k, field }: { k: string; field: string }) => {
  const mapKeys = useContext(KeyMapperContext);

  return <span>{mapKeys(k, field)}</span>;
};

describe('KeyMapperProvider (async resolution)', () => {
  usePlugin(testPlugin);

  it('updates labels once asynchronously-loaded titles arrive', async () => {
    const config = AggregationWidgetConfig.builder()
      .rowPivots([Pivot.createValues(['b'])])
      .columnPivots([])
      .build();
    const data = { chart: [{ source: 'leaf', key: ['b1'], values: [] }] } as any;

    render(
      <KeyMapperProvider data={data} config={config} fields={fields}>
        <Consumer k="b1" field="b" />
      </KeyMapperProvider>,
    );

    expect(await screen.findByText('Asset b1')).toBeInTheDocument();
  });
});
