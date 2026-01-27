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
import { Stack } from '@mantine/core';

import { Button, Input } from 'components/bootstrap';
import Modal from 'components/bootstrap/Modal';

import type { Fleet } from '../types';

type Props = {
  fleet?: Fleet;
  onClose: () => void;
  onSave: (fleet: Omit<Fleet, 'id' | 'created_at' | 'updated_at'>) => void;
  isLoading?: boolean;
};

const FleetFormModal = ({ fleet = undefined, onClose, onSave, isLoading = false }: Props) => {
  const isEdit = !!fleet;
  const [name, setName] = useState(fleet?.name || '');
  const [description, setDescription] = useState(fleet?.description || '');
  const [targetVersion, setTargetVersion] = useState(fleet?.target_version || '');

  const handleSave = () => {
    onSave({
      name,
      description,
      target_version: targetVersion || null,
    });
  };

  return (
    <Modal show onHide={onClose}>
      <Modal.Header>
        <Modal.Title>{isEdit ? 'Edit Fleet' : 'New Fleet'}</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        <Stack gap="md">
          <Input
            id="fleet-name"
            type="text"
            label="Name"
            help="A unique name for this fleet"
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
          />

          <Input
            id="fleet-description"
            type="textarea"
            label="Description"
            help="Optional description of this fleet's purpose"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
          />

          <Input
            id="fleet-target-version"
            type="text"
            label="Target Version"
            help="Optional collector version for this fleet"
            placeholder="e.g., 1.2.0"
            value={targetVersion}
            onChange={(e) => setTargetVersion(e.target.value)}
          />
        </Stack>
      </Modal.Body>
      <Modal.Footer>
        <Button bsStyle="default" onClick={onClose}>Cancel</Button>
        {' '}
        <Button bsStyle="primary" onClick={handleSave} disabled={!name || isLoading}>
          {isEdit ? 'Save Changes' : 'Create Fleet'}
        </Button>
      </Modal.Footer>
    </Modal>
  );
};

export default FleetFormModal;
