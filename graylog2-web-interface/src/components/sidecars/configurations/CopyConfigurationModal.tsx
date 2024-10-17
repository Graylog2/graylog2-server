/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
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
