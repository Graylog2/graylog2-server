import React from 'react';
import { Button } from 'react-bootstrap';

import { ConfigurationForm } from 'components/configurationforms';

const EditAlarmCallbackButton = React.createClass({
  propTypes: {
    alarmCallback: React.PropTypes.object.isRequired,
    disabled: React.PropTypes.bool.isRequired,
    onUpdate: React.PropTypes.func.isRequired,
    types: React.PropTypes.object.isRequired,
  },
  getDefaultProps() {
    return {
      disabled: false,
    };
  },
  _handleClick() {
    this.refs.configurationForm.open();
  },
  _handleSubmit(data) {
    this.props.onUpdate(this.props.alarmCallback, data);
  },
  render() {
    const alarmCallback = this.props.alarmCallback;
    const definition = this.props.types[alarmCallback.type];
    const configurationForm = (definition ? <ConfigurationForm ref="configurationForm" key={'configuration-form-alarm-callback-' + alarmCallback.id}
                                                               configFields={definition.requested_configuration}
                                                               title={"Editing Alarm Callback "}
                                                               typeName={alarmCallback.type} includeTitleField={false}
                                                               submitAction={this._handleSubmit} values={alarmCallback.configuration} /> : null);

    return (
      <span>
        <Button bsStyle="success" disabled={this.props.disabled} onClick={this._handleClick}>
          Edit callback
        </Button>
        {configurationForm}
      </span>
    );
  },
});

export default EditAlarmCallbackButton;
