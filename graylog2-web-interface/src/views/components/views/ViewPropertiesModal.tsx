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
import * as React from 'react';
import { useState } from 'react';
import PropTypes from 'prop-types';

import * as FormsUtils from 'util/FormsUtils';
import ViewTypeLabel from 'views/components/ViewTypeLabel';
import BootstrapModalForm from 'components/bootstrap/BootstrapModalForm';
import Input from 'components/bootstrap/Input';
import type View from 'views/logic/views/View';
import usePluginEntities from 'views/logic/usePluginEntities';

type Props = {
  onClose: () => void,
  onSave: (view: View) => void,
  title: string,
  view: View,
  show: boolean
};

const ViewPropertiesModal = ({ onClose, onSave, show, view, title: modalTitle }: Props) => {
  const [updatedView, setUpdatedView] = useState(view);
  const viewType = ViewTypeLabel({ type: updatedView.type });
  const pluggableFormComponents = usePluginEntities('views.components.saveViewForm');

  const _onChange = (event) => {
    const { name } = event.target;
    let value = FormsUtils.getValueFromInput(event.target);
    const trimmedValue = value.trim();

    if (trimmedValue === '') {
      value = trimmedValue;
    }

    switch (name) {
      case 'title':
        setUpdatedView((_updatedView) => _updatedView.toBuilder().title(value).build());
        break;
      case 'summary':
        setUpdatedView((_updatedView) => _updatedView.toBuilder().summary(value).build());
        break;
      case 'description':
        setUpdatedView((_updatedView) => _updatedView.toBuilder().description(value).build());
        break;
      default:
    }
  };

  const _onSave = () => {
    onSave(view);
    onClose();
  };

  return (
    <BootstrapModalForm show={show}
                        title={modalTitle}
                        onCancel={onClose}
                        onSubmitForm={_onSave}
                        submitButtonText="Save"
                        bsSize="large">
      <Input id="title"
             type="text"
             name="title"
             label="Title"
             help={`The title of the ${viewType}.`}
             required
             onChange={_onChange}
             value={updatedView.title} />
      <Input id="summary"
             type="text"
             name="summary"
             label="Summary"
             help={`A helpful summary of the ${viewType}.`}
             onChange={_onChange}
             value={updatedView.summary} />
      <Input id="description"
             type="textarea"
             name="description"
             label="Description"
             help={`A longer, helpful description of the ${viewType} and its functionality.`}
             onChange={_onChange}
             value={updatedView.description} />
      {pluggableFormComponents?.map((Component) => (<Component />))}
    </BootstrapModalForm>
  );
};

ViewPropertiesModal.propTypes = {
  onClose: PropTypes.func.isRequired,
  onSave: PropTypes.func.isRequired,
  show: PropTypes.bool.isRequired,
  title: PropTypes.string.isRequired,
  view: PropTypes.object.isRequired,
};

export default ViewPropertiesModal;
