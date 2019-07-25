import React from 'react';
import PropTypes from 'prop-types';
import { Col, ControlLabel, FormGroup, HelpBlock, Row } from 'react-bootstrap';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { Select } from 'components/common';

import commonStyles from '../common/commonStyles.css';

class EventConditionForm extends React.Component {
  static propTypes = {
    eventDefinition: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
  };

  getConditionPlugin = (type) => {
    if (type === undefined) {
      return {};
    }
    return PluginStore.exports('eventDefinitionTypes').find(edt => edt.type === type) || {};
  };

  formattedEventDefinitionTypes = () => {
    return PluginStore.exports('eventDefinitionTypes')
      .map(type => ({ label: type.displayName, value: type.type }));
  };

  handleEventDefinitionTypeChange = (nextType) => {
    const { onChange } = this.props;
    const conditionPlugin = this.getConditionPlugin(nextType);
    const defaultConfig = conditionPlugin.defaultConfig || {};
    onChange('config', { ...defaultConfig, type: nextType });
  };

  render() {
    const { eventDefinition } = this.props;
    const eventDefinitionType = this.getConditionPlugin(eventDefinition.config.type);

    const eventDefinitionTypeComponent = eventDefinitionType.formComponent
      ? React.createElement(eventDefinitionType.formComponent, {
        ...this.props,
        key: eventDefinition.id,
      })
      : null;

    return (
      <Row>
        <Col md={7} lg={6}>
          <h2 className={commonStyles.title}>Event Condition</h2>
          <FormGroup controlId="event-definition-priority">
            <ControlLabel>Condition Type</ControlLabel>
            <Select placeholder="Select a Condition Type"
                    options={this.formattedEventDefinitionTypes()}
                    value={eventDefinition.config.type}
                    onChange={this.handleEventDefinitionTypeChange}
                    clearable={false}
                    required />
            <HelpBlock>Choose the type of Condition the Event will use.</HelpBlock>
          </FormGroup>
        </Col>

        <Col md={12}>
          {eventDefinitionTypeComponent}
        </Col>
      </Row>
    );
  }
}

export default EventConditionForm;
