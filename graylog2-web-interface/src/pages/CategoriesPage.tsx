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

import { DocumentTitle, PageHeader, Spinner, IconButton } from 'components/common';
import { Row, Col, Button, BootstrapModalConfirm } from 'components/bootstrap';

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

const CategoriesPage = () => {
  const categoryList = [];
  const [editId, setEditId] = React.useState('');
  const [editValue, setEditValue] = React.useState('');
  const [showAddCategory, setShowAddCategory] = React.useState(false);
  const [newCategoryTagValue, setNewCategoryTagValue] = React.useState('');
  const [showDeleteModal, setShowDeleteModal] = React.useState(false);
  const [categoryToDelete, setCategoryToDelete] = React.useState({ id: '', value: '' });
  // const { updateCategory, updatingCategory } = useUpdateCategory();
  // const { deleteCategory } = useDeleteCategory();
  // const { addCategory, addingCategory } = useAddCategory();

  const onAddCategory = async () => {
    // await addCategory({ category: newCategoryTagValue });

    setNewCategoryTagValue('');
    setShowAddCategory(false);
  };

  const resetAddValues = () => {
    setNewCategoryTagValue('');
    setShowAddCategory(false);
  };

  const handleAddKeyDown = (e: React.KeyboardEvent) => {
    if (!newCategoryTagValue) return;

    if (e.key === 'Enter') {
      onAddCategory();
    }

    if (e.key === 'Escape') {
      resetAddValues();
    }
  };

  const onEditCategory = async () => {
    // await updateCategory({ categoryId: editId, category: editValue });

    setEditId('');
    setEditValue('');
  };

  const resetEditValues = () => {
    setEditId('');
    setEditValue('');
  };

  const handleEditKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      onEditCategory();
    }

    if (e.key === 'Escape') {
      resetEditValues();
    }
  };

  const onDeleteCategory = async (categoryId: string) => {
    // await deleteCategory({ categoryId });

    setShowDeleteModal(false);
    setCategoryToDelete({ id: '', value: '' });
  };

  return (
    <DocumentTitle title="Manage Categories">
      <PageHeader title="Manage Content Categories"
                  actions={(
                    <Button bsStyle="success">Create a new category</Button>
                )}>
        <span>
          Manage the tags/categories of your content.
        </span>
      </PageHeader>
      <Row className="content">
        <Col md={12}>
          {showAddCategory && (
            <DataRow key="new-category">
              <div style={{ margin: '0' }}>
                <StyledInput id="add-category-input"
                             data-testid="new-category-input"
                             type="text"
                             autoComplete="off"
                             style={{ marginBottom: '0px', paddingRight: '5px' }}
                             value={newCategoryTagValue}
                             onChange={(e: React.BaseSyntheticEvent) => setNewCategoryTagValue(e.target.value)}
                             onKeyDown={handleAddKeyDown} />
              </div>
              <div>
                <CancelButton onClick={() => resetAddValues()}>
                  Cancel
                </CancelButton>
                <StyledButton bsStyle="success"
                              data-testid="save-edit-category"
                              disabled={!newCategoryTagValue || false}
                              onClick={onAddCategory}>
                  Add
                </StyledButton>
              </div>
            </DataRow>
          )}
          {categoryList.map((category) => {
            const isCurrentlyEditing = category.id === editId;

            return (
              <DataRow key={category.id}>
                {isCurrentlyEditing ? (
                  <>
                    <div style={{ margin: '0' }}>
                      <StyledInput id="edit-category-input"
                                   data-testid="category-input"
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
                                    data-testid="save-edit-category"
                                    disabled={false}
                                    onClick={onEditCategory}>
                        Save
                      </StyledButton>
                    </div>
                  </>
                ) : (
                  <>
                    <div style={{ display: 'flex' }}>
                      <div>{category.value}</div>
                    </div>
                    <div>
                      <StyledIconButton data-testid="delete-category"
                                        title="Delete category"
                                        name="close"
                                        onClick={() => {
                                          setCategoryToDelete({ id: category.id, value: category.value });
                                          setShowDeleteModal(true);
                                        }} />
                      <StyledIconButton name="edit"
                                        data-testid="edit-category"
                                        title="Edit category"
                                        onClick={() => {
                                          setEditId(category.id);
                                          setEditValue(category.value);
                                        }} />
                    </div>
                  </>
                )}
                <BootstrapModalConfirm showModal={showDeleteModal}
                                       title="Are you sure you want to delete this category?"
                                       onConfirm={() => onDeleteCategory(categoryToDelete.id)}
                                       onCancel={() => setShowDeleteModal(false)}>
                  <div>You are about to delete this tag: {categoryToDelete.value}</div>
                </BootstrapModalConfirm>
              </DataRow>
            );
          })}
        </Col>
      </Row>
    </DocumentTitle>
  );
};

export default CategoriesPage;
