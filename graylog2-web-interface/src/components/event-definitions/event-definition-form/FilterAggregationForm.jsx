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

class FilterAggregationForm extends React.Component {
  static propTypes = {
    eventDefinition: PropTypes.object.isRequired,
    streams: PropTypes.array.isRequired,
    onChange: PropTypes.func.isRequired,
  };

  state = {
    conditionType: undefined,
  };

  componentDidMount() {
    const { eventDefinition } = this.props;
    const config = lodash.cloneDeep(eventDefinition.config);
    config.type = 'aggregation-v1';
    this.propagateChange('config', config);
  }

  isAggregationSelected = () => {
    const { eventDefinition } = this.props;
    const { conditionType } = this.state;

    if (conditionType === conditionTypes.AGGREGATION) {
      return true;
    }

    // eslint-disable-next-line camelcase
    const { group_by, conditions, series } = eventDefinition.config;
    return conditionType === undefined
      && (!lodash.isEmpty(group_by) || !lodash.isEmpty(conditions) || !lodash.isEmpty(series));
  };

  handleConditionTypeChange = (event) => {
    const selectedConditionType = Number(FormsUtils.getValueFromInput(event.target));
    this.setState({ conditionType: selectedConditionType });
  };

  handleSubmit = (event) => {
    event.preventDefault();
  };

  propagateChange = (key, value) => {
    const { onChange } = this.props;
    onChange(key, value);
  };

  handleChange = (event) => {
    const { name } = event.target;
    this.propagateChange(name, FormsUtils.getValueFromInput(event.target));
  };

  renderDataSourceForm = (dataSource) => {
    const { eventDefinition, streams } = this.props;
    const { conditionType } = this.state;
    const isFilterSelected = conditionType === conditionTypes.FILTER;
    const isAggregationSelected = this.isAggregationSelected();

    if (dataSource === 'log-messages') {
      return (
        <React.Fragment>
          <Row>
            <Col md={12} lg={7}>
              <FilterForm eventDefinition={eventDefinition} streams={streams} onChange={this.propagateChange} />

              <FormGroup>
                <ControlLabel>Create Events for Alert if...</ControlLabel>
                <Radio id="filter-type"
                       name="type"
                       value={conditionTypes.FILTER}
                       checked={isFilterSelected}
                       onChange={this.handleConditionTypeChange}>
                  Filter has results
                </Radio>
                <Radio id="aggregation-type"
                       name="type"
                       value={conditionTypes.AGGREGATION}
                       checked={isAggregationSelected}
                       onChange={this.handleConditionTypeChange}>
                  Aggregation of results reaches a threshold
                </Radio>
              </FormGroup>
            </Col>
          </Row>
          {isAggregationSelected && (
            <Row>
              <Col md={12}>
                <AggregationForm eventDefinition={eventDefinition} onChange={this.propagateChange} />
              </Col>
            </Row>
          )}
        </React.Fragment>
      );
    }

    return (<div>TBD</div>);
  };

  render() {
    const { eventDefinition } = this.props;

    return (
      <Row>
        <Col md={12}>
          <h2 className={commonStyles.title}>Select Data Source</h2>
          <form onSubmit={this.handleSubmit}>
            <fieldset>
              <FormGroup>
                <Radio id="data-source-log-messages"
                       name="data_source"
                       value="log-messages"
                       checked={eventDefinition.data_source === 'log-messages'}
                       onChange={this.handleChange}>
                  Log Messages
                </Radio>
                <HelpBlock>Select this option to create an Alert from Log Messages.</HelpBlock>
                <Radio id="data-source-events"
                       name="data_source"
                       value="events"
                       checked={eventDefinition.data_source === 'events'}
                       onChange={this.handleChange}>
                  Events
                </Radio>
                <HelpBlock>Select this option to create an Alert from Events created by other Event Definitions.</HelpBlock>
              </FormGroup>
            </fieldset>

            {eventDefinition.data_source && this.renderDataSourceForm(eventDefinition.data_source)}
          </form>
        </Col>
      </Row>
    );
  }
}

export default FilterAggregationForm;
