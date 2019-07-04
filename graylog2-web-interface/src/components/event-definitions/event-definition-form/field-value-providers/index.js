import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';

import TemplateFieldValueProviderForm from './TemplateFieldValueProviderForm';
import TemplateFieldValueProviderSummary from './TemplateFieldValueProviderSummary';

PluginStore.register(new PluginManifest({}, {
  fieldValueProviders: [
    {
      type: 'template-v1',
      displayName: 'Template',
      formComponent: TemplateFieldValueProviderForm,
      summaryComponent: TemplateFieldValueProviderSummary,
    },
  ],
}));
