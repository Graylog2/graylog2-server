'use strict';

var React = require('react/addons');
var ConfigurationForm = require('../configurationforms/ConfigurationForm');
var AlarmCallbacksStore = require('../../stores/alarmcallbacks/AlarmCallbacksStore');

var CreateAlarmCallbackButton = React.createClass({
    getInitialState() {
        return {
            types: [],
            streamId: this.props.streamId,
            typeName: "",
            typeDefinition: {}
        };
    },
    componentWillReceiveProps(props) {
        this.setState(props);
    },
    componentDidMount() {
        this.loadData();
    },
    loadData() {
        AlarmCallbacksStore.available(this.state.streamId, (types) => {
            this.setState({types:types});
        });
    },
    render() {
        var alarmCallbackTypes = $.map(this.state.types, this._formatOutputType);
        var helpBlock = (<p className="help-block">{"Select a name of your new alarm callback that describes it."}</p>);
        return (
            <div className="form-inline">
                <div className="form-group">
                    <select id="input-type" defaultValue="placeholder" onChange={this.onTypeChange} className="form-control">
                        <option value="placeholder" disabled>--- Select Alarm Callback Type ---</option>
                        {alarmCallbackTypes}
                    </select>

                    <button className="btn btn-success btn-sm" onClick={this._openModal}>Configure new alert destination</button>

                    <ConfigurationForm ref="configurationForm" key="configuration-form-output" configFields={this.state.typeDefinition} title="Create new Alarm Callback"
                                       typeName={this.state.typeName}
                                       helpBlock={helpBlock} submitAction={this.handleSubmit}/>
                </div>
            </div>
        );
    },
    _openModal() {
        this.refs.configurationForm.open();
    },
    _formatOutputType(typeDefinition, typeName) {
        return (<option key={typeName} value={typeName}>{typeDefinition.name}</option>);
    },
    onTypeChange(evt) {
        var alarmCallbackType = evt.target.value;
        this.setState({typeName: alarmCallbackType});
        if (this.state.types[alarmCallbackType]) {
            this.setState({typeDefinition: this.state.types[alarmCallbackType].requested_configuration});
        } else {
            this.setState({typeDefinition: {}});
        }
    },
    handleSubmit(data) {
        AlarmCallbacksStore.save(this.state.streamId, data, (result) => {
            this.props.onUpdate();
        });
    }
});

module.exports = CreateAlarmCallbackButton;
