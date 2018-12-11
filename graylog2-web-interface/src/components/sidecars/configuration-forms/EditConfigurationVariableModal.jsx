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
      id: this.props.id,
      name: this.props.name,
      savedName: this.props.name,
      description: this.props.description,
      content: this.props.content,
      errors: {},
    };
  };

  _hasErrors = () => {
    return !lodash.isEmpty(this.state.errors);
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

  _validate = () => {
    ConfigurationVariableActions.validate(this.state).then((validation) => {
      this.setState({ errors: validation.errors });
    });
  };

  _debouncedValidate = lodash.debounce(this._validate, 200);

  _save = () => {
    const configuration = this.state;

    if (!configuration.error) {
      this.props.saveConfigurationVariable(configuration, this._saved);
    }
  };

  _changeName = (event) => {
    this.setState({ name: event.target.value }, this._debouncedValidate);
  };

  _changeDescription = (event) => {
    this.setState({ description: event.target.value });
  };

  _changeContent = (event) => {
    this.setState({ content: event.target.value });
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
                            title={<React.Fragment>{this.props.create ? 'Create' : 'Edit'} Variable $&#123;user.{this.state.name}&#125;</React.Fragment>}
                            onSubmitForm={this._save}
                            onModalClose={this._cleanState}
                            submitButtonDisabled={this._hasErrors()}
                            submitButtonText="Save">
          <fieldset>
            <Input type="text"
                   id={this._getId('variable-name')}
                   label="Name"
                   defaultValue={this.state.name}
                   onChange={this._changeName}
                   bsStyle={this.state.errors.name ? 'error' : null}
                   help={this.state.errors.name ? this.state.errors.name.join(' ') : 'Type a name for this variable'}
                   autoFocus
                   spellCheck={false}
                   required />
            <Input type="text"
                   id={this._getId('variable-description')}
                   label="Description"
                   defaultValue={this.state.description}
                   onChange={this._changeDescription}
                   bsStyle={this.state.error ? 'error' : null}
                   help="Type a description for this variable"
                   spellCheck={false} />
            <Input type="textarea"
                   id={this._getId('variable-content')}
                   label="Content"
                   rows="10"
                   className={ConfigurationHelperStyle.monoSpaceFont}
                   defaultValue={this.state.content}
                   onChange={this._changeContent}
                   bsStyle={this.state.error ? 'error' : null}
                   help="Write your variable content"
                   spellCheck={false}
                   required />
          </fieldset>
        </BootstrapModalForm>
      </React.Fragment>
    );
  }
}

export default EditConfigurationVariableModal;
