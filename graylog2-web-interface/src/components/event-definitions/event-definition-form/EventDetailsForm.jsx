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
import lodash from 'lodash';

import { Col, ControlLabel, FormGroup, HelpBlock, Row } from 'components/graylog';
import { Select } from 'components/common';
import { Input } from 'components/bootstrap';
import EventDefinitionPriorityEnum from 'logic/alerts/EventDefinitionPriorityEnum';
import * as FormsUtils from 'util/FormsUtils';

import commonStyles from '../common/commonStyles.css';

const priorityOptions = lodash.map(EventDefinitionPriorityEnum.properties, (value, key) => ({ value: key, label: lodash.upperFirst(value.name) }));

class EventDetailsForm extends React.Component {
  static propTypes = {
    eventDefinition: PropTypes.object.isRequired,
    validation: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
  };

  handleChange = (event) => {
    const { name } = event.target;
    const { onChange } = this.props;

    onChange(name, FormsUtils.getValueFromInput(event.target));
  };

  handlePriorityChange = (nextPriority) => {
    const { onChange } = this.props;

    onChange('priority', lodash.toNumber(nextPriority));
  };

  render() {
    const { eventDefinition, validation } = this.props;

    return (
      <Row>
        <Col md={7} lg={6}>
          <h2 className={commonStyles.title}>Event Details</h2>
          <fieldset>
            <Input id="event-definition-title"
                   name="title"
                   label="Title"
                   type="text"
                   bsStyle={validation.errors.title ? 'error' : null}
                   help={lodash.get(validation, 'errors.title[0]', 'Title for this Event Definition, Events and Alerts created from it.')}
                   value={eventDefinition.title}
                   onChange={this.handleChange}
                   required />

            <Input id="event-definition-description"
                   name="description"
                   label={<span>Description <small className="text-muted">(Optional)</small></span>}
                   type="textarea"
                   help="Longer description for this Event Definition."
                   value={eventDefinition.description}
                   onChange={this.handleChange}
                   rows={2} />

            <FormGroup controlId="event-definition-priority">
              <ControlLabel>Priority</ControlLabel>
              <Select options={priorityOptions}
                      value={lodash.toString(eventDefinition.priority)}
                      onChange={this.handlePriorityChange}
                      clearable={false}
                      required />
              <HelpBlock>Choose the priority for Events created from this Definition.</HelpBlock>
            </FormGroup>
          </fieldset>
        </Col>
      </Row>
    );
  }
}

export default EventDetailsForm;
