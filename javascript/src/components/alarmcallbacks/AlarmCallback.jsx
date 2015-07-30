'use strict';

var React = require('react');
var PermissionsMixin = require('../../util/PermissionsMixin');
var DeleteAlarmCallbackButton = require('./DeleteAlarmCallbackButton');
var ConfigurationWell = require('../configurationforms/ConfigurationWell');
var EditAlarmCallbackButton = require('./EditAlarmCallbackButton');
var Row = require('react-bootstrap').Row;
var Col = require('react-bootstrap').Col;
var Alert = require('react-bootstrap').Alert;
var Button = require('react-bootstrap').Button;

var AlarmCallback = React.createClass({
    mixins: [PermissionsMixin],
    getInitialState() {
        return {
            showConfiguration: false
        };
    },
    /* jshint -W116 */
    _typeNotAvailable() {
        return (this.props.types[this.props.alarmCallback.type] == undefined);
    },
    getDefaultProps() {
        return {
            hideButtons: false
        };
    },
    _toggleConfiguration() {
        this.setState({showConfiguration: !this.state.showConfiguration});
    },
    _formatActionButtons() {
        var alarmCallback = this.props.alarmCallback;
        var editAlarmCallbackButton = (this.isPermitted(this.props.permissions, ["streams:edit:"+this.props.streamId]) ?
            <EditAlarmCallbackButton disabled={this._typeNotAvailable()} alarmCallback={alarmCallback} types={this.props.types}
                                     streamId={this.props.streamId} onUpdate={this.props.updateAlarmCallback} /> : null);
        var deleteAlarmCallbackButton = (this.isPermitted(this.props.permissions, ["streams:edit:"+this.props.streamId]) ?
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
            return;
        }
        var toggleConfigurationText = (this.state.showConfiguration ? "Hide configuration" : "Show configuration");
        return <Button onClick={this._toggleConfiguration} bsSize="xsmall" bsStyle="link">{toggleConfigurationText}</Button>;
    },
    _renderConfiguration(alarmCallback) {
        if (this.props.concise && !this.state.showConfiguration) return;

        var alert = (this._typeNotAvailable() ? <Alert bsStyle="danger">
            The plugin required for this alarm callback is not loaded. Editing it is not possible. Please load the plugin or delete the alarm callback.
        </Alert> : null);
        var configurationWell = (this._typeNotAvailable() ? null :
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
        var alarmCallback = this.props.alarmCallback;
        var humanReadableType = (this._typeNotAvailable() ? <i>Type not available ({alarmCallback.type})</i> : this.props.types[alarmCallback.type].name);
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

                        {this.props.concise ? null : "Executed once per triggered alert condition."}
                    </Col>


                    {!this.props.concise &&
                    <Col md={3} style={{textAlign: "right"}}>
                        {this._formatActionButtons()}
                    </Col>
                    }
                </Row>

                {this._renderConfiguration(alarmCallback)}

                <hr />
            </div>
        );
    }
});

module.exports = AlarmCallback;
