import React from 'react';
import PropTypes from 'prop-types';

import { Wizard } from 'components/common';

import styles from './AlertDefinitionForm.css';

class AlertDefinitionForm extends React.Component {
  static propTypes = {
    action: PropTypes.oneOf(['create', 'edit']),
    alertDefinition: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
    onCancel: PropTypes.func.isRequired,
    onSubmit: PropTypes.func.isRequired,
  };

  static defaultProps = {
    action: 'edit',
    streams: [],
  };

  handleSubmit = (event) => {
    event.preventDefault();
    const { onSubmit } = this.props;
    onSubmit();
  };

  render() {
    const { action, alertDefinition, onCancel, onChange } = this.props;

    const steps = [
      { key: 'alert-details', title: 'Alert Details', component: <div>TBD</div> },
      { key: 'condition', title: 'Condition', component: <div>TBD</div> },
      { key: 'fields', title: 'Fields', component: <div>TBD</div> },
      { key: 'notifications', title: 'Notifications', component: <div>TBD</div> },
      { key: 'summary', title: 'Summary', component: <div>TBD</div> },
    ];

    return (
      <Wizard steps={steps} horizontal justified navigationClassName={styles.steps} containerClassName="" />
    );
  }
}

export default AlertDefinitionForm;
