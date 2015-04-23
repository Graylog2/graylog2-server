'use strict';

var React = require('react');
var Input = require('react-bootstrap').Input;
var Immutable = require('immutable');

var BootstrapModal = require('../bootstrap/BootstrapModal');
var Widget = require('./Widget');

var WidgetCreationModal = React.createClass({
    getInitialState() {
        return {
            title: this._getDefaultWidgetTitle(),
            configuration: Immutable.Map()
        };
    },
    open() {
        this.refs.createModal.open();
    },
    hide() {
        this.refs.createModal.close();
    },
    getConfiguration() {
        var configuration = Immutable.Map({title: this.state.title});
        return configuration.concat(this.state.configuration);
    },
    save(e) {
        e.preventDefault();
        var configuration = this.getConfiguration();
        this.props.onConfigurationSaved(configuration);
    },
    _getDefaultWidgetTitle() {
        var title = "";

        switch (this.props.widgetType) {
            case Widget.Type.SEARCH_RESULT_COUNT:
            case Widget.Type.STREAM_SEARCH_RESULT_COUNT:
                title = "messages";
                break;
            case Widget.Type.STATS_COUNT:
            case Widget.Type.QUICKVALUES:
            case Widget.Type.FIELD_CHART:
                break;
            case Widget.Type.SEARCH_RESULT_CHART:
                title = "search histogram";
                break;
            default:
                throw("Unsupported widget type " + this.props.widgetType);
        }

        return title;
    },
    _getSpecificWidgetInputs() {
        var controls = [];

        switch (this.props.widgetType) {
            case Widget.Type.SEARCH_RESULT_COUNT:
            case Widget.Type.STREAM_SEARCH_RESULT_COUNT:
            case Widget.Type.STATS_COUNT:
                if (this.props.supportsTrending) {
                    controls.push(
                        <Input key="trend"
                               type="checkbox"
                               ref="trend"
                               label="Display trend"
                               defaultChecked={false}
                               onChange={() => this._onConfigurationCheckboxChange("trend")}
                               help="Show trend information for this number."/>
                    );
                    controls.push(
                        <Input key="lowerIsBetter"
                               type="checkbox"
                               ref="lower_is_better"
                               label="Lower is better"
                               disabled={!this.state.configuration.get('trend', false)}
                               defaultChecked={false}
                               onChange={() => this._onConfigurationCheckboxChange("lower_is_better")}
                               help="Use green colour when trend goes down."/>
                    );
                }
                break;
            case Widget.Type.SEARCH_RESULT_CHART:
            case Widget.Type.FIELD_CHART:
                break;
            case Widget.Type.QUICKVALUES:
                controls.push(
                    <Input key="showPieChart"
                           type="checkbox"
                           ref="show_pie_chart"
                           label="Show pie chart"
                           defaultChecked={false}
                           onChange={() => this._onConfigurationCheckboxChange("show_pie_chart")}
                           help="Include a pie chart representation of the data"/>
                );
                break;
            default:
                throw("Unsupported widget type " + this.props.widgetType);
        }

        return controls;
    },
    _onTitleChange() {
        this.setState({title: this.refs.title.getValue()});
    },
    _onConfigurationChange(key, value) {
        var newConfiguration = this.state.configuration.set(key, value);
        this.setState({configuration: newConfiguration});
    },
    _onConfigurationCheckboxChange(ref) {
        this._onConfigurationChange(ref, this.refs[ref].getChecked());
    },
    render() {
        var configModalHeader = <h2 className="modal-title">Create Dashboard Widget</h2>;
        var configModalBody = (
            <fieldset>
                <Input type="text"
                       label="Title"
                       ref="title"
                       required
                       defaultValue={this.state.title}
                       onChange={this._onTitleChange}
                       help="Type a name that describes your widget."/>
                {this._getSpecificWidgetInputs()}
            </fieldset>
        );

        return (
            <BootstrapModal ref="createModal"
                            onCancel={this.hide}
                            onConfirm={this.save}
                            onHidden={this.props.onModalHidden}
                            cancel="Cancel"
                            confirm="Create">
                {configModalHeader}
                {configModalBody}
            </BootstrapModal>
        );
    }
});

module.exports = WidgetCreationModal;