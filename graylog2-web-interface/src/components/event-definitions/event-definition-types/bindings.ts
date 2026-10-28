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

import type { PluginExports } from 'graylog-web-plugin/plugin';

import i18n from 'i18n';
import LookupTableParameter from 'views/logic/parameters/LookupTableParameter';
import LookupTableQueryParameterEdit from 'components/lookup-table-parameters/LookupTableQueryParameterEdit';
import { SYSTEM_EVENT_DEFINITION_TYPE } from 'components/event-definitions/constants';

import FilterAggregationFormContainer from './FilterAggregationFormContainer';
import FilterAggregationForm from './FilterAggregationForm';
import FilterAggregationSummary from './FilterAggregationSummary';

const bindings: PluginExports = {
  eventDefinitionQueryParameterTypes: [
    {
      type: LookupTableParameter.type,
      title: 'Lookup Table',
      fromJSON: LookupTableParameter.fromJSON.bind(LookupTableParameter),
      validate: (param: any) => ({
        lookupTable: !param.lookupTable ? i18n.t('validation.cannot_be_empty', { ns: 'event_definitions' }) : undefined,
        key: !param.key ? i18n.t('validation.cannot_be_empty', { ns: 'event_definitions' }) : undefined,
      }),
      editComponent: LookupTableQueryParameterEdit,
    },
  ],
  eventDefinitionTypes: [
    {
      type: 'aggregation-v1',
      displayName: 'Filter & Aggregation',
      displayNameKey: 'event_definitions:types.aggregation.title',
      sortOrder: 0, // Sort before conditions working on events
      description:
        'Create Events from log messages by filtering them and (optionally) ' +
        'aggregating their results to match a given condition. These Events can be used as input for a Correlation Rule.',
      descriptionKey: 'event_definitions:types.aggregation.description',
      formComponent: FilterAggregationFormContainer,
      summaryComponent: FilterAggregationSummary,
      defaultConfig: FilterAggregationForm.defaultConfig,
      useCondition: () => true,
    },
    {
      type: SYSTEM_EVENT_DEFINITION_TYPE,
      displayName: 'System Event Definition',
      displayNameKey: 'event_definitions:types.system.title',
      sortOrder: 6,
      description: 'Built-in Event Definition Graylog uses to report system notifications as Events.',
      descriptionKey: 'event_definitions:types.system.description',
      defaultConfig: { type: SYSTEM_EVENT_DEFINITION_TYPE },
      // Only ever created by Graylog itself, never through the wizard, so it has no
      // form/summary component and is hidden from the creation dropdown. It's registered here
      // purely so it can be offered as an Event Definition Type filter option.
      useCondition: () => true,
      hideFromCreation: true,
    },
  ],
};

export default bindings;
