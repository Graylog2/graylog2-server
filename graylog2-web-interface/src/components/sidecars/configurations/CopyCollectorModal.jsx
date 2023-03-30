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
import PropTypes from 'prop-types';
import upperFirst from 'lodash/upperFirst';

import CloneMenuItem from '../common/CloneMenuItem';

class CopyCollectorModal extends React.Component {
  static propTypes = {
    collector: PropTypes.object.isRequired,
    copyCollector: PropTypes.func.isRequired,
    validateCollector: PropTypes.func.isRequired,
  };

  constructor(props) {
    super(props);

    this.state = {
      showModal: false,
      id: props.collector.id,
      name: '',
      errorMessage: undefined,
    };
  }

  openModal = () => {
    this.setState({ showModal: true });
  };

  _getId = (prefixIdName) => {
    const { id } = this.state;

    return `${prefixIdName}-${id}`;
  };

  _closeModal = () => {
    this.setState({ showModal: false });
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

    const nextCollector = cloneDeep(collector);

    nextCollector.name = name;
    nextCollector.id = '';

    validateCollector(nextCollector).then((validation) => {
      if (validation.errors.name) {
        this.setState({ errorMessage: validation.errors.name[0] });
      }
    });
  };

  render() {
    const { errorMessage, name, showModal } = this.state;

    return (
      <CloneMenuItem onSelect={this.openModal}
                     onClose={this._closeModal}
                     onSave={this._save}
                     id={this._getId('collector-name')}
                     onChange={this._changeName}
                     error={errorMessage}
                     name={name}
                     showModal={showModal} />
    );
  }
}

export default CopyCollectorModal;
