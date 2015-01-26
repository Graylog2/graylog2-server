'use strict';

var React = require('react/addons');
//noinspection JSUnusedGlobalSymbols
var BootstrapModal = require('../bootstrap/BootstrapModal');

var EditDashboardModal = React.createClass({
    getInitialState() {
        return {
            id: this.props.id,
            name: this.props.name,
            pattern: this.props.pattern
        };
    },
    _onPatternChange(event) {
        this.setState({pattern: event.target.value});
    },
    _onNameChange(event) {
        this.setState({name: event.target.value});
    },
    render() {
        var header = <h2>{this.props.create ? "Create" : "Edit"} Grok Pattern {this.state.name}</h2>;
        var body = (
            <fieldset>
                <label>Name:</label>
                <input type="text" onChange={this._onNameChange} value={this.state.name} required/>
                <label>Pattern:</label>
                <input type="text" onChange={this._onPatternChange} value={this.state.pattern} required/>
            </fieldset>
        );
        return (
            <span>
                <button onClick={this.openModal} className="btn btn-small btn-success">
                    <i className="icon-edit"></i> {this.props.create ? "Create pattern" : "Edit"}
                </button>
                <BootstrapModal ref="modal" onCancel={this._closeModal} onConfirm={this._save} cancel="Cancel" confirm="Save">
                   {header}
                   {body}
                </BootstrapModal>
            </span>
        );
    },
    _closeModal() {
        this.refs.modal.close();
    },
    openModal() {
        this.refs.modal.open();
    },
    _save() {
        var pattern = this.state;
        this.props.savePattern(pattern, this._closeModal);
    }
});

module.exports = EditDashboardModal;
