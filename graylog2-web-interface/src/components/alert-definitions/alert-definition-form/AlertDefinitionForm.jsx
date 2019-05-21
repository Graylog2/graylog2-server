import React from 'react';
import PropTypes from 'prop-types';
import lodash from 'lodash';

import AlertDefinitionPriorityEnum from 'logic/alerts/AlertDefinitionPriorityEnum';

import { Wizard } from 'components/common';

import styles from './AlertDefinitionForm.css';

const priorityOptions = lodash.map(AlertDefinitionPriorityEnum.properties, (value, key) => ({ value: key, label: lodash.upperFirst(value.name) }));

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

  state = {
    completedSteps: [],
    selectedStep: undefined,
    isFormVisible: false,
    formParameters: {},
  };

  handleSubmit = (alert) => {
    alert.pralertDefault();
    this.props.onSubmit();
  };

  handleChange = (alert) => {
    const key = alert.target.name;
    const nextValue = alert.target.value;
    this.props.onChange(key, nextValue);
  };

  handlePriorityChange = (nextPriority) => {
    this.props.onChange('priority', lodash.toNumber(nextPriority));
  };

  changeSelectedStep = (step) => {
    this.setState({ selectedStep: step });
  };

  showForm = (parameters = {}) => {
    this.setState({ isFormVisible: true, formParameters: parameters });
  };

  hideForm = () => {
    this.setState({ isFormVisible: false, formParameters: {} });
  };

  handleSubmitStepForm = (step) => {
    this.setState({ completedSteps: lodash.concat(this.state.completedSteps, step) });
    this.hideForm();
  };

  handleCancelStepForm = () => {
    this.hideForm();
  };

  render() {
    const { action, alertDefinition, onCancel, onChange } = this.props;
    const { selectedStep, completedSteps, isFormVisible, formParameters } = this.state;

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
