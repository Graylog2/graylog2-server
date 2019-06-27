import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';

import TemplateFieldValueProviderForm from './TemplateFieldValueProviderForm';

PluginStore.register(new PluginManifest({}, {
  fieldValueProviders: [
    {
      type: 'template-v1',
      displayName: 'Template',
      formComponent: TemplateFieldValueProviderForm,
    },
  ],
}));
