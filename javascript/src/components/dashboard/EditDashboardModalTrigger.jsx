'use strict';

var React = require('react/addons');
var EditDashboardModal = require('./EditDashboardModal');

var EditDashboardModalTrigger = React.createClass({
    getDefaultProps() {
        return {
            action: 'create'
        };
    },
    _isCreateModal() {
        return this.props.action === 'create';
    },
    openModal() {
        this.refs['modal'].open();
    },
    render() {
        var triggerButtonContent;

        if (this.props.children === undefined || this.props.children.trim() === "") {
            var buttonText = this._isCreateModal() ? "Create dashboard" : "Edit dashboard";
            triggerButtonContent = {__html: buttonText};
        } else {
            triggerButtonContent = {__html: this.props.children};
        }

        return (
            <span>
                <button onClick={this.openModal}
                    className={"btn btn-info " + this.props.buttonClass}
                    dangerouslySetInnerHTML={triggerButtonContent}>
                </button>
                <EditDashboardModal ref="modal" {...this.props}/>
            </span>
        );
    }
});

module.exports = EditDashboardModalTrigger;
