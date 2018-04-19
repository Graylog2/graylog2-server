import PropTypes from 'prop-types';
import React from 'react';
import { Button } from 'react-bootstrap';
import { ConfigurationForm } from 'components/configurationforms';

class EditOutputButton extends React.Component {
  static propTypes = {
    output: PropTypes.object,
    disabled: PropTypes.bool,
    getTypeDefinition: PropTypes.func.isRequired,
    onUpdate: PropTypes.func,
  };

  state = {
    typeDefinition: undefined,
    typeName: undefined,
    configurationForm: '',
  };

  handleClick = () => {
    this.props.getTypeDefinition(this.props.output.type, (definition) => {
      this.setState({ typeDefinition: definition.requested_configuration });
      this.configurationForm.open();
    });
  };

  _handleSubmit = (data) => {
    this.props.onUpdate(this.props.output, data);
  };

  render() {
    const typeDefinition = this.state.typeDefinition;
    const output = this.props.output;
    let configurationForm;

    if (typeDefinition) {
      configurationForm = (
        <ConfigurationForm ref={(configurationForm) => { this.configurationForm = configurationForm; }} key={`configuration-form-output-${output.id}`}
                           configFields={this.state.typeDefinition}
                           title={`Editing Output ${output.title}`}
                           typeName={output.type}
                           helpBlock={'Select a name of your new output that describes it.'}
                           submitAction={this._handleSubmit} values={output.configuration} titleValue={output.title} />
      );
    }

    return (
      <span>
        <Button disabled={this.props.disabled} bsStyle="info" onClick={this.handleClick.bind(null, output)}>
          Edit
        </Button>
        {configurationForm}
      </span>
    );
  }
}

export default EditOutputButton;
