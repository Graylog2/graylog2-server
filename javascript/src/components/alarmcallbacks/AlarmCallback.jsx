import React from 'react';
import { Row, Col, Alert, Button } from 'react-bootstrap';

import PermissionsMixin from '../../util/PermissionsMixin';
import DeleteAlarmCallbackButton from 'components/alarmcallbacks/DeleteAlarmCallbackButton';
import ConfigurationWell from 'components/configurationforms/ConfigurationWell';
import EditAlarmCallbackButton from 'components/alarmcallbacks/EditAlarmCallbackButton';

const AlarmCallback = React.createClass({
  propTypes: {
    alarmCallback: React.PropTypes.object.isRequired,
    concise: React.PropTypes.bool.isRequired,
    deleteAlarmCallback: React.PropTypes.func.isRequired,
    permissions: React.PropTypes.arrayOf(React.PropTypes.string).isRequired,
    streamId: React.PropTypes.string.isRequired,
    subtitle: React.PropTypes.string.isRequired,
    titleAnnotation: React.PropTypes.string.isRequired,
    types: React.PropTypes.object.isRequired,
    updateAlarmCallback: React.PropTypes.func.isRequired,
  },
  mixins: [PermissionsMixin],
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
    const editAlarmCallbackButton = (this.isPermitted(this.props.permissions, ['streams:edit:' + this.props.streamId]) ?
      <EditAlarmCallbackButton disabled={this._typeNotAvailable()} alarmCallback={alarmCallback} types={this.props.types}
                               streamId={this.props.streamId} onUpdate={this.props.updateAlarmCallback} /> : null);
    const deleteAlarmCallbackButton = (this.isPermitted(this.props.permissions, ['streams:edit:' + this.props.streamId]) ?
      <DeleteAlarmCallbackButton alarmCallback={alarmCallback} onClick={this.props.deleteAlarmCallback} /> : null);
    return (
      <span>
                {' '}
        {editAlarmCallbackButton}
        {' '}
        {deleteAlarmCallbackButton}
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

        <hr />
      </div>
    );
  },
});

export default AlarmCallback;
