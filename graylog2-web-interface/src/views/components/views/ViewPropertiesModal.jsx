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
import { isEqual } from 'lodash';

import FormsUtils from 'util/FormsUtils';
import ViewTypeLabel from 'views/components/ViewTypeLabel';
import BootstrapModalForm from 'components/bootstrap/BootstrapModalForm';
import Input from 'components/bootstrap/Input';

export default class ViewPropertiesModal extends React.Component {
  static propTypes = {
    onClose: PropTypes.func.isRequired,
    onSave: PropTypes.func.isRequired,
    show: PropTypes.bool.isRequired,
    title: PropTypes.string.isRequired,
    view: PropTypes.object.isRequired,
  };

  constructor(props) {
    super(props);

    this.state = {
      view: props.view,
      title: props.title,
    };
  }

  // eslint-disable-next-line camelcase
  UNSAFE_componentWillReceiveProps(nextProps) {
    const { title } = this.props;
    const { view } = this.state;

    if (title !== nextProps.title || !isEqual(view, nextProps.view)) {
      this.setState({ view: nextProps.view, title: nextProps.title });
    }
  }

  // eslint-disable-next-line consistent-return
  _onChange = (event) => {
    const { name } = event.target;
    let value = FormsUtils.getValueFromInput(event.target);
    const trimmedValue = value.trim();

    if (trimmedValue === '') {
      value = trimmedValue;
    }

    switch (name) {
      case 'title': return this.setState((state) => ({ view: state.view.toBuilder().title(value).build() }));
      case 'summary': return this.setState((state) => ({ view: state.view.toBuilder().summary(value).build() }));
      case 'description': return this.setState((state) => ({ view: state.view.toBuilder().description(value).build() }));
      default: break;
    }
  };

  _onSave = () => {
    const { onClose, onSave } = this.props;
    const { view } = this.state;

    onSave(view);
    onClose();
  };

  render() {
    const { view: { title = '', summary = '', description = '' }, title: modalTitle } = this.state;
    const { onClose, show, view } = this.props;
    const viewType = ViewTypeLabel({ type: view.type });

    return (
      <BootstrapModalForm show={show}
                          title={modalTitle}
                          onCancel={onClose}
                          onSubmitForm={this._onSave}
                          submitButtonText="Save"
                          bsSize="large">
        <Input id="title"
               type="text"
               name="title"
               label="Title"
               help={`The title of the ${viewType}.`}
               required
               onChange={this._onChange}
               value={title} />
        <Input id="summary"
               type="text"
               name="summary"
               label="Summary"
               help={`A helpful summary of the ${viewType}.`}
               onChange={this._onChange}
               value={summary} />
        <Input id="description"
               type="textarea"
               name="description"
               label="Description"
               help={`A longer, helpful description of the ${viewType} and its functionality.`}
               onChange={this._onChange}
               value={description} />
      </BootstrapModalForm>
    );
  }
}
