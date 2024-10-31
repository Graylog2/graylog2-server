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

import { BootstrapModalForm, Input } from 'components/bootstrap';

type CloneMenuModalProps = {
  error?: string;
  id: string;
  showModal: boolean;
  name: string;
  onChange: (...args: any[]) => void;
  onSave: (...args: any[]) => void;
  onSelect?: () => void;
  onClose: (...args: any[]) => void;
};

const CloneMenuModal = ({
  error,
  id,
  showModal,
  onClose,
  name,
  onChange,
  onSave,
  // TODO: Find out why this is not in use (anymore)
  onSelect: _onSelect,
}: CloneMenuModalProps) => (
  <BootstrapModalForm show={showModal}
                      title="Clone"
                      onSubmitForm={onSave}
                      onCancel={onClose}
                      submitButtonDisabled={Boolean(error)}
                      submitButtonText="Done">
    <fieldset>
      <Input type="text"
             id={id}
             label="Name"
             defaultValue={name}
             onChange={onChange}
             bsStyle={error ? 'error' : null}
             help={error || 'Type a name for the new collector'}
             autoFocus
             required />
    </fieldset>
  </BootstrapModalForm>
);

export default CloneMenuModal;
