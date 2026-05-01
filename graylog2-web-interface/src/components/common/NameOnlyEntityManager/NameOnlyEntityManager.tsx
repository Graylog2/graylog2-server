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
import styled from 'styled-components';

import { BootstrapModalConfirm, Button } from 'components/bootstrap';
import SectionComponent from 'components/common/Section/SectionComponent';
import { IconButton, Spinner } from 'components/common';

const DataRow = styled.div`
  width: 100%;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: space-between;

  .default-button {
    opacity: 0;
    transition: opacity 0.2s ease-in-out;
  }

  &:hover .default-button {
    opacity: 1;
  }
`;

const StyledInput = styled.input(
  ({ theme }) => `
  color: ${theme.colors.input.color};
  background-color: ${theme.colors.input.background};
  padding: 6px 12px;
  border: 1px solid ${theme.colors.input.border};

  &:focus {
    border-color: ${theme.colors.input.borderFocus};
    box-shadow: ${theme.colors.input.boxShadow};
  }
`,
);

const StyledButton = styled(Button)`
  justify-content: flex-end;
`;

const CancelButton = styled(StyledButton)`
  margin-right: 5px;
`;

const StyledIconButton = styled(IconButton)`
  padding: 0;
  cursor: pointer;
  background: transparent;
  border: 0;
  float: right;
  font-weight: bold;
  line-height: 1;
  opacity: 0.5;

  &:hover {
    background-color: transparent;
    opacity: 0.7;
  }
`;

export type NameOnlyItem = { id: string; value: string };

export type NameOnlyTelemetryEvent = 'added' | 'updated' | 'deleted';

type Props = {
  title: string;
  /** Lower-case label used in user-visible copy (e.g. "tag", "category"). Drives a kebab-cased key for testids and DOM ids. */
  entityLabel: string;
  items: ReadonlyArray<NameOnlyItem>;
  onAdd: (value: string) => Promise<unknown>;
  onUpdate: (id: string, value: string) => Promise<unknown>;
  onDelete: (id: string) => Promise<unknown>;
  busy?: { adding?: boolean; updating?: boolean; deleting?: boolean };
  /** Per-action permissions. Hide affordances the user can't use. Default to true. */
  permissions?: { create?: boolean; edit?: boolean; delete?: boolean };
  /** Optional custom warning rendered inside the delete confirmation modal. */
  renderDeleteWarning?: (item: NameOnlyItem) => React.ReactNode;
  onTelemetry?: (event: NameOnlyTelemetryEvent) => void;
};

const toKebabCase = (label: string) =>
  label.trim().toLowerCase().replace(/\s+/g, '-').replace(/[^a-z0-9-]/g, '');

const NameOnlyEntityManager = ({
  title,
  entityLabel,
  items,
  onAdd,
  onUpdate,
  onDelete,
  busy = {},
  permissions = {},
  renderDeleteWarning = undefined,
  onTelemetry = undefined,
}: Props) => {
  const labelKey = toKebabCase(entityLabel);
  const canCreate = permissions.create ?? true;
  const canEdit = permissions.edit ?? true;
  const canDelete = permissions.delete ?? true;
  const [editId, setEditId] = useState('');
  const [editValue, setEditValue] = useState('');
  const [showAdd, setShowAdd] = useState(false);
  const [newValue, setNewValue] = useState('');
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [itemToDelete, setItemToDelete] = useState<NameOnlyItem>({ id: '', value: '' });

  const sortedItems = [...items].sort((a, b) => a.value.localeCompare(b.value));

  const resetAdd = () => {
    setNewValue('');
    setShowAdd(false);
  };

  const handleAdd = async () => {
    if (!newValue) return;
    await onAdd(newValue);
    resetAdd();
    onTelemetry?.('added');
  };

  const handleAddKeyDown = (e: React.KeyboardEvent) => {
    if (!newValue) return;
    if (e.key === 'Enter') handleAdd().catch(() => {});
    if (e.key === 'Escape') resetAdd();
  };

  const resetEdit = () => {
    setEditId('');
    setEditValue('');
  };

  const handleEdit = async () => {
    await onUpdate(editId, editValue);
    resetEdit();
    onTelemetry?.('updated');
  };

  const handleEditKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') handleEdit().catch(() => {});
    if (e.key === 'Escape') resetEdit();
  };

  const handleDelete = async (id: string) => {
    await onDelete(id);
    setShowDeleteModal(false);
    setItemToDelete({ id: '', value: '' });
    onTelemetry?.('deleted');
  };

  return (
    <SectionComponent
      title={title}
      headerActions={
        canCreate ? (
          <StyledButton
            bsStyle="primary"
            data-testid={`add-${labelKey}`}
            onClick={() => setShowAdd(true)}
            disabled={showAdd}>
            Add New
          </StyledButton>
        ) : undefined
      }>
      {showAdd && (
        <DataRow key={`new-${labelKey}`}>
          <div style={{ margin: '0' }}>
            <StyledInput
              id={`add-${labelKey}-input`}
              data-testid={`new-${labelKey}-input`}
              type="text"
              autoComplete="off"
              style={{ marginBottom: '0px', paddingRight: '5px' }}
              value={newValue}
              onChange={(e: React.ChangeEvent<HTMLInputElement>) => setNewValue(e.target.value)}
              onKeyDown={handleAddKeyDown}
            />
          </div>
          <div>
            <CancelButton onClick={resetAdd}>Cancel</CancelButton>
            <StyledButton
              bsStyle="primary"
              data-testid={`save-new-${labelKey}`}
              disabled={!newValue || busy.adding}
              onClick={handleAdd}>
              Add
            </StyledButton>
          </div>
        </DataRow>
      )}
      {sortedItems.map((item) => {
        const isEditing = item.id === editId;

        return (
          <DataRow key={item.id}>
            {isEditing ? (
              <>
                <div style={{ margin: '0' }}>
                  <StyledInput
                    id={`edit-${labelKey}-input`}
                    data-testid={`${labelKey}-input`}
                    type="text"
                    autoComplete="off"
                    style={{ marginBottom: '0px', paddingRight: '5px' }}
                    value={editValue}
                    onChange={(e: React.ChangeEvent<HTMLInputElement>) => setEditValue(e.target.value)}
                    onKeyDown={handleEditKeyDown}
                  />
                </div>
                <div>
                  <CancelButton onClick={resetEdit}>Cancel</CancelButton>
                  <StyledButton
                    bsStyle="primary"
                    data-testid={`save-edit-${labelKey}`}
                    disabled={busy.updating}
                    onClick={handleEdit}>
                    Save
                  </StyledButton>
                </div>
              </>
            ) : (
              <>
                <div style={{ display: 'flex' }}>
                  <div>{item.value}</div>
                </div>
                <div>
                  {canDelete && (
                    <StyledIconButton
                      data-testid={`delete-${labelKey}`}
                      title={`Delete ${entityLabel}`}
                      name="close"
                      onClick={() => {
                        setItemToDelete(item);
                        setShowDeleteModal(true);
                      }}
                    />
                  )}
                  {canEdit && (
                    <StyledIconButton
                      name="edit"
                      data-testid={`edit-${labelKey}`}
                      title={`Edit ${entityLabel}`}
                      onClick={() => {
                        setEditId(item.id);
                        setEditValue(item.value);
                      }}
                    />
                  )}
                </div>
              </>
            )}
          </DataRow>
        );
      })}
      <BootstrapModalConfirm
        showModal={showDeleteModal}
        title={`Are you sure you want to delete this ${entityLabel}?`}
        onConfirm={() => handleDelete(itemToDelete.id)}
        onCancel={() => setShowDeleteModal(false)}
        cancelButtonDisabled={busy.deleting}
        confirmButtonDisabled={busy.deleting}>
        {busy.deleting ? (
          <Spinner text="Deleting..." />
        ) : (
          <>
            <div>
              You are about to delete this {entityLabel}: {itemToDelete.value}
            </div>
            {renderDeleteWarning && <div>{renderDeleteWarning(itemToDelete)}</div>}
          </>
        )}
      </BootstrapModalConfirm>
    </SectionComponent>
  );
};

export default NameOnlyEntityManager;
