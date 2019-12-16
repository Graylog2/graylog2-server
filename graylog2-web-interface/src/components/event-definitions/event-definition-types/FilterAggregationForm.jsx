import React from 'react';
import PropTypes from 'prop-types';
import lodash from 'lodash';
import { Col, ControlLabel, FormGroup, Radio, Row } from 'components/graylog';

import FormsUtils from 'util/FormsUtils';

import FilterForm from './FilterForm';
import FilterPreviewContainer from './FilterPreviewContainer';
import AggregationForm from './AggregationForm';

const conditionTypes = {
  AGGREGATION: 0,
  FILTER: 1,
};

const initialFilterConfig = {
  query: '',
  streams: [],
  search_within_ms: 60 * 1000,
  execute_every_ms: 60 * 1000,
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
    allFieldTypes: PropTypes.array.isRequired,
    entityTypes: PropTypes.object.isRequired,
    streams: PropTypes.array.isRequired,
    onChange: PropTypes.func.isRequired,
  };

  constructor(props) {
    super(props);

    // eslint-disable-next-line camelcase
    const { group_by, series, conditions } = props.eventDefinition.config;
    const expression = lodash.get(conditions, 'expression', {});
    const defaultConditionType = (lodash.isEmpty(group_by) && lodash.isEmpty(series) && lodash.isEmpty(expression)
      ? conditionTypes.FILTER : conditionTypes.AGGREGATION);

    this.state = {
      conditionType: defaultConditionType,
    };
  }

  propagateChange = (key, value) => {
    const { onChange } = this.props;
    onChange(key, value);
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

      const nextConfig = Object.assign({}, eventDefinition.config, initialAggregationConfig);
      this.propagateChange('config', nextConfig);
    } else {
      // Reset aggregation data from state if it exists
      const { existingAggregationConfig } = this.state;
      if (existingAggregationConfig) {
        const { eventDefinition } = this.props;
        const nextConfig = Object.assign({}, eventDefinition.config, existingAggregationConfig);
        this.propagateChange('config', nextConfig);
        stateChange.existingAggregationConfig = undefined;
      }
    }

    this.setState(stateChange);
  };

  handleChange = (event) => {
    const { name } = event.target;
    this.propagateChange(name, FormsUtils.getValueFromInput(event.target));
  };

  static defaultConfig = {
    ...initialFilterConfig,
    ...initialAggregationConfig,
  };

  render() {
    const { conditionType } = this.state;
    const { allFieldTypes, entityTypes, eventDefinition, streams, validation } = this.props;

    return (
      <React.Fragment>
        <Row>
          <Col md={7} lg={6}>
            <FilterForm eventDefinition={eventDefinition}
                        validation={validation}
                        streams={streams}
                        onChange={this.propagateChange} />

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
                               allFieldTypes={allFieldTypes}
                               aggregationFunctions={entityTypes.aggregation_functions}
                               onChange={this.propagateChange} />
            </Col>
          </Row>
        )}
      </React.Fragment>
    );
  }
}

export default FilterAggregationForm;
