'use strict';

var React = require('react/addons');
var PermissionsMixin = require('../../util/PermissionsMixin');
var DeleteAlarmCallbackButton = require('./DeleteAlarmCallbackButton');
var ConfigurationWell = require('../configurationforms/ConfigurationWell');
var EditAlarmCallbackButton = require('./EditAlarmCallbackButton');
var Row = require('react-bootstrap').Row;
var Col = require('react-bootstrap').Col;
var Alert = require('react-bootstrap').Alert;

var AlarmCallback = React.createClass({
    mixins: [PermissionsMixin],
    /* jshint -W116 */
    _typeNotAvailable() {
        return (this.props.types[this.props.alarmCallback.type] == undefined);
    },
    render() {
        var alarmCallback = this.props.alarmCallback;
        var humanReadableType = (this._typeNotAvailable() ? <i>Unknown alarm callback</i> : this.props.types[alarmCallback.type].name);
        var editAlarmCallbackButton = (this.isPermitted(this.props.permissions, ["streams:edit:"+this.props.streamId]) ?
            <EditAlarmCallbackButton disabled={this._typeNotAvailable()} alarmCallback={alarmCallback} types={this.props.types}
                                     streamId={this.props.streamId} onUpdate={this.props.updateAlarmCallback} /> : null);
        var deleteAlarmCallbackButton = (this.isPermitted(this.props.permissions, ["streams:edit:"+this.props.streamId]) ?
            <DeleteAlarmCallbackButton alarmCallback={alarmCallback} onClick={this.props.deleteAlarmCallback} /> : null);
        var alert = (this._typeNotAvailable() ? <Alert bsStyle="danger">
                The plugin required for this alarm callback is not loaded. Editing it is not possible. Please load the plugin or delete the alarm callback.
            </Alert> : null);
        return (
            <div className="alert-callback" data-destination-id={alarmCallback.id}>
                <Row style={{marginBottom: 0}}>
                    <Col md={9}>
                        <h3>
                            {' '}
                            <span>{humanReadableType}</span>
                        </h3>

                        Executed once per triggered alert condition.
                    </Col>

                    <Col md={3} style={{textAlign: "right"}}>
                        {' '}
                        {editAlarmCallbackButton}
                        {' '}
                        {deleteAlarmCallbackButton}
                    </Col>
                </Row>

                <Row style={{marginBottom: 0}}>
                    <Col md={12}>
                        {alert}
                        <ConfigurationWell configuration={alarmCallback.configuration}/>
                    </Col>
                </Row>

                <hr />
            </div>
        );
    }
});

module.exports = AlarmCallback;
