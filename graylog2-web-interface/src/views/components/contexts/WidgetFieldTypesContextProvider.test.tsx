import * as React from 'react';
import { render, screen } from 'wrappedTestingLibrary';
import asMock from 'helpers/mocking/AsMock';

import useFieldTypes from 'views/logic/fieldtypes/useFieldTypes';
import WidgetFieldTypesContextProvider from 'views/components/contexts/WidgetFieldTypesContextProvider';
import WidgetContext from 'views/components/contexts/WidgetContext';
import Widget from 'views/logic/widgets/Widget';
import useCurrentQuery from 'views/logic/queries/useCurrentQuery';
import Query from 'views/logic/queries/Query';

import FieldTypesContext from './FieldTypesContext';

jest.mock('views/logic/fieldtypes/useFieldTypes');
jest.mock('views/logic/queries/useCurrentQuery');

describe('WidgetFieldTypesContextProvider', () => {
  it('retrieves field types based on streams and timerange of current widget', async () => {
    asMock(useFieldTypes).mockReturnValue({ data: [] } as ReturnType<typeof useFieldTypes>);
    asMock(useCurrentQuery).mockReturnValue(Query.builder().id('deadbeef').build());

    const widget = Widget.builder().streams(['stream1', 'stream2']).timerange({ type: 'relative', range: 3600 }).build();

    render((
      <WidgetContext.Provider value={widget}>

        <WidgetFieldTypesContextProvider>
          <FieldTypesContext.Consumer>
            {({ all }) => (
              <span>Got {all.size} field types</span>
            )}
          </FieldTypesContext.Consumer>
        </WidgetFieldTypesContextProvider>
      </WidgetContext.Provider>
    ));

    await screen.findByText('Got 0 field types');

    expect(useFieldTypes).toHaveBeenCalledWith(['stream1', 'stream2'], { type: 'relative', range: 3600 });
  });
});
