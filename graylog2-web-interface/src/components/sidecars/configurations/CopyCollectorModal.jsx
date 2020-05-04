import React, { createRef } from 'react';
import PropTypes from 'prop-types';
import lodash from 'lodash';

import CloneMenuItem from '../common/CloneMenuItem';

class CopyCollectorModal extends React.Component {
  static propTypes = {
    collector: PropTypes.object.isRequired,
    copyCollector: PropTypes.func.isRequired,
    validateCollector: PropTypes.func.isRequired,
  };

  constructor(props) {
    super(props);

    this.modalRef = createRef();

    this.state = {
      id: props.collector.id,
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
    const { errorMessage, id, name } = this.state;
    const { copyCollector } = this.props;

    if (!errorMessage) {
      copyCollector(id, name, this._saved);
    }
  };

  _changeName = (event) => {
    const { collector, validateCollector } = this.props;
    const name = event.target.value;
    this.setState({ name, errorMessage: undefined });

    const nextCollector = lodash.cloneDeep(collector);
    nextCollector.name = name;
    nextCollector.id = '';

    validateCollector(nextCollector).then((validation) => {
      if (validation.errors.name) {
        this.setState({ errorMessage: validation.errors.name[0] });
      }
    });
  };

  render() {
    const { errorMessage, name } = this.state;

    return (
      <CloneMenuItem onSelect={this.openModal}
                     onSave={this._save}
                     id={this._getId('collector-name')}
                     onChange={this._changeName}
                     error={errorMessage || ''}
                     name={name}
                     modalRef={this.modalRef} />
    );
  }
}

export default CopyCollectorModal;
