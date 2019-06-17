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

class FilterAggregationForm extends React.Component {
  static propTypes = {
    action: PropTypes.oneOf(['create', 'edit']).isRequired,
    eventDefinition: PropTypes.object.isRequired,
    streams: PropTypes.array.isRequired,
    onChange: PropTypes.func.isRequired,
  };

  constructor(props) {
    super(props);

    const defaultDataSource = props.action === 'edit' ? dataSources.LOG_MESSAGES : undefined;
    // eslint-disable-next-line camelcase
    const { group_by, conditions, series } = props.eventDefinition.config;
    let defaultConditionType;
    if (props.action === 'edit') {
      defaultConditionType = (lodash.isEmpty(group_by) && lodash.isEmpty(conditions) && lodash.isEmpty(series)
        ? conditionTypes.FILTER : conditionTypes.AGGREGATION);
    }

    this.state = {
      dataSource: defaultDataSource,
      conditionType: defaultConditionType,
    };
  }

  componentDidMount() {
    const { eventDefinition } = this.props;
    const config = lodash.cloneDeep(eventDefinition.config);
    config.type = 'aggregation-v1';
    this.propagateChange('config', config);
  }

  handleTypeChange = (event) => {
    const stateChange = {};
    stateChange[event.target.name] = Number(FormsUtils.getValueFromInput(event.target));
    this.setState(stateChange);
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

    if (dataSource === dataSources.LOG_MESSAGES) {
      return (
        <React.Fragment>
          <Row>
            <Col md={12} lg={7}>
              <FilterForm eventDefinition={eventDefinition} streams={streams} onChange={this.propagateChange} />

              <FormGroup>
                <ControlLabel>Create Events for Alert if...</ControlLabel>
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
    const { dataSource } = this.state;

    return (
      <Row>
        <Col md={12}>
          <h2 className={commonStyles.title}>Select Data Source</h2>
          <form onSubmit={this.handleSubmit}>
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
                <HelpBlock>Select this option to create an Alert from Events created by other Event Definitions.</HelpBlock>
              </FormGroup>
            </fieldset>

            {dataSource !== undefined && this.renderDataSourceForm(dataSource)}
          </form>
        </Col>
      </Row>
    );
  }
}

export default FilterAggregationForm;
