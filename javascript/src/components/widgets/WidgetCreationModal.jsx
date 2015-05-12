'use strict';

var React = require('react');
var Input = require('react-bootstrap').Input;
var Immutable = require('immutable');

var BootstrapModal = require('../bootstrap/BootstrapModal');
var Widget = require('./Widget');

var FieldStatisticsStore = require('../../stores/field-analyzers/FieldStatisticsStore');

var StringUtils = require('../../util/StringUtils');

var WidgetCreationModal = React.createClass({
    getInitialState() {
        this.initialConfiguration = Immutable.Map();
        return {
            title: this._getDefaultWidgetTitle(),
            configuration: this.initialConfiguration
        };
    },
    // We need to set the default configuration in case some inputs are never changed
    _setInitialConfiguration() {
        var initialConfiguration = this.initialConfiguration;
        if (this.refs['inputFieldset'] !== undefined) {
            var fieldsetChildren = this.refs['inputFieldset'].props.children;
            React.Children.forEach(fieldsetChildren, (child) => {
                // We only care about children with refs, as all inputs have one
                if (child.ref !== undefined && child.ref !== 'title') {
                    var input = this.refs[child.ref];
                    var value;
                    if (input.props.type === 'checkbox') {
                        value = input.getChecked();
                    } else {
                        value = input.getValue();
                    }

                    if (value !== undefined) {
                        initialConfiguration = initialConfiguration.set(child.ref, value);
                    }
                }
            }, this);
        }
        this.initialConfiguration = initialConfiguration;
    },
    open() {
        this.refs.createModal.open();
    },
    hide() {
        this.refs.createModal.close();
    },
    getEffectiveConfiguration() {
        return this.initialConfiguration.merge(Immutable.Map(this.state.configuration));
    },
    save(e) {
        var configuration = this.getEffectiveConfiguration();
        this.props.onConfigurationSaved(this.state.title, configuration);
    },
    saved() {
        this.setState(this.getInitialState());
        this.hide();
    },
    _getDefaultWidgetTitle() {
        var title = "";

        switch (this.props.widgetType) {
            case Widget.Type.SEARCH_RESULT_COUNT:
            case Widget.Type.STREAM_SEARCH_RESULT_COUNT:
                title = "message count";
                break;
            case Widget.Type.STATS_COUNT:
                title = "field statistical value";
                break;
            case Widget.Type.QUICKVALUES:
                if (this.props.configuration['field'] !== undefined) {
                    title = this.props.configuration['field'] + " quick values";
                } else {
                    title = "field quick values";
                }
                break;
            case Widget.Type.FIELD_CHART:
                if (this.props.configuration['field'] !== undefined && this.props.configuration['valuetype'] !== undefined) {
                    title = this.props.configuration['field'] + " " + this.props.configuration['valuetype'] + " value graph";
                } else {
                    title = "field graph";
                }
                break;
            case Widget.Type.SEARCH_RESULT_CHART:
                title = "search histogram";
                break;
            default:
                throw("Unsupported widget type " + this.props.widgetType);
        }

        return StringUtils.capitalizeFirstLetter(title);
    },
    _getSpecificWidgetInputs() {
        var controls = [];

        switch (this.props.widgetType) {
            case Widget.Type.SEARCH_RESULT_COUNT:
            case Widget.Type.STREAM_SEARCH_RESULT_COUNT:
            case Widget.Type.STATS_COUNT:
                if (this.props.widgetType === Widget.Type.STATS_COUNT) {
                    controls.push(
                        <Input key="field"
                               type="select"
                               ref="field"
                               label="Field name"
                               help="Select the field name you want to use in the widget."
                               onChange={() => this._onConfigurationInputChange('field')}>
                            {this.props.fields
                                .sort((a, b) => a.toLowerCase().localeCompare(b.toLowerCase()))
                                .map((field) => {
                                    return <option key={field} value={field}>{field}</option>;
                                })
                            }
                        </Input>
                    );
                    controls.push(
                        <Input key="statsFunction"
                               type="select"
                               ref="statsFunction"
                               label="Statistical function"
                               help="Select the statistical function to use in the widget."
                               onChange={() => this._onConfigurationInputChange('statsFunction')}>
                            {FieldStatisticsStore.FUNCTIONS.keySeq().map((statFunction) => {
                                return (
                                    <option key={statFunction} value={statFunction}>
                                        {FieldStatisticsStore.FUNCTIONS.get(statFunction)}
                                    </option>
                                );
                            })}
                        </Input>
                    );
                }

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
                           help="Include a pie chart representation of the data."/>
                );
                controls.push(
                    <Input key="showDataTable"
                           type="checkbox"
                           ref="show_data_table"
                           label="Show data table"
                           defaultChecked={true}
                           onChange={() => this._onConfigurationCheckboxChange("show_data_table")}
                           help="Include a table with quantitative information."/>
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
    _onConfigurationInputChange(ref) {
        this._onConfigurationChange(ref, this.refs[ref].getValue());
    },
    render() {
        var configModalHeader = <h2 className="modal-title">Create Dashboard Widget</h2>;
        var configModalBody = (
            <fieldset ref="inputFieldset">
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
                            onShown={this._setInitialConfiguration}
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