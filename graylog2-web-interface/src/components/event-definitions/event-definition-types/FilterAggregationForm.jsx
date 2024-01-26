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
import React from 'react';
import PropTypes from 'prop-types';
import get from 'lodash/get';
import isEmpty from 'lodash/isEmpty';
import cloneDeep from 'lodash/cloneDeep';

import { Col, ControlLabel, FormGroup, Input, Radio, Row } from 'components/bootstrap';
import * as FormsUtils from 'util/FormsUtils';

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
  filters: [],
  search_within_ms: 5 * 60 * 1000,
  execute_every_ms: 5 * 60 * 1000,
  _is_scheduled: true,
  event_limit: 100,
};

const initialAggregationConfig = {
  group_by: [],
  series: [],
  conditions: {},
};

class FilterAggregationForm extends React.Component {
  static propTypes = {
    eventDefinition: PropTypes.object.isRequired,
    validation: PropTypes.object.isRequired,
    entityTypes: PropTypes.object.isRequired,
    streams: PropTypes.array.isRequired,
    onChange: PropTypes.func.isRequired,
    currentUser: PropTypes.object.isRequired,
  };

  static defaultConfig = {
    ...initialFilterConfig,
    ...initialAggregationConfig,
  };

  constructor(props) {
    super(props);

    const { group_by, series, conditions } = props.eventDefinition.config;
    const expression = get(conditions, 'expression', {});
    const defaultConditionType = (isEmpty(group_by) && isEmpty(series) && isEmpty(expression)
      ? conditionTypes.FILTER : conditionTypes.AGGREGATION);

    this.state = {
      conditionType: defaultConditionType,
    };
  }

  handleConfigChange = (event) => {
    const config = cloneDeep(this.props.eventDefinition.config);

    config[event.target.name] = FormsUtils.getValueFromInput(event.target);
    this.propagateConfigChange(config);
  };

  propagateConfigChange = (config) => {
    this.props.onChange('config', config);
  };

  handleTypeChange = (event) => {
    const stateChange = {};
    const nextConditionType = Number(FormsUtils.getValueFromInput(event.target));

    stateChange[event.target.name] = nextConditionType;

    if (nextConditionType === conditionTypes.FILTER) {
      const { eventDefinition } = this.props;

      // Store existing data temporarily in state, to restore it in case the type change was accidental
      const existingAggregationConfig = {};

      Object.keys(initialAggregationConfig).forEach((key) => {
        existingAggregationConfig[key] = eventDefinition.config[key];
      });

      stateChange.existingAggregationConfig = existingAggregationConfig;

      const nextConfig = { ...eventDefinition.config, ...initialAggregationConfig };

      this.propagateConfigChange(nextConfig);
    } else {
      // Reset aggregation data from state if it exists
      const { existingAggregationConfig } = this.state;

      if (existingAggregationConfig) {
        const { eventDefinition } = this.props;
        const nextConfig = { ...eventDefinition.config, ...existingAggregationConfig };

        this.propagateConfigChange(nextConfig);
        stateChange.existingAggregationConfig = undefined;
      }
    }

    this.setState(stateChange);
  };

  render() {
    const { conditionType } = this.state;
    const { entityTypes, eventDefinition, streams, validation, currentUser } = this.props;

    return (
      <>
        <Row>
          <Col md={7} lg={6}>
            <FilterForm eventDefinition={eventDefinition}
                        validation={validation}
                        streams={streams.filter((s) => s.is_editable)}
                        currentUser={currentUser}
                        onChange={this.props.onChange} />

            <FormGroup>
              <ControlLabel>Create Events for Definition if...</ControlLabel>
              <Radio id="filter-type"
                     name="conditionType"
                     value={conditionTypes.FILTER}
                     checked={conditionType === conditionTypes.FILTER}
                     onChange={this.handleTypeChange}>
                Filter has results
              </Radio>
              <Radio id="aggregation-type"
                     name="conditionType"
                     value={conditionTypes.AGGREGATION}
                     checked={conditionType === conditionTypes.AGGREGATION}
                     onChange={this.handleTypeChange}>
                Aggregation of results reaches a threshold
              </Radio>
            </FormGroup>
            {conditionType === conditionTypes.FILTER && (
              <Row>
                <Col md={12}>
                  <Input id="event-limit"
                         name="event_limit"
                         label="Event Limit"
                         type="number"
                         bsStyle={validation.errors.event_limit ? 'error' : null}
                         help={get(validation, 'errors.event_limit', 'Maximum number of events to be created.')}
                         value={eventDefinition.config.event_limit}
                         onChange={this.handleConfigChange}
                         required />
                </Col>
              </Row>
            )}

          </Col>
          <Col md={5} lgOffset={1}>
            <FilterPreviewContainer eventDefinition={eventDefinition} />
          </Col>
        </Row>
        {conditionType === conditionTypes.AGGREGATION && (
          <Row>
            <Col md={12}>
              <AggregationForm eventDefinition={eventDefinition}
                               validation={validation}
                               aggregationFunctions={entityTypes.aggregation_functions}
                               onChange={this.props.onChange} />
            </Col>
          </Row>
        )}
      </>
    );
  }
}

export default FilterAggregationForm;
