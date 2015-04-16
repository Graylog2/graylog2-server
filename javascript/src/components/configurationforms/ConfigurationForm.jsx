'use strict';

var $ = require('jquery'); // excluded and shimed

var React = require('react/addons');
var BootstrapModal = require('../bootstrap/BootstrapModal');
var TextField = require('./TextField');
var NumberField = require('./NumberField');

var ConfigurationForm = React.createClass({
    getInitialState() {
        return {
            configFields: this.props.configFields,
            title: this.props.title,
            typeName: this.props.typeName,
            elementName: this.props.elementName,
            formTarget: this.props.formTarget,
            formId: this.props.formId
        };
    },
    componentDidMount() {},
	componentWillReceiveProps(props) {
		this.setState(props);
	},
    render() {
        var typeName = this.state.typeName;
        var configFields = $.map(this.state.configFields, this._renderConfigField);
        var title = this.state.title;
        return (
            <form action={this.state.formTarget} method="POST">
                <div id={this.state.formId} className="configuration-form modal fade" data-inputtype={typeName} tabIndex="-1" role="dialog" aria-hidden="false">
                    <div className="modal-dialog">
                        <div className="modal-content">
                            <div className="modal-header">
                                <button type="button" className="close" data-dismiss="modal" aria-label="Close">
                                    <span aria-hidden="true">&times;</span>
                                </button>
                                <h2 className="modal-title">
                                    <i className="fa fa-signin"></i>
                                    {title}
                                </h2>
                            </div>

                            <div className="modal-body">
                                <input type="hidden" name="type" value={typeName} />

                                <label htmlFor={"title-" + typeName}>Title</label>
                                <input id={"title-" + typeName} name="title" required="true" type="text" className="input-xlarge form-control" />
                                    <p className="help-block">{"Select a name of your new " + this.state.elementName + " that describes it."}</p>
                                    {configFields}
                            </div>
                            <div className="modal-footer">
                                <button type="button" className="btn" data-dismiss="modal">Close</button>
                                <button type="submit" className="btn btn-primary launch-input" data-type={typeName}>
                                    <i className="fa fa-rocket"></i>
                                    Launch
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            </form>
        );
    },
    _renderConfigField(configField, key) {
        switch(configField.type) {
            case "text":
                return (<TextField key={this.state.typeName + "-" + key} typeName={this.state.typeName} title={key} field={configField} />);
            case "number":
                return (<NumberField key={this.state.typeName + "-" + key} typeName={this.state.typeName} title={key} field={configField} />);
            case "boolean":
                return "";
            case "dropdown":
                return "";
        }
    }
});

module.exports = ConfigurationForm;
