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
import { render, screen } from 'wrappedTestingLibrary';

import asMock from 'helpers/mocking/AsMock';
import useFieldTypes from 'views/logic/fieldtypes/useFieldTypes';
import WidgetFieldTypesContextProvider from 'views/components/contexts/WidgetFieldTypesContextProvider';
import WidgetContext from 'views/components/contexts/WidgetContext';
import Widget from 'views/logic/widgets/Widget';
import useCurrentQuery from 'views/logic/queries/useCurrentQuery';
import Query from 'views/logic/queries/Query';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import { FieldTypes } from 'views/logic/fieldtypes/FieldType';

import FieldTypesContext from './FieldTypesContext';

jest.mock('views/logic/fieldtypes/useFieldTypes', () => jest.fn());
jest.mock('views/logic/queries/useCurrentQuery');

describe('WidgetFieldTypesContextProvider', () => {
  it('retrieves field types based on streams and timerange of current widget', async () => {
    const data: FieldTypeMapping[] = [FieldTypeMapping.create('foo', FieldTypes.LONG())];
    asMock(useFieldTypes).mockReturnValue({ data } as ReturnType<typeof useFieldTypes>);
    asMock(useCurrentQuery).mockReturnValue(Query.builder().id('deadbeef').build());

    const widget = Widget.builder().streams(['stream1', 'stream2']).timerange({ type: 'relative', range: 3600 }).build();

    render((
      <WidgetContext.Provider value={widget}>

        <WidgetFieldTypesContextProvider>
          <FieldTypesContext.Consumer>
            {({ all, queryFields }) => (
              <>
                <span>Got {all.size} field type(s)</span>
                <span>and {queryFields.get('deadbeef').size} field type(s) for query</span>
              </>
            )}
          </FieldTypesContext.Consumer>
        </WidgetFieldTypesContextProvider>
      </WidgetContext.Provider>
    ));

    await screen.findByText('Got 1 field type(s)');
    await screen.findByText('and 1 field type(s) for query');

    expect(useFieldTypes).toHaveBeenCalledWith(['stream1', 'stream2'], { type: 'relative', range: 3600 });
  });
});
