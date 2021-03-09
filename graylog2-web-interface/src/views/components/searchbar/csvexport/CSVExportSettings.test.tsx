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
import { PluginExports, PluginStore } from 'graylog-web-plugin/plugin';
import { WidgetExport } from 'views/types';

import Widget from 'views/logic/widgets/Widget';
import View from 'views/logic/views/View';
import CSVExportSettings from 'views/components/searchbar/csvexport/CSVExportSettings';
import MessagesWidget from 'views/logic/widgets/MessagesWidget';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import FieldType from 'views/logic/fieldtypes/FieldType';

import { viewWithoutWidget, stateWithOneWidget } from './Fixtures';

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

const SimpleExportSettings = (props: Omit<React.ComponentProps<typeof CSVExportSettings>, 'fields'>) => (
  <Formik initialValues={{ selectedFields: [] }} onSubmit={() => {}}>
    {() => (
      <CSVExportSettings fields={fields} {...props} />
    )}
  </Formik>
);
const customWidget = Widget.builder()
  .id('widget-id-1')
  .type('custom')
  .build();
const view = viewWithoutWidget(View.Type.Search).toBuilder()
  .state(Immutable.Map({ 'query-id-1': stateWithOneWidget(customWidget) }))
  .build();

describe('CSVExportSettings', () => {
  beforeAll(() => {
    PluginStore.register(pluginExports);
  });

  afterAll(() => {
    PluginStore.unregister(pluginExports);
  });

  it('renders custom export settings component', async () => {
    const { findByText } = render(<SimpleExportSettings view={view} selectedWidget={customWidget} />);

    await findByText('This is a custom export component');
  });
});
