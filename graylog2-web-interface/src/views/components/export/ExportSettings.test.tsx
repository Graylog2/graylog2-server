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
import { Formik } from 'formik';
import * as Immutable from 'immutable';
import { render } from 'wrappedTestingLibrary';

import Widget from 'views/logic/widgets/Widget';
import View from 'views/logic/views/View';
import MessagesWidget from 'views/logic/widgets/MessagesWidget';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import FieldType from 'views/logic/fieldtypes/FieldType';
import TestStoreProvider from 'views/test/TestStoreProvider';
import type { FieldTypes } from 'views/components/contexts/FieldTypesContext';
import { asMock } from 'helpers/mocking';
import useActiveQueryId from 'views/hooks/useActiveQueryId';
import FieldTypesContext from 'views/components/contexts/FieldTypesContext';
import useViewsPlugin from 'views/test/testViewsPlugin';
import { usePlugin } from 'views/test/testPlugins';

import { viewWithoutWidget, stateWithOneWidget } from './Fixtures';
import ExportSettings from './ExportSettings';

jest.mock('views/hooks/useActiveQueryId');

const CustomExportComponent = () => <>This is a custom export component</>;

const pluginExports = {
  exports: {
    enterpriseWidgets: [
      {
        type: 'messages',
        displayName: 'Message List',
        titleGenerator: () => MessagesWidget.defaultTitle,
        searchTypes: () => [],
        needsControlledHeight: () => false,
        editComponent: () => <>Hey!</>,
        visualizationComponent: () => <>Hey!</>,
      },
      {
        type: 'custom',
        displayName: 'Widget with Custom Export Settings',
        titleGenerator: () => 'Default Title',
        exportComponent: CustomExportComponent,
        searchTypes: () => [],
        needsControlledHeight: () => false,
        editComponent: () => <>Hey!</>,
        visualizationComponent: () => <>Hey!</>,
      },
    ],
  },
};

const fields = Immutable.List([FieldTypeMapping.create('foo', FieldType.create('long'))]);
const fieldTypes: FieldTypes = {
  all: fields,
  queryFields: Immutable.Map({ 'view-query-id': fields }),
};

const SimpleExportSettings = (props: Omit<React.ComponentProps<typeof ExportSettings>, 'fields'>) => (
  <TestStoreProvider>
    <FieldTypesContext.Provider value={fieldTypes}>
      <Formik initialValues={{ selectedFields: [] }} onSubmit={() => {}}>
        {() => (
          <ExportSettings {...props} />
        )}
      </Formik>
    </FieldTypesContext.Provider>
  </TestStoreProvider>
);
const customWidget = Widget.builder()
  .id('widget-id-1')
  .type('custom')
  .build();
const view = viewWithoutWidget(View.Type.Search).toBuilder()
  .state(Immutable.Map({ 'query-id-1': stateWithOneWidget(customWidget) }))
  .build();

describe('ExportSettings', () => {
  useViewsPlugin();
  usePlugin(pluginExports);

  beforeEach(() => {
    asMock(useActiveQueryId).mockReturnValue('view-query-id');
  });

  it('renders custom export settings component', async () => {
    const { findByText } = render(<SimpleExportSettings view={view} selectedWidget={customWidget} />);

    await findByText('This is a custom export component');
  });
});
