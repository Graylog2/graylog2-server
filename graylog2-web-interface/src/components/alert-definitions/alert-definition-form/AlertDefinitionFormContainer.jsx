import React from 'react';
import PropTypes from 'prop-types';
import lodash from 'lodash';

import history from 'util/History';
import Routes from 'routing/Routes';

import AlertDefinitionPriorityEnum from 'logic/alerts/AlertDefinitionPriorityEnum';
import CombinedProvider from 'injection/CombinedProvider';
import AlertDefinitionForm from './AlertDefinitionForm';

const { AlertDefinitionsActions } = CombinedProvider.get('AlertDefinition');

class AlertDefinitionFormContainer extends React.Component {
  static propTypes = {
    action: PropTypes.oneOf(['create', 'edit']),
    alertDefinition: PropTypes.object,
  };

  static defaultProps = {
    action: 'edit',
    alertDefinition: {
      title: '',
      description: '',
      priority: AlertDefinitionPriorityEnum.NORMAL,
      config: {},
      field_spec: {},
      key_spec: [],
      actions: [],
    },
  };

  constructor(props) {
    super(props);

    this.state = {
      alertDefinition: props.alertDefinition,
    };
  }

  handleChange = (key, value) => {
    const { alertDefinition } = this.state;
    const nextAlertDefinition = lodash.cloneDeep(alertDefinition);
    nextAlertDefinition[key] = value;
    this.setState({ alertDefinition: nextAlertDefinition });
  };

  handleCancel = () => {
    history.goBack();
  };

  handleSubmit = () => {
    const { action } = this.props;
    const { alertDefinition } = this.state;

    if (action === 'create') {
      AlertDefinitionsActions.create(alertDefinition)
        .then(() => history.push(Routes.NEXT_ALERTS.DEFINITIONS.CREATE));
    } else {
      AlertDefinitionsActions.update(alertDefinition.id, alertDefinition)
        .then(() => history.push(Routes.NEXT_ALERTS.DEFINITIONS.LIST));
    }
  };

  render() {
    const { action } = this.props;
    const { alertDefinition } = this.state;

    return (
      <AlertDefinitionForm action={action}
                           alertDefinition={alertDefinition}
                           onChange={this.handleChange}
                           onCancel={this.handleCancel}
                           onSubmit={this.handleSubmit} />
    );
  }
}

export default AlertDefinitionFormContainer;
