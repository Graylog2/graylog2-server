import * as React from 'react';
import { Formik } from 'formik';
import * as Immutable from 'immutable';
import { render } from 'wrappedTestingLibrary';
import { PluginStore } from 'graylog-web-plugin/plugin';

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
      },
      {
        type: 'custom',
        displayName: 'Widget with Custom Export Settings',
        titleGenerator: () => 'Default Title',
        exportComponent: CustomExportComponent,
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
