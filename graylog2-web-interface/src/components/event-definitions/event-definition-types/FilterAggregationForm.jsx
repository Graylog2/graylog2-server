import React from 'react';
import PropTypes from 'prop-types';
import lodash from 'lodash';
import { Col, ControlLabel, FormGroup, HelpBlock, Radio, Row } from 'react-bootstrap';

import FormsUtils from 'util/FormsUtils';
import FilterForm from './FilterForm';
import AggregationForm from './AggregationForm';

import commonStyles from '../common/commonStyles.css';

const conditionTypes = {
  AGGREGATION: 0,
  FILTER: 1,
};

const dataSources = {
  LOG_MESSAGES: 0,
  EVENTS: 1,
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

const initialConfig = {
  ...initialFilterConfig,
  ...initialAggregationConfig,
};

class FilterAggregationForm extends React.Component {
  static propTypes = {
    action: PropTypes.oneOf(['create', 'edit']).isRequired,
    eventDefinition: PropTypes.object.isRequired,
    allFieldTypes: PropTypes.array.isRequired,
    entityTypes: PropTypes.object.isRequired,
    streams: PropTypes.array.isRequired,
    onChange: PropTypes.func.isRequired,
  };

  constructor(props) {
    super(props);

    const defaultDataSource = props.action === 'edit' ? dataSources.LOG_MESSAGES : undefined;
    // eslint-disable-next-line camelcase
    const { group_by, series } = props.eventDefinition.config;
    const defaultConditionType = (lodash.isEmpty(group_by) && lodash.isEmpty(series)
      ? conditionTypes.FILTER : conditionTypes.AGGREGATION);

    this.state = {
      dataSource: defaultDataSource,
      conditionType: defaultConditionType,
    };
  }

  componentDidMount() {
    // Set initial config for this type
    const { eventDefinition } = this.props;
    const defaultConfig = Object.assign({}, initialConfig, eventDefinition.config);
    this.propagateChange('config', defaultConfig);
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

  renderDataSourceForm = (dataSource) => {
    const { allFieldTypes, entityTypes, eventDefinition, streams } = this.props;
    const { conditionType } = this.state;

    if (dataSource === dataSources.LOG_MESSAGES) {
      return (
        <React.Fragment>
          <Row>
            <Col md={7} lg={6}>
              <FilterForm eventDefinition={eventDefinition} streams={streams} onChange={this.propagateChange} />

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
          </Row>
          {conditionType === conditionTypes.AGGREGATION && (
            <Row>
              <Col md={12}>
                <AggregationForm eventDefinition={eventDefinition}
                                 allFieldTypes={allFieldTypes}
                                 aggregationFunctions={entityTypes.aggregation_functions}
                                 onChange={this.propagateChange} />
              </Col>
            </Row>
          )}
        </React.Fragment>
      );
    }

    return (<div>TBD</div>);
  };

  render() {
    const { dataSource } = this.state;

    return (
      <Row>
        <Col md={12}>
          <h2 className={commonStyles.title}>Select Data Source</h2>
          <fieldset>
            <FormGroup>
              <Radio id="data-source-log-messages"
                     name="dataSource"
                     value={dataSources.LOG_MESSAGES}
                     checked={dataSource === dataSources.LOG_MESSAGES}
                     onChange={this.handleTypeChange}>
                Log Messages
              </Radio>
              <HelpBlock>Select this option to create an Alert from Log Messages.</HelpBlock>
              <Radio id="data-source-events"
                     name="dataSource"
                     value={dataSources.EVENTS}
                     checked={dataSource === dataSources.EVENTS}
                     onChange={this.handleTypeChange}>
                Events
              </Radio>
              <HelpBlock>
                Select this option to create an Alert from Events created by other Event Definitions.
              </HelpBlock>
            </FormGroup>
          </fieldset>

          {dataSource !== undefined && this.renderDataSourceForm(dataSource)}
        </Col>
      </Row>
    );
  }
}

export default FilterAggregationForm;
