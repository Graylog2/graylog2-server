import React from 'react';
import PropTypes from 'prop-types';
import lodash from 'lodash';
import { Col, ControlLabel, FormGroup, HelpBlock, Radio, Row } from 'react-bootstrap';

import FormsUtils from 'util/FormsUtils';
import FilterForm from './FilterForm';

import commonStyles from '../common/commonStyles.css';

class AlertDetailsForm extends React.Component {
  static propTypes = {
    alertDefinition: PropTypes.object.isRequired,
    streams: PropTypes.array.isRequired,
    onChange: PropTypes.func.isRequired,
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

  handleConfigChange = (event) => {
    const { alertDefinition } = this.props;
    const { name } = event.target;
    const config = lodash.cloneDeep(alertDefinition.config);
    config[name] = FormsUtils.getValueFromInput(event.target);
    this.propagateChange('config', config);
  };

  renderDataSourceForm = (dataSource) => {
    const { alertDefinition, streams } = this.props;

    if (dataSource === 'log-messages') {
      return (
        <Row>
          <Col md={12} lg={7}>
            <h2 className={commonStyles.title}>Filter</h2>
            <FilterForm alertDefinition={alertDefinition} streams={streams} onChange={this.propagateChange} />

            <FormGroup>
              <ControlLabel>Create Events for Alert if...</ControlLabel>
              <Radio id="filter-type"
                     name="type"
                     value="filter-v1"
                     checked={alertDefinition.config.type === 'filter-v1'}
                     onChange={this.handleConfigChange}>
                Filter has results
              </Radio>
              <Radio id="aggregation-type"
                     name="type"
                     value="aggregation-v1"
                     checked={alertDefinition.config.type === 'aggregation-v1'}
                     onChange={this.handleConfigChange}>
                Aggregation of results reaches a threshold
              </Radio>
            </FormGroup>
          </Col>
        </Row>
      );
    }

    return (<div>TBD</div>);
  };

  render() {
    const { alertDefinition } = this.props;

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
                       checked={alertDefinition.data_source === 'log-messages'}
                       onChange={this.handleChange}>
                  Log Messages
                </Radio>
                <HelpBlock>Select this option to create an Alert from Log Messages.</HelpBlock>
                <Radio id="data-source-events"
                       name="data_source"
                       value="events"
                       checked={alertDefinition.data_source === 'events'}
                       onChange={this.handleChange}>
                  Events
                </Radio>
                <HelpBlock>Select this option to create an Alert from Events created by other Alert Definitions.</HelpBlock>
              </FormGroup>
            </fieldset>

            {alertDefinition.data_source && this.renderDataSourceForm(alertDefinition.data_source)}
          </form>
        </Col>
      </Row>
    );
  }
}

export default AlertDetailsForm;
