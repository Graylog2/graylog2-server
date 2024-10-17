import React from 'react';

import CloneMenuModal from '../common/CloneMenuModal';

type CopyConfigurationModalProps = {
  configuration: any;
  copyConfiguration: (...args: any[]) => void;
  validateConfiguration: (config: { name: string }) => Promise<{ errors: { name: string[] } }>;
  onClose: (...args: any[]) => void;
  showModal?: boolean;
};

class CopyConfigurationModal extends React.Component<CopyConfigurationModalProps, {
  [key: string]: any;
}> {
  static defaultProps = {
    showModal: false,
  };

  constructor(props) {
    super(props);

    this.state = {
      id: props.configuration.id,
      name: '',
      errorMessage: undefined,
    };
  }

  _getId = (prefixIdName) => {
    const { id } = this.state;

    return `${prefixIdName}-${id}`;
  };

  _saved = () => {
    const { onClose } = this.props;
    onClose();
    this.setState({ name: '' });
  };

  _save = () => {
    const { copyConfiguration } = this.props;
    const { errorMessage, id, name } = this.state;

    if (!errorMessage) {
      copyConfiguration(id, name, this._saved);
    }
  };

  _changeName = (event) => {
    const { validateConfiguration } = this.props;
    const name = event.target.value;

    this.setState({ name, errorMessage: undefined });

    validateConfiguration({ name }).then((validation) => {
      if (validation.errors.name) {
        this.setState({ errorMessage: validation.errors.name[0] });
      }
    });
  };

  render() {
    const { errorMessage, name } = this.state;
    const { onClose, showModal } = this.props;

    return (
      <CloneMenuModal onClose={() => onClose()}
                      onSave={this._save}
                      id={this._getId('configuration-name')}
                      onChange={this._changeName}
                      error={errorMessage}
                      name={name}
                      showModal={showModal} />
    );
  }
}

export default CopyConfigurationModal;
