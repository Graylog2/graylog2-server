import PropTypes from 'prop-types';
import React, { createRef } from 'react';

import CloneMenuItem from '../common/CloneMenuItem';

class CopyConfigurationModal extends React.Component {
  static propTypes = {
    configuration: PropTypes.object.isRequired,
    copyConfiguration: PropTypes.func.isRequired,
    validateConfiguration: PropTypes.func.isRequired,
  };

  constructor(props) {
    super(props);

    this.modalRef = createRef();

    this.state = {
      id: props.configuration.id,
      name: '',
      errorMessage: undefined,
    };
  }

  openModal = () => {
    this.modalRef.current.open();
  };

  _getId = (prefixIdName) => {
    const { id } = this.state;

    return `${prefixIdName}-${id}`;
  };

  _closeModal = () => {
    this.modalRef.current.close();
  };

  _saved = () => {
    this._closeModal();
    this.setState({ name: '' });
  };

  _save = () => {
    const { copyConfiguration } = this.props;
    const { errorMessage, id, name } = this.state;

    if (errorMessage) {
      copyConfiguration(id, name, this._saved);
    }
  };

  _changeName = (event) => {
    const { validateConfiguration } = this.props;
    const name = event.target.value;
    this.setState({ name, errorMessage: undefined });

    validateConfiguration({ name }).then((validation) => {
      const [errorMessage] = validation.errors.name;

      this.setState({ errorMessage });
    });
  };

  render() {
    const { errorMessage, name } = this.state;

    return (
      <CloneMenuItem onSelect={this.openModal}
                     onSave={this._save}
                     id={this._getId('configuration-name')}
                     onChange={this._changeName}
                     error={errorMessage}
                     name={name}
                     modalRef={this.modalRef} />
    );
  }
}

export default CopyConfigurationModal;
