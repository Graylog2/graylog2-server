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
    _onDescriptionChange(event) {
        this.setState({description: event.target.value});
    },
    _onTitleChange(event) {
        this.setState({title: event.target.value});
    },
    render() {
        var header = <h2 className="modal-title">Edit Dashboard {this.props.title}</h2>;
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
                    <input type="text" className="form-control" onChange={this._onDescriptionChange} value={this.state.description}  required/>
                </div>
            </fieldset>
        );
        return (
            <span>
                <button onClick={this.openModal} className="btn btn-info">Edit dashboard</button>
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
        DashboardStore.saveDashboard(this.state, () => {
            this._closeModal();
            var idSelector = '[data-dashboard-id="' + this.state.id + '"]';
            $(idSelector + '.dashboard-title').html(this.state.title);
            $(idSelector + '.dashboard-description').html(this.state.description);
        });
    }
});

module.exports = EditDashboardModal;
