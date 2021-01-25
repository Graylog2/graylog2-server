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
