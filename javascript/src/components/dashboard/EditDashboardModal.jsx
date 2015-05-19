'use strict';

var $ = require('jquery'); // excluded and shimed

var React = require('react/addons');
var BootstrapModal = require('../bootstrap/BootstrapModal');
var DashboardStore = require('../../stores/dashboard/DashboardStore');

var EditDashboardModal = React.createClass({
    getInitialState() {
        return {
            id: this.props.id,
            description: this.props.description,
            title: this.props.title
        };
    },
    getDefaultProps() {
        return {
            action: 'create'
        };
    },
    _onDescriptionChange(event) {
        this.setState({description: event.target.value});
    },
    _onTitleChange(event) {
        this.setState({title: event.target.value});
    },
    _isCreateModal() {
        return this.props.action === 'create';
    },
    render() {
        var header = <h2 className="modal-title">{this._isCreateModal() ? "New Dashboard" : "Edit Dashboard " + this.props.title}</h2>;
        var body = (
            <fieldset>
                <div className="form-group">
                    <label htmlFor={this.props.id + "-title"}>Title:</label>
                    <input id={this.props.id + "-title"}
                           type="text"
                           className="form-control"
                           onChange={this._onTitleChange}
                           value={this.state.title}
                           required/>
                </div>
                <div className="form-group">
                    <label>Description:</label>
                    <input type="text" className="form-control" onChange={this._onDescriptionChange} value={this.state.description} required/>
                </div>
            </fieldset>
        );

        return (
            <BootstrapModal ref="modal" onCancel={this._closeModal} onConfirm={this._save} cancel="Cancel" confirm="Save">
               {header}
               {body}
            </BootstrapModal>
        );
    },
    close() {
        this.refs.modal.close();
    },
    open() {
        this.refs.modal.open();
    },
    _save() {
        var promise;

        if (this._isCreateModal()) {
            promise = DashboardStore.createDashboard(this.state.title, this.state.description);
            promise.done((id) => {
                this.close();
                if (typeof this.props.onSaved === 'function') {
                    this.props.onSaved(id);
                }
            });
        } else {
            promise = DashboardStore.saveDashboard(this.state);
            promise.done(() => {
                this.close();
                var idSelector = '[data-dashboard-id="' + this.state.id + '"]';
                $(idSelector + '.dashboard-title').html(this.state.title);
                $(idSelector + '.dashboard-description').html(this.state.description);
                if (typeof this.props.onSaved === 'function') {
                    this.props.onSaved(this.state.id);
                }
            });
        }
    }
});

module.exports = EditDashboardModal;
