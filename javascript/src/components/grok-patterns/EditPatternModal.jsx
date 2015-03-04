'use strict';

var React = require('react/addons');
//noinspection JSUnusedGlobalSymbols
var BootstrapModal = require('../bootstrap/BootstrapModal');

var EditDashboardModal = React.createClass({
    getInitialState() {
        return {
            id: this.props.id,
            name: this.props.name,
            pattern: this.props.pattern,
            _error: false,
            _error_message: ""
        };
    },
    _onPatternChange(event) {
        this.setState({pattern: event.target.value});
    },
    _onNameChange(event) {
        var name = event.target.value;

        if (! this.props.validPatternName(name)) {
            this.setState({name: name, _error: true, _error_message: "Pattern with that name already exists!"});
        } else {
            this.setState({name: name, _error: false, _error_message: ""});
        }
    },
    render() {
        var header = <h2>{this.props.create ? "Create" : "Edit"} Grok Pattern {this.state.name}</h2>;
        var body = (
            <fieldset>
                <div className={this.state._error ? "control-group error" : "control-group"}>
                    <label>Name:</label>
                    <div className="controls">
                        <input type="text" onChange={this._onNameChange} value={this.state.name} required/>
                        <span className="help-inline">{this.state._error_message}</span>
                    </div>
                </div>
                <label>Pattern:</label>
                <textarea onChange={this._onPatternChange} value={this.state.pattern} required></textarea>
            </fieldset>
        );
        return (
            <span>
                <button onClick={this.openModal} className={this.props.create ? "btn btn-small btn-success" : "btn btn-small"}>
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
    _saved() {
        this._closeModal();
        if (this.props.create) {
            this.setState({name: "", pattern: ""});
        }
    },
    _save() {
        var pattern = this.state;

        if (! pattern._error) {
            this.props.savePattern(pattern, this._saved);
        }
    }
});

module.exports = EditDashboardModal;
