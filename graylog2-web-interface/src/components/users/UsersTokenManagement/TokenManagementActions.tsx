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
import React, { useState } from 'react';

import { Button, ButtonToolbar } from 'components/bootstrap';
import { ConfirmDialog, IfPermitted } from 'components/common';
import useDeleteTokenMutation from 'components/users/UsersTokenManagement/hooks/useDeleteTokenMutation';

type Props = {
  userId: string;
  username: string;
  tokenId: string;
  tokenName: string;
  onDeleteCallback?: () => void;
};

const TokenActions = ({ userId, username, tokenId, tokenName, onDeleteCallback = () => {} }: Props) => {
  const { deleteToken } = useDeleteTokenMutation(userId, tokenId);
  const [showDeleteDialog, setShowDeleteDialog] = useState<boolean>(false);
  const [isDeleting, setIsDeleting] = useState<boolean>(false);

  const cancelDelete = () => {
    setShowDeleteDialog(false);
  };

  const handleDelete = () => {
    setIsDeleting(true);

    deleteToken().then(() => {
      setShowDeleteDialog(false);
      setIsDeleting(false);
      onDeleteCallback();
    });
  };

  const onDelete = () => {
    setShowDeleteDialog(true);
  };

  return (
    <IfPermitted permissions={[`users:tokenremove:${username}`]}>
      {showDeleteDialog && (
        <ConfirmDialog show={showDeleteDialog} title="Deleting token" onCancel={cancelDelete} onConfirm={handleDelete}>
          <p>You are about to delete the token: &quot;{tokenName}&quot;. Are you sure?</p>
        </ConfirmDialog>
      )}

      <ButtonToolbar className="pull-right">
        <Button bsSize="xs" disabled={isDeleting} bsStyle="danger" onClick={onDelete}>
          Delete
        </Button>
      </ButtonToolbar>
    </IfPermitted>
  );
};

export default TokenActions;
