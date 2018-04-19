import PropTypes from 'prop-types';
import React from 'react';

import { EditDashboardModal } from 'components/dashboard';

class EditDashboardModalTrigger extends React.Component {
  static propTypes = {
    action: PropTypes.string.isRequired,
  };

  static defaultProps = {
    action: 'create',
  };

  _isCreateModal = () => {
    return this.props.action === 'create';
  };

  openModal = () => {
    this.modal.open();
  };

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
        <EditDashboardModal ref={(modal) => { this.modal = modal; }} {...this.props} />
      </span>
    );
  }
}

export default EditDashboardModalTrigger;
