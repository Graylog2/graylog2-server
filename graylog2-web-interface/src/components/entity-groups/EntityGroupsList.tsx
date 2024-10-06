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
import styled from 'styled-components';

import { IconButton } from 'components/common';
import { Button, BootstrapModalConfirm } from 'components/bootstrap';
import type { EntityGroupsListResponse } from 'components/entity-groups/Types';
import { useCreateEntityGroup, useUpdateEntityGroup, useDeleteEntityGroup } from 'components/entity-groups/hooks/useEntityGroups';

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

const StyledInput = styled.input`
  color: #fff;
  background-color: #303030;
  padding: 6px 12px;
  border: 1px solid #525252;

  &:focus {
    border: 1px solid #5082bc;
    box-shadow: inset 0 1px 1px rgb(0 0 0 / 8%), 0 0 8px rgb(80 130 188 / 40%);
  }
`;

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

type Props = {
  entityGroups: EntityGroupsListResponse[];
  showAddEntityGroup: boolean;
  setShowAddEntityGroup: (boolean) => void;
}

const EntityGroupsList = ({ entityGroups, showAddEntityGroup, setShowAddEntityGroup }: Props) => {
  const [editId, setEditId] = React.useState('');
  const [editValue, setEditValue] = React.useState('');
  const [newEntityGroupTagValue, setNewEntityGroupTagValue] = React.useState('');
  const [showDeleteModal, setShowDeleteModal] = React.useState(false);
  const [entityGroupToDelete, setEntityGroupToDelete] = React.useState({ id: '', value: '' });

  const { createEntityGroup } = useCreateEntityGroup();
  const { updateEntityGroup } = useUpdateEntityGroup();
  const { deleteEntityGroup } = useDeleteEntityGroup();

  const onAddEntityGroup = async () => {
    await createEntityGroup({ name: newEntityGroupTagValue });

    setNewEntityGroupTagValue('');
    setShowAddEntityGroup(false);
  };

  const resetAddValues = () => {
    setNewEntityGroupTagValue('');
    setShowAddEntityGroup(false);
  };

  const handleAddKeyDown = (e: React.KeyboardEvent) => {
    if (!newEntityGroupTagValue) return;

    if (e.key === 'Enter') {
      onAddEntityGroup();
    }

    if (e.key === 'Escape') {
      resetAddValues();
    }
  };

  const onEditEntityGroup = async () => {
    await updateEntityGroup({ id: editId, requestObj: { name: editValue } });

    setEditId('');
    setEditValue('');
  };

  const resetEditValues = () => {
    setEditId('');
    setEditValue('');
  };

  const handleEditKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      onEditEntityGroup();
    }

    if (e.key === 'Escape') {
      resetEditValues();
    }
  };

  const onDeleteEntityGroup = async () => {
    await deleteEntityGroup({ entityGroupId: entityGroupToDelete.id });

    setShowDeleteModal(false);
    setEntityGroupToDelete({ id: '', value: '' });
  };

  return (
    <>
      {showAddEntityGroup && (
        <DataRow key="new-entity-group">
          <div style={{ margin: '0' }}>
            <StyledInput id="add-entity-group-input"
                         data-testid="new-entity-group-input"
                         type="text"
                         autoComplete="off"
                         style={{ marginBottom: '0px', paddingRight: '5px' }}
                         value={newEntityGroupTagValue}
                         onChange={(e: React.BaseSyntheticEvent) => setNewEntityGroupTagValue(e.target.value)}
                         onKeyDown={handleAddKeyDown} />
          </div>
          <div>
            <CancelButton onClick={() => resetAddValues()}>
              Cancel
            </CancelButton>
            <StyledButton bsStyle="success"
                          data-testid="save-edit-entity-group"
                          disabled={!newEntityGroupTagValue || false}
                          onClick={onAddEntityGroup}>
              Add
            </StyledButton>
          </div>
        </DataRow>
      )}
      {entityGroups.map((entityGroup) => {
        const isCurrentlyEditing = entityGroup.id === editId;

        return (
          <DataRow key={entityGroup.id}>
            {isCurrentlyEditing ? (
              <>
                <div style={{ margin: '0' }}>
                  <StyledInput id="edit-entity-group-input"
                               data-testid="entity-group-input"
                               type="text"
                               autoComplete="off"
                               style={{ marginBottom: '0px', paddingRight: '5px' }}
                               value={editValue}
                               onChange={(e: React.BaseSyntheticEvent) => setEditValue(e.target.value)}
                               onKeyDown={handleEditKeyDown} />
                </div>
                <div>
                  <CancelButton onClick={() => resetEditValues()}>
                    Cancel
                  </CancelButton>
                  <StyledButton bsStyle="success"
                                data-testid="save-edit-entity-group"
                                disabled={false}
                                onClick={onEditEntityGroup}>
                    Save
                  </StyledButton>
                </div>
              </>
            ) : (
              <>
                <div style={{ display: 'flex' }}>
                  <div>{entityGroup.name}</div>
                </div>
                <div>
                  <StyledIconButton data-testid="delete-entity-group"
                                    title="Delete entity group"
                                    name="close"
                                    onClick={() => {
                                      setEntityGroupToDelete({ id: entityGroup.id, value: entityGroup.name });
                                      setShowDeleteModal(true);
                                    }} />
                  <StyledIconButton name="edit"
                                    data-testid="edit-category"
                                    title="Edit category"
                                    onClick={() => {
                                      setEditId(entityGroup.id);
                                      setEditValue(entityGroup.name);
                                    }} />
                </div>
              </>
            )}
            <BootstrapModalConfirm showModal={showDeleteModal}
                                   title="Are you sure you want to delete this category?"
                                   onConfirm={() => onDeleteEntityGroup()}
                                   onCancel={() => setShowDeleteModal(false)}>
              <div>You are about to delete this tag: {entityGroupToDelete.value}</div>
            </BootstrapModalConfirm>
          </DataRow>
        );
      })}
    </>
  );
};

export default EntityGroupsList;
