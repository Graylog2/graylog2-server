import React from 'react';

import { EditDashboardModal } from 'components/dashboard';

const EditDashboardModalTrigger = React.createClass({
  propTypes: {
    action: React.PropTypes.string.isRequired,
  },
  getDefaultProps() {
    return {
      action: 'create',
    };
  },
  _isCreateModal() {
    return this.props.action === 'create';
  },
  openModal() {
    this.refs.modal.open();
  },
  render() {
    let triggerButtonContent;

    if (this.props.children === undefined || this.props.children.length === 0) {
      triggerButtonContent = this._isCreateModal() ? 'Create dashboard' : 'Edit dashboard';
    } else {
      triggerButtonContent = this.props.children;
    }

    return (
      <span>
        <button onClick={this.openModal}
                className={`btn ${this.props.buttonClass}`}>
          {triggerButtonContent}
        </button>
        <EditDashboardModal ref="modal" {...this.props} />
      </span>
    );
  },
});

export default EditDashboardModalTrigger;
