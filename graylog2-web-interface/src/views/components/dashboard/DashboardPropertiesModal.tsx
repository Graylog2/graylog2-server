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

import * as FormsUtils from 'util/FormsUtils';
import BootstrapModalForm from 'components/bootstrap/BootstrapModalForm';
import Input from 'components/bootstrap/Input';
import type View from 'views/logic/views/View';
import useSaveViewFormControls from 'views/hooks/useSaveViewFormControls';
import EntityCreateShareFormGroup from 'components/permissions/EntityCreateShareFormGroup';
import type { EntitySharePayload } from 'actions/permissions/EntityShareActions';

type Props = {
  onClose: () => void;
  onSave: (view: View, entityShare?: EntitySharePayload) => void;
  show: boolean;
  submitButtonText: string;
  title: string;
  view: View;
  dashboardId?: string;
};

const DashboardPropertiesModal = ({
  onClose,
  onSave,
  show,
  view,
  title: modalTitle,
  submitButtonText,
  dashboardId = null,
}: Props) => {
  const [updatedDashboard, setUpdatedDashboard] = useState(view);
  const [sharePayload, setSharePayload] = useState(null);
  const pluggableFormComponents = useSaveViewFormControls();

  const _onChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const { name } = event.target;
    let value = FormsUtils.getValueFromInput(event.target);
    const trimmedValue = value.trim();

    if (trimmedValue === '') {
      value = trimmedValue;
    }

    switch (name) {
      case 'title':
        setUpdatedDashboard((_updatedDashboard) => _updatedDashboard.toBuilder().title(value).build());
        break;
      case 'summary':
        setUpdatedDashboard((_updatedDashboard) => _updatedDashboard.toBuilder().summary(value).build());
        break;
      case 'description':
        setUpdatedDashboard((_updatedDashboard) => _updatedDashboard.toBuilder().description(value).build());
        break;
      default:
    }
  };

  const _onSave = (e: React.FormEvent<HTMLFormElement>) => {
    e.stopPropagation();
    onSave(updatedDashboard, sharePayload);
    onClose();
  };

  return (
    <BootstrapModalForm
      show={show}
      title={modalTitle}
      data-telemetry-title="Dashboard Properties"
      onCancel={onClose}
      onSubmitForm={_onSave}
      submitButtonText={submitButtonText}
      bsSize="large">
      <>
        <Input
          id="title"
          type="text"
          name="title"
          label="Title"
          help="The title of the dashboard."
          required
          onChange={_onChange}
          value={updatedDashboard.title}
        />
        <Input
          id="summary"
          type="text"
          name="summary"
          label="Summary"
          help="A helpful summary of the dashboard."
          onChange={_onChange}
          value={updatedDashboard.summary}
        />
        <Input
          id="description"
          type="textarea"
          name="description"
          label="Description"
          help="A longer, helpful description of the dashboard and its functionality."
          onChange={_onChange}
          value={updatedDashboard.description}
        />
        {dashboardId !== view.id && (
          <EntityCreateShareFormGroup
            description="Search for a User or Team to add as collaborator on this dashboard."
            entityType="dashboard"
            entityTitle=""
            onSetEntityShare={(payload) => setSharePayload(payload)}
          />
        )}
        {pluggableFormComponents?.map(({ component: Component, id }) => Component && <Component key={id} />)}
      </>
    </BootstrapModalForm>
  );
};

export default DashboardPropertiesModal;
