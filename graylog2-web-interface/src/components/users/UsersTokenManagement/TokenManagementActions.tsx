import React, { useState } from 'react';

import { Button, ButtonToolbar } from 'components/bootstrap';
import { ConfirmDialog } from 'components/common';
import useDeleteTokenMutation from 'components/users/UsersTokenManagement/hooks/useDeleteTokenMutation';

type Props = {userId: string, tokenName: string}

const TokenActions = ({ userId, tokenName }: Props) => {
  const { deleteToken } = useDeleteTokenMutation(userId, tokenName);
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
