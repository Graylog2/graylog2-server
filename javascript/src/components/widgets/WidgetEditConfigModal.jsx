/* global momentHelper */

'use strict';

var React = require('react');
var Input = require('react-bootstrap').Input;

var BootstrapModal = require('../bootstrap/BootstrapModal');

var WidgetEditConfigModal = React.createClass({
    getInitialState() {
        return {
            title: this.props.widget.title,
            type: this.props.widget.type,
            cacheTime: this.props.widget.cacheTime,
            config: this.props.widget.config,
            errors: {}
        };
    },
    open() {
        this.refs.editModal.open();
    },
    hide() {
        this.refs.editModal.close();
    },
    _getWidgetData() {
        var widget = {};
        var stateKeys = Object.keys(this.state);

        stateKeys.forEach((key) => {
            if (this.state.hasOwnProperty(key) && key !== "errors") {
                widget[key] = this.state[key];
            }
        });

        return widget;
    },
    save() {
        var errorKeys = Object.keys(this.state.errors);
        if (!errorKeys.some((key) => this.state.errors[key] === true)) {
            this.props.onUpdate(this._getWidgetData());
        }
        this.hide();
    },
    _onTitleChange(event) {
        this.setState({title: event.target.value});
    },
    _onCacheTimeChange(event) {
        this.setState({cacheTime: event.target.value});
    },
    _onConfigurationChange(key, value) {
        var config = this.state.config;
        config[key] = value;
        this.setState({config: config});
    },
    _onQueryChange(event) {
        this._onConfigurationChange("query", event.target.value);
    },
    _onConfigurationCheckboxChange(key) {
        return (event) => {
            this._onConfigurationChange(key, event.target.checked);
        };
    },
    _onRelativeTimeRangeChange(event) {
        this._onConfigurationChange("range", event.target.value);
    },
    _onAbsoluteTimeRangeFromChange(event) {
        var from = momentHelper.parseUserLocalFromString(event.target.value);
        var hasError = !from.isValid();

        var errors = this.state.errors;
        errors["from"] = hasError;
        this.setState({errors: errors});
        if (!hasError) {
            this._onConfigurationChange("from", from.tz("utc").format());
        }
    },
    _onAbsoluteTimeRangeToChange(event) {
        var to = momentHelper.parseUserLocalFromString(event.target.value);
        var hasError = !to.isValid();

        var errors = this.state.errors;
        errors["to"] = hasError;
        this.setState({errors: errors});
        if (!hasError) {
            this._onConfigurationChange("to", to.tz("utc").format());
        }
    },
    _onKeywordTimeRangeChange(event) {
        this._onConfigurationChange("keyword", event.target.value);
    },
    _formatDateTime(dateTime) {
        return momentHelper.toUserTimeZone(dateTime).format(momentHelper.DATE_FORMAT_NO_MS);
    },
    _getTimeRangeFormControls() {
        var rangeTypeSelector = (
            <Input type="text"
                   label="Time range type"
                   disabled
                   value={this.state.config.range_type.capitalize()}
                   help="Type of time range to use in the widget."/>
        );

        var rangeValueInput;

        switch (this.state.config.range_type) {
            case 'relative':
                rangeValueInput = (
                    <Input type="number"
                           label="Search relative time"
                           required
                           min="0"
                           defaultValue={this.state.config.range}
                           onChange={this._onRelativeTimeRangeChange}
                           help="Number of seconds relative to the moment the search executes. 0 searches in all messages."/>
                );
                break;
            case 'absolute':
                rangeValueInput = (
                    <div>
                        <Input type="text"
                               label="Search from"
                               required
                               bsStyle={this.state.errors["from"] === true ? "error" : null}
                               defaultValue={this._formatDateTime(this.state.config.from)}
                               onChange={this._onAbsoluteTimeRangeFromChange}
                               help="Earliest time to be included in the search. E.g. 2015-03-27 13:23:41"/>
                        <Input type="text"
                               label="Search to"
                               required
                               bsStyle={this.state.errors["to"] === true ? "error" : null}
                               defaultValue={this._formatDateTime(this.state.config.to)}
                               onChange={this._onAbsoluteTimeRangeToChange}
                               help="Latest time to be included in the search. E.g. 2015-03-27 13:23:41"/>
                    </div>
                );
                break;
            case 'keyword':
                rangeValueInput = (
                    <Input type="text"
                           label="Search keyword"
                           required
                           defaultValue={this.state.config.keyword}
                           onChange={this._onKeywordTimeRangeChange}
                           help="Search keyword representing the time to be included in the search. E.g. last day"/>
                );
                break;
        }

        return (
            <div>
                {rangeTypeSelector}
                {rangeValueInput}
            </div>
        );
    },
    _getSpecificConfigurationControls() {
        var controls = [];

        if (this.state.config.hasOwnProperty("trend")) {
            controls.push(
                <Input key="trend"
                       type="checkbox"
                       label="Display trend"
                       defaultChecked={this.state.config.trend}
                       onChange={this._onConfigurationCheckboxChange("trend")}
                       help="Show trend information for this number."/>
            );

            controls.push(
                <Input key="lowerIsBetter"
                       type="checkbox"
                       label="Lower is better"
                       disabled={this.state.config.trend === false}
                       defaultChecked={this.state.config.lower_is_better}
                       onChange={this._onConfigurationCheckboxChange("lower_is_better")}
                       help="Use green colour when trend goes down."/>
            );
        }

        if (this.state.config.hasOwnProperty("show_pie_chart")) {
            controls.push(
                <Input key="showPieChart"
                       type="checkbox"
                       label="Show pie chart"
                       defaultChecked={this.state.config.show_pie_chart}
                       onChange={this._onConfigurationCheckboxChange("show_pie_chart")}
                       help="Represent data in a pie chart"/>
            );
        }

        return controls;
    },
    render() {
        var configModalHeader = <h2 className="modal-title">Edit widget "{this.state.title}"</h2>;
        var configModalBody = (
            <fieldset>
                <Input type="text"
                       label="Title"
                       required
                       defaultValue={this.state.title}
                       onChange={this._onTitleChange}
                       help="Select a name of your new input that describes it."/>
                <Input type="number"
                       min="1"
                       required
                       label="Cache time"
                       defaultValue={this.state.cacheTime}
                       onChange={this._onCacheTimeChange}
                       help="Number of seconds the widget value will be cached."/>
                <Input type="text"
                       label="Search query"
                       defaultValue={this.state.config.query}
                       onChange={this._onQueryChange}
                       help="Search query that will be executed to get the widget value."/>
                {this._getTimeRangeFormControls()}
                {this._getSpecificConfigurationControls()}
            </fieldset>
        );

        return (
            <BootstrapModal ref="editModal"
                            onCancel={this.hide}
                            onConfirm={this.save}
                            onHidden={this.props.onModalHidden}
                            cancel="Cancel"
                            confirm="Update">
                {configModalHeader}
                {configModalBody}
            </BootstrapModal>
        );
    }
});

module.exports = WidgetEditConfigModal;