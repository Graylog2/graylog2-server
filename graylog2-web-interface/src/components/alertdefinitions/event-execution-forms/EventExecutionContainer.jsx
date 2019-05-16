import React from 'react';
import PropTypes from 'prop-types';
import { Alert } from 'react-bootstrap';
import { BootstrapModalForm } from 'components/bootstrap';

import DummyProcessorParameterForm from './DummyProcessorParameterForm';
import FilterProcessorParameterForm from './FilterProcessorParameterForm';
import CorrelationProcessorParameterForm from './CorrelationProcessorParameterForm';

// TODO: The different processor types should register themselves instead of hard-coding this here
const PROCESSOR_TYPES = {
  'dummy-v1': DummyProcessorParameterForm,
  'filter-v1': FilterProcessorParameterForm,
  'correlation-v1': CorrelationProcessorParameterForm,
};

export default class EventExecutionContainer extends React.Component {
  static propTypes = {
    eventDefinition: PropTypes.object,
    onSubmit: PropTypes.func.isRequired,
    onCancel: PropTypes.func,
  };

  static defaultProps = {
    eventDefinition: undefined,
    onCancel: () => {},
  };

  static getDerivedStateFromProps(props) {
    if (!props.eventDefinition) {
      // Reset state to make sure we start with a clean slate next time the modal is opened.
      // We don't do this in handleSubmit() because if the submit fails, we don't close the modal and the values
      // should stay the same.
      return { parameters: {}, isSubmittable: false };
    }
    return null;
  }

  state = {
    isSubmittable: false,
    parameters: {},
  };

  handleParameterChange = (parameters) => {
    this.setState({ parameters });
  };

  handleCancel = (definition) => {
    return () => {
      this.setState({ parameters: {}, isSubmittable: false });
      this.props.onCancel(definition);
    };
  };

  handleSubmit = () => {
    this.props.onSubmit(this.props.eventDefinition, this.state.parameters);
  };

  handleSubmittable = (isSubmittable) => {
    this.setState({ isSubmittable });
  };

  renderContent = (definition) => {
    const ParameterComponent = PROCESSOR_TYPES[definition.config.type];

    if (!ParameterComponent) {
      return (
        <Alert bsStyle="danger">
          <strong>Missing UI Component</strong><br />
          Could not find parameter UI component for definition type <code>{definition.config.type}</code>
        </Alert>
      );
    }

    return (
      <ParameterComponent eventDefinition={definition}
                          parameters={this.state.parameters}
                          onChange={this.handleParameterChange}
                          onSubmittable={this.handleSubmittable} />
    );
  };

  render() {
    const definition = this.props.eventDefinition;

    if (!definition) {
      return null;
    }

    return (
      <BootstrapModalForm title={`Execute "${definition.title}"`}
                          submitButtonDisabled={!this.state.isSubmittable}
                          show
                          onCancel={this.handleCancel(definition)}
                          onSubmitForm={this.handleSubmit}>
        {this.renderContent(definition)}
      </BootstrapModalForm>
    );
  }
}
