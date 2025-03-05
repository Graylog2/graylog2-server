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
import { ConfirmDialog } from 'components/common';
import useDeleteTokenMutation from 'components/users/UsersTokenManagement/hooks/useDeleteTokenMutation';

type Props = {userId: string, tokenId: string, tokenName: string}

const TokenActions = ({ userId, tokenId, tokenName}: Props) => {
  const { deleteToken } = useDeleteTokenMutation(userId, tokenId);
  const [showDeleteDialog, setShowDeleteDialog] = useState<boolean>(false);

  const cancelDelete = () => {
    setShowDeleteDialog(false);
  };

  const handleDelete = () => {
    deleteToken().then(() => {
      setShowDeleteDialog(false);
    });
  };

  const onDelete = () => {
    setShowDeleteDialog(true);
  };

  return (
    <>
      {showDeleteDialog && (
        <ConfirmDialog
          show={showDeleteDialog}
          title="Deleting token"
          onCancel={cancelDelete}
          onConfirm={handleDelete}>
          <p>You are about to delete the token: &quot;{tokenName}&quot;. Are you sure?</p>
        </ConfirmDialog>
      )}

      <ButtonToolbar>
        <Button bsSize="xs" bsStyle="danger" onClick={onDelete}>
          Delete
        </Button>
      </ButtonToolbar>
    </>
  );
};

export default TokenActions;
