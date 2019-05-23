import React from 'react';
import PropTypes from 'prop-types';
import lodash from 'lodash';
import { Col, ControlLabel, FormGroup, HelpBlock, Row } from 'react-bootstrap';

import { Select } from 'components/common';
import { Input } from 'components/bootstrap';

import AlertDefinitionPriorityEnum from 'logic/alerts/AlertDefinitionPriorityEnum';
import FormsUtils from 'util/FormsUtils';

import commonStyles from '../common/commonStyles.css';

const priorityOptions = lodash.map(AlertDefinitionPriorityEnum.properties, (value, key) => ({ value: key, label: lodash.upperFirst(value.name) }));

class AlertDetailsForm extends React.Component {
  static propTypes = {
    alertDefinition: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
  };

  handleSubmit = (event) => {
    event.preventDefault();
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
    const { alertDefinition } = this.props;

    return (
      <Row>
        <Col md={7}>
          <h2 className={commonStyles.title}>Alert Details</h2>
          <form onSubmit={this.handleSubmit}>
            <fieldset>
              <Input id="alert-definition-title"
                     name="title"
                     label="Title"
                     type="text"
                     help="Title for this Alert Definition, Events and Alerts created from it."
                     value={alertDefinition.title}
                     onChange={this.handleChange}
                     required />

              <Input id="alert-definition-description"
                     name="description"
                     label={<span>Description <small className="text-muted">(Optional)</small></span>}
                     type="textarea"
                     help="Longer description for this Alert Definition."
                     value={alertDefinition.description}
                     onChange={this.handleChange}
                     rows={2} />

              <FormGroup controlId="alert-definition-priority">
                <ControlLabel>Priority</ControlLabel>
                <Select options={priorityOptions}
                        value={lodash.toString(alertDefinition.priority)}
                        onChange={this.handlePriorityChange}
                        clearable={false}
                        required />
                <HelpBlock>Choose the priority for Alerts created from this Definition.</HelpBlock>
              </FormGroup>
            </fieldset>
          </form>
        </Col>
      </Row>
    );
  }
}

export default AlertDetailsForm;
