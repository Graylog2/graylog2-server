import React from 'react';
import PropTypes from 'prop-types';
import lodash from 'lodash';

import history from 'util/History';
import Routes from 'routing/Routes';

import EventDefinitionPriorityEnum from 'logic/alerts/EventDefinitionPriorityEnum';
import CombinedProvider from 'injection/CombinedProvider';
import EventDefinitionForm from './EventDefinitionForm';

const { EventDefinitionsActions } = CombinedProvider.get('EventDefinitions');
const { StreamsStore } = CombinedProvider.get('Streams');

class EventDefinitionFormContainer extends React.Component {
  static propTypes = {
    action: PropTypes.oneOf(['create', 'edit']),
    eventDefinition: PropTypes.object,
  };

  static defaultProps = {
    action: 'edit',
    eventDefinition: {
      title: '',
      description: '',
      priority: EventDefinitionPriorityEnum.NORMAL,
      config: {},
      field_spec: [],
      key_spec: [],
      actions: [],
    },
  };

  constructor(props) {
    super(props);

    this.state = {
      eventDefinition: props.eventDefinition,
      availableStreams: undefined,
    };
  }

  componentDidMount() {
    StreamsStore.load(streams => this.setState({ availableStreams: streams }));
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
        .then(() => history.push(Routes.NEXT_ALERTS.DEFINITIONS.CREATE));
    } else {
      EventDefinitionsActions.update(eventDefinition.id, eventDefinition)
        .then(() => history.push(Routes.NEXT_ALERTS.DEFINITIONS.LIST));
    }
  };

  render() {
    const { action } = this.props;
    const { eventDefinition, availableStreams } = this.state;

    return (
      <EventDefinitionForm action={action}
                           eventDefinition={eventDefinition}
                           streams={availableStreams}
                           onChange={this.handleChange}
                           onCancel={this.handleCancel}
                           onSubmit={this.handleSubmit} />
    );
  }
}

export default EventDefinitionFormContainer;
