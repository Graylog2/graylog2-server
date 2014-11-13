/** @jsx React.DOM */

'use strict';

var React = require('react/addons');
var BootstrapModal = require('../bootstrap/BootstrapModal');

var TrendConfigurationModal = React.createClass({
    getInitialState: function () {
        return {description: ""};
    },
    _onChange: function (event) {
        this.setState({description:  event.target.value});
    },
    render() {
        var header = <h2>Configure Dashboard Widget</h2>;
        var body = (
            <label>Description:&nbsp;
                <input onChange={this._onChange} className="form-control" value={this.state.description}/>
            </label>
        );
        return (
            <form className="form-inline" role="form">
                <BootstrapModal ref="modal" onCancel={this.closeModal} onConfirm={this._save} cancel="Cancel" confirm="Save">
                   {header}
                   {body}
                </BootstrapModal>
            </form>
        );
    },
    closeModal() {
        this.refs.modal.close();
    },
    openModal(resultcallback) {
        this.setState({description:  ""});
        this.resultCallback = resultcallback;
        this.refs.modal.open();
    },
    _save: function () {
        this.resultCallback(this.state.description);
        this.closeModal();
    }

});

module.exports = TrendConfigurationModal;
