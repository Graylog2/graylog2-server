import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';

import TemplateFieldValueProviderForm from './TemplateFieldValueProviderForm';
import TemplateFieldValueProviderSummary from './TemplateFieldValueProviderSummary';
import LookupTableFieldValueProviderForm from './LookupTableFieldValueProviderForm';
import LookupTableFieldValueProviderFormContainer from './LookupTableFieldValueProviderFormContainer';
import LookupTableFieldValueProviderSummary from './LookupTableFieldValueProviderSummary';

PluginStore.register(new PluginManifest({}, {
  fieldValueProviders: [
    {
      type: TemplateFieldValueProviderForm.type,
      displayName: 'Template',
      formComponent: TemplateFieldValueProviderForm,
      summaryComponent: TemplateFieldValueProviderSummary,
      defaultConfig: TemplateFieldValueProviderForm.defaultConfig,
      requiredFields: TemplateFieldValueProviderForm.requiredFields,
    },
    {
      type: LookupTableFieldValueProviderForm.type,
      displayName: 'Lookup Table',
      formComponent: LookupTableFieldValueProviderFormContainer,
      summaryComponent: LookupTableFieldValueProviderSummary,
      defaultConfig: LookupTableFieldValueProviderForm.defaultConfig,
      requiredFields: LookupTableFieldValueProviderForm.requiredFields,
    },
  ],
}));
