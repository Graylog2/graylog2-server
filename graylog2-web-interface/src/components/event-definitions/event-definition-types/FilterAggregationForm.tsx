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
import { useCallback, useState } from 'react';
import get from 'lodash/get';
import isEmpty from 'lodash/isEmpty';
import cloneDeep from 'lodash/cloneDeep';

import { Col, ControlLabel, FormGroup, Input, Radio, Row } from 'components/bootstrap';
import * as FormsUtils from 'util/FormsUtils';
import type { EventDefinition } from 'components/event-definitions/event-definitions-types';
import type { Stream } from 'views/stores/StreamsStore';
import type User from 'logic/users/User';
import type { EventDefinitionValidation } from 'components/event-definitions/types';

import FilterForm from './FilterForm';
import FilterPreviewContainer from './FilterPreviewContainer';
import AggregationForm from './AggregationForm';

const conditionTypes = {
  AGGREGATION: 0,
  FILTER: 1,
};

const initialFilterConfig = {
  query: '',
  query_parameters: [],
  streams: [],
  stream_categories: [],
  filters: [],
  search_within_ms: 5 * 60 * 1000,
  execute_every_ms: 5 * 60 * 1000,
  _is_scheduled: true,
  event_limit: 100,
  use_cron_scheduling: false,
};

const initialAggregationConfig = {
  group_by: [],
  series: [],
  conditions: {},
};

type Props = {
  eventDefinition: EventDefinition,
  onChange: (key: string, value: any) => void,
  entityTypes: {
    aggregation_functions: Array<{}>
  },
  streams: Array<Stream>,
  currentUser: User,
  validation: EventDefinitionValidation,
};

const FilterAggregationForm = ({ entityTypes, eventDefinition, streams, validation, currentUser, onChange }: Props) => {
  const { group_by, series, conditions } = eventDefinition.config;
  const expression = conditions?.expression ?? {};
  const [conditionType, setConditionType] = useState((isEmpty(group_by) && isEmpty(series) && isEmpty(expression))
    ? conditionTypes.FILTER
    : conditionTypes.AGGREGATION);
  const [existingAggregationConfig, setExistingAggregationConfig] = useState<EventDefinition['config'] | undefined>();

  const propagateConfigChange = useCallback((config: EventDefinition['config']) => {
    onChange('config', config);
  }, [onChange]);
  const handleConfigChange = useCallback((event: React.ChangeEvent<HTMLInputElement>) => {
    const config = cloneDeep(eventDefinition.config);

    config[event.target.name] = FormsUtils.getValueFromInput(event.target);
    propagateConfigChange(config);
  }, [propagateConfigChange, eventDefinition.config]);

  const handleTypeChange = useCallback((event: React.FormEvent<Radio>) => {
    const nextConditionType = Number(FormsUtils.getValueFromInput(event.target));

    setConditionType(nextConditionType);
    let newExistingAggregationConfig;

    if (nextConditionType === conditionTypes.FILTER) {
      // Store existing data temporarily in state, to restore it in case the type change was accidental
      Object.keys(initialAggregationConfig).forEach((key) => {
        newExistingAggregationConfig[key] = eventDefinition.config[key];
      });

      const nextConfig = { ...eventDefinition.config, ...initialAggregationConfig };

      propagateConfigChange(nextConfig as EventDefinition['config']);
    } else
      // Reset aggregation data from state if it exists
      if (existingAggregationConfig) {
        const nextConfig = { ...eventDefinition.config, ...existingAggregationConfig };

        propagateConfigChange(nextConfig);
        newExistingAggregationConfig = undefined;
      }

    setExistingAggregationConfig(newExistingAggregationConfig);
  }, [eventDefinition.config, existingAggregationConfig, propagateConfigChange]);

  const onlyFilters = eventDefinition._scope === 'ILLUMINATE';

  return (
    <>
      <Row>
        <Col md={7} lg={6}>
          <FilterForm eventDefinition={eventDefinition}
                      validation={validation}
                      streams={streams.filter((s) => s.is_editable)}
                      currentUser={currentUser}
                      onChange={onChange} />

          {onlyFilters || (
          <FormGroup>
            <ControlLabel>Create Events for Definition if...</ControlLabel>
            <Radio id="filter-type"
                   name="conditionType"
                   value={conditionTypes.FILTER}
                   checked={conditionType === conditionTypes.FILTER}
                   onChange={handleTypeChange}>
              Filter has results
            </Radio>
            <Radio id="aggregation-type"
                   name="conditionType"
                   value={conditionTypes.AGGREGATION}
                   checked={conditionType === conditionTypes.AGGREGATION}
                   onChange={handleTypeChange}>
              Aggregation of results reaches a threshold
            </Radio>
          </FormGroup>
          )}
          {(conditionType === conditionTypes.FILTER && !onlyFilters) && (
          <Row>
            <Col md={12}>
              <Input id="event-limit"
                     name="event_limit"
                     label="Event Limit"
                     type="number"
                     bsStyle={validation.errors.event_limit ? 'error' : null}
                     help={get(validation,
                       'errors.event_limit',
                       'Maximum number of events to be created per execution of this event definition. Excess events will be suppressed.') as string}
                     value={eventDefinition.config.event_limit}
                     onChange={handleConfigChange}
                     required />
            </Col>
          </Row>
          )}
        </Col>
        <Col md={5} lgOffset={1}>
          <FilterPreviewContainer eventDefinition={eventDefinition} />
        </Col>
      </Row>
      {(conditionType === conditionTypes.AGGREGATION && !onlyFilters) && (
      <Row>
        <Col md={12}>
          <AggregationForm eventDefinition={eventDefinition}
                           validation={validation}
                           aggregationFunctions={entityTypes.aggregation_functions}
                           onChange={onChange} />
        </Col>
      </Row>
      )}
    </>
  );
};

FilterAggregationForm.defaultConfig = {
  ...initialFilterConfig,
  ...initialAggregationConfig,
} as EventDefinition['config'];

export default FilterAggregationForm;
