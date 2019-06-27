import React from 'react';
import PropTypes from 'prop-types';
import lodash from 'lodash';

import history from 'util/History';
import Routes from 'routing/Routes';
import connect from 'stores/connect';

import EventDefinitionPriorityEnum from 'logic/alerts/EventDefinitionPriorityEnum';
import CombinedProvider from 'injection/CombinedProvider';

import { Spinner } from 'components/common';
import EventDefinitionForm from './EventDefinitionForm';

// Import built-in Event Definition Types
import {} from './event-definition-types';

const { EventDefinitionsActions } = CombinedProvider.get('EventDefinitions');
const { AvailableEventDefinitionTypesStore } = CombinedProvider.get('AvailableEventDefinitionTypes');

class EventDefinitionFormContainer extends React.Component {
  static propTypes = {
    action: PropTypes.oneOf(['create', 'edit']),
    eventDefinition: PropTypes.object,
    entityTypes: PropTypes.object,
  };

  static defaultProps = {
    action: 'edit',
    eventDefinition: {
      title: '',
      description: '',
      priority: EventDefinitionPriorityEnum.NORMAL,
      config: {},
      field_spec: {},
      key_spec: [],
      actions: [],
    },
    entityTypes: undefined,
  };

  constructor(props) {
    super(props);

    this.state = {
      eventDefinition: props.eventDefinition,
    };
  }

  handleChange = (key, value) => {
    const { eventDefinition } = this.state;
    const nextEventDefinition = lodash.cloneDeep(eventDefinition);
    nextEventDefinition[key] = value;
    this.setState({ eventDefinition: nextEventDefinition });
  };

  handleCancel = () => {
    if (window.confirm('Do you really want to abandon this page and lose your changes? This action cannot be undone.')) {
      history.goBack();
    }
  };

  handleSubmit = () => {
    const { action } = this.props;
    const { eventDefinition } = this.state;

    if (action === 'create') {
      EventDefinitionsActions.create(eventDefinition)
        .then(() => history.push(Routes.NEXT_ALERTS.DEFINITIONS.LIST));
    } else {
      EventDefinitionsActions.update(eventDefinition.id, eventDefinition)
        .then(() => history.push(Routes.NEXT_ALERTS.DEFINITIONS.LIST));
    }
  };

  render() {
    const { action, entityTypes } = this.props;
    const { eventDefinition } = this.state;
    const isLoading = !entityTypes;

    if (isLoading) {
      return <Spinner text="Loading Event information..." />;
    }

    return (
      <EventDefinitionForm action={action}
                           eventDefinition={eventDefinition}
                           entityTypes={entityTypes}
                           onChange={this.handleChange}
                           onCancel={this.handleCancel}
                           onSubmit={this.handleSubmit} />
    );
  }
}

export default connect(EventDefinitionFormContainer, { entityTypes: AvailableEventDefinitionTypesStore });
