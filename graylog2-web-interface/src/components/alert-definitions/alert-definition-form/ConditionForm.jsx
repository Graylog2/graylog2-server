import React from 'react';
import PropTypes from 'prop-types';
import lodash from 'lodash';
import { Col, ControlLabel, FormGroup, HelpBlock, Radio, Row } from 'react-bootstrap';

import { MultiSelect } from 'components/common';
import { Input } from 'components/bootstrap';
import FormsUtils from 'util/FormsUtils';

import styles from './AlertDefinitionForm.css';

const conditionOptions = [
  { value: 'filter-v1', label: 'Filter' },
  { value: 'aggregation-v1', label: 'Aggregation' },
  { value: 'correlation-v1', label: 'Correlation' },
];

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

  handleStreamsChange = (nextValue) => {
    const { alertDefinition } = this.props;
    const config = lodash.cloneDeep(alertDefinition.config);
    config.selected_streams = nextValue;
    this.propagateChange('config', config);
  };

  renderDataSourceForm = (dataSource) => {
    const { alertDefinition, streams } = this.props;

    if (!dataSource) {
      return null;
    }

    if (dataSource === 'log-messages') {
      return (
        <Row>
          <Col md={12}>
            <h2 className={styles.title}>Filter</h2>
            <fieldset>
              <Input id="filter-query"
                     name="query"
                     label="Search Query"
                     type="text"
                     help="Search query that Messages should match. You can use the same syntax as in the Search page."
                     value={lodash.defaultTo(alertDefinition.config.query, '')}
                     onChange={this.handleConfigChange} />

              <FormGroup>
                <ControlLabel>Streams <small className="text-muted">(Optional)</small></ControlLabel>
                <MultiSelect id="filter-streams"
                             matchProp="label"
                             onChange={selected => this.handleStreamsChange(selected === '' ? [] : selected.split(','))}
                             options={streams.map(stream => ({ label: stream.title, value: stream.id }))}
                             value={lodash.defaultTo(alertDefinition.config.selected_streams, []).join(',')} />
                <HelpBlock>Select streams the search should include. Searches in all streams if empty.</HelpBlock>
              </FormGroup>

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
            </fieldset>
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
        <Col md={7}>
          <h2 className={styles.title}>Select Data Source</h2>
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

            {this.renderDataSourceForm(alertDefinition.data_source)}
          </form>
        </Col>
      </Row>
    );
  }
}

export default AlertDetailsForm;
