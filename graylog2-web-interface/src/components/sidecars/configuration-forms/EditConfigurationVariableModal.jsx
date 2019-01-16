import PropTypes from 'prop-types';
import lodash from 'lodash';
import React from 'react';
import { Button } from 'react-bootstrap';
import { BootstrapModalForm, Input } from 'components/bootstrap';
import CombinedProvider from 'injection/CombinedProvider';
import ConfigurationHelperStyle from './ConfigurationHelper.css';

const { ConfigurationVariableActions } = CombinedProvider.get('ConfigurationVariable');

class EditConfigurationVariableModal extends React.Component {
  static propTypes = {
    create: PropTypes.bool,
    id: PropTypes.string,
    name: PropTypes.string,
    description: PropTypes.string,
    content: PropTypes.string,
    saveConfigurationVariable: PropTypes.func.isRequired,
  };

  static defaultProps = {
    create: false,
    id: '',
    name: '',
    description: '',
    content: '',
  };

  constructor(props) {
    super(props);
    this.state = this._cleanState();
  }

  _cleanState = () => {
    return {
      error: false,
      validation_errors: {},
      savedName: this.props.name,
      formData: {
        id: this.props.id,
        name: this.props.name,
        description: this.props.description,
        content: this.props.content,
      },
    };
  };

  _hasErrors = () => {
    return this.state.error;
  };

  openModal = () => {
    this.setState(this._cleanState());
    this.modal.open();
  };

  _getId = (prefixIdName) => {
    return prefixIdName + this.state.id || 'new';
  };

  _saved = () => {
    this.modal.close();
  };

  _validateFormData = (nextFormData) => {
    ConfigurationVariableActions.validate(nextFormData).then((validation) => {
      this.setState({ validation_errors: validation.errors, error: validation.failed });
    });
  };

  _debouncedValidateFormData = lodash.debounce(this._validateFormData, 200);

  _formDataUpdate = (key) => {
    return (nextValue) => {
      const nextFormData = lodash.cloneDeep(this.state.formData);
      nextFormData[key] = nextValue;
      this._debouncedValidateFormData(nextFormData);
      this.setState({ formData: nextFormData });
    };
  };

  _save = () => {
    if (this._hasErrors()) {
      // Ensure we display an error on the content field, as this is not validated by the browser
      this._validateFormData(this.state.formData);
      return;
    }

    this.props.saveConfigurationVariable(this.state.formData, this.state.savedName, this._saved);
  };

  _handleInputChange = (event) => {
    this._formDataUpdate(event.target.name)(event.target.value);
  };

  _formatValidationMessage = (fieldName, defaultText) => {
    if (this.state.validation_errors[fieldName]) {
      return <span>{this.state.validation_errors[fieldName][0]}</span>;
    }
    return <span>{defaultText}</span>;
  };

  _validationState = (fieldName) => {
    if (this.state.validation_errors[fieldName]) {
      return 'error';
    }
    return null;
  };

  render() {
    let triggerButtonContent;
    if (this.props.create) {
      triggerButtonContent = 'Create Variable';
    } else {
      triggerButtonContent = <span>Edit</span>;
    }

    return (
      <React.Fragment>
        <Button onClick={this.openModal}
                bsStyle={this.props.create ? 'success' : 'info'}
                bsSize={this.props.create ? 'small' : 'xsmall'}
                className={this.props.create ? 'pull-right' : ''}>
          {triggerButtonContent}
        </Button>
        <BootstrapModalForm ref={(ref) => { this.modal = ref; }}
                            title={<React.Fragment>{this.props.create ? 'Create' : 'Edit'} Variable $&#123;user.{this.state.formData.name}&#125;</React.Fragment>}
                            onSubmitForm={this._save}
                            onModalClose={this._cleanState}
                            submitButtonDisabled={this._hasErrors()}
                            submitButtonText="Save">
          <fieldset>
            <Input type="text"
                   id={this._getId('variable-name')}
                   label="Name"
                   name="name"
                   defaultValue={this.state.formData.name}
                   onChange={this._handleInputChange}
                   bsStyle={this._validationState('name')}
                   help={this._formatValidationMessage('name', 'Type a name for this variable')}
                   autoFocus
                   spellCheck={false}
                   required />
            <Input type="text"
                   id={this._getId('variable-description')}
                   label={<span>Description <small className="text-muted">(Optional)</small></span>}
                   name="description"
                   defaultValue={this.state.formData.description}
                   onChange={this._handleInputChange}
                   help="Type a description for this variable"
                   spellCheck={false} />
            <Input type="textarea"
                   id={this._getId('variable-content')}
                   label="Content"
                   name="content"
                   rows="10"
                   className={ConfigurationHelperStyle.monoSpaceFont}
                   defaultValue={this.state.formData.content}
                   onChange={this._handleInputChange}
                   bsStyle={this._validationState('content')}
                   help={this._formatValidationMessage('content', 'Write your variable content')}
                   spellCheck={false}
                   required />
          </fieldset>
        </BootstrapModalForm>
      </React.Fragment>
    );
  }
}

export default EditConfigurationVariableModal;
