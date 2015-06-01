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
            error: false,
            error_message: ""
        };
    },
    _onPatternChange(event) {
        this.setState({pattern: event.target.value});
    },
    _onNameChange(event) {
        var name = event.target.value;

        if (! this.props.validPatternName(name)) {
            this.setState({name: name, error: true, error_message: "Pattern with that name already exists!"});
        } else {
            this.setState({name: name, error: false, error_message: ""});
        }
    },
    _getId(prefixIdName) {
        return this.state.name !== undefined ? prefixIdName + this.state.name : prefixIdName;
    },
    render() {
        var header = <h2 className="modal-title">{this.props.create ? "Create" : "Edit"} Grok Pattern {this.state.name}</h2>;
        var body = (
            <fieldset>
                <div className={this.state.error ? "form-group has-error" : "form-group"}>
                    <label htmlFor={this._getId("pattern-name")}>Name:</label>
                    <input type="text"
                        className="form-control"
                        onChange={this._onNameChange}
                        value={this.state.name}
                        id={this._getId("pattern-name")}
                        required/>
                    <span className="help-block">{this.state.error_message}</span>
                </div>
                <label htmlFor={this._getId("pattern")}>Pattern:</label>
                <textarea id={this._getId("pattern")}
                          className="form-control"
                          onChange={this._onPatternChange}
                          value={this.state.pattern}
                          required></textarea>
            </fieldset>
        );
        var triggerButtonContent;
        if (this.props.create) {
            triggerButtonContent = "Create pattern";
        } else {
            triggerButtonContent = <span>Edit</span>;
        }
        return (
            <span>
                <button onClick={this.openModal} className={this.props.create ? "btn btn-success" : "btn btn-info btn-xs"}>
                    {triggerButtonContent}
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

        if (! pattern.error) {
            this.props.savePattern(pattern, this._saved);
        }
    }
});

module.exports = EditDashboardModal;
