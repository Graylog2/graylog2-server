import React from 'react';
import { Row, Col, Alert, Button } from 'react-bootstrap';

import { IfPermitted } from 'components/common';

import { DeleteAlarmCallbackButton, EditAlarmCallbackButton } from 'components/alarmcallbacks';
import ConfigurationWell from 'components/configurationforms/ConfigurationWell';

const AlarmCallback = React.createClass({
  propTypes: {
    alarmCallback: React.PropTypes.object.isRequired,
    concise: React.PropTypes.bool,
    deleteAlarmCallback: React.PropTypes.func.isRequired,
    streamId: React.PropTypes.string.isRequired,
    subtitle: React.PropTypes.string,
    titleAnnotation: React.PropTypes.string,
    types: React.PropTypes.object.isRequired,
    updateAlarmCallback: React.PropTypes.func.isRequired,
  },
  getDefaultProps() {
    return {
      hideButtons: false,
    };
  },
  getInitialState() {
    return {
      showConfiguration: false,
    };
  },
  /* jshint -W116 */
  _typeNotAvailable() {
    return (this.props.types[this.props.alarmCallback.type] === undefined);
  },
  _toggleConfiguration() {
    this.setState({showConfiguration: !this.state.showConfiguration});
  },
  _formatActionButtons() {
    const alarmCallback = this.props.alarmCallback;
    return (
      <span>
        {' '}
        <IfPermitted permissions={'streams:edit:' + this.props.streamId}>
          <EditAlarmCallbackButton disabled={this._typeNotAvailable()} alarmCallback={alarmCallback} types={this.props.types}
                                   streamId={this.props.streamId} onUpdate={this.props.updateAlarmCallback} />
        </IfPermitted>
        {' '}
        <IfPermitted permissions={'streams:edit:' + this.props.streamId}>
          <DeleteAlarmCallbackButton alarmCallback={alarmCallback} onClick={this.props.deleteAlarmCallback} />
        </IfPermitted>
      </span>
    );
  },
  _renderToggleConfigurationLink() {
    if (!this.props.concise) {
      return null;
    }
    const toggleConfigurationText = (this.state.showConfiguration ? 'Hide configuration' : 'Show configuration');
    return <Button onClick={this._toggleConfiguration} bsSize="xsmall" bsStyle="link">{toggleConfigurationText}</Button>;
  },
  _renderConfiguration(alarmCallback) {
    if (this.props.concise && !this.state.showConfiguration) return null;

    const alert = (this._typeNotAvailable() ? <Alert bsStyle="danger">
      The plugin required for this alarm callback is not loaded. Editing it is not possible. Please load the plugin or delete the alarm callback.
    </Alert> : null);
    const configurationWell = (this._typeNotAvailable() ? null :
      <ConfigurationWell configuration={alarmCallback.configuration}
                         typeDefinition={this.props.types[alarmCallback.type]} />);
    return (
      <Row style={{marginBottom: 0}}>
        <Col md={12}>
          {alert}
          {configurationWell}
        </Col>
      </Row>
    );
  },
  render() {
    const alarmCallback = this.props.alarmCallback;
    const humanReadableType = (this._typeNotAvailable() ? <i>Type not available ({alarmCallback.type})</i> : this.props.types[alarmCallback.type].name);
    return (
      <div className="alert-callback" data-destination-id={alarmCallback.id}>
        <Row style={{marginBottom: 0}}>
          <Col md={9}>
            <h3>
              {' '}
              <span>{humanReadableType}</span> <small>{this.props.titleAnnotation}</small>
              <span> {this._renderToggleConfigurationLink()} </span>
              <small>{this.props.subtitle}</small>
            </h3>

            {this.props.concise ? null : 'Executed once per triggered alert condition.'}
          </Col>


          {!this.props.concise &&
          <Col md={3} style={{textAlign: 'right'}}>
            {this._formatActionButtons()}
          </Col>
            }
        </Row>

        {this._renderConfiguration(alarmCallback)}
      </div>
    );
  },
});

export default AlarmCallback;
