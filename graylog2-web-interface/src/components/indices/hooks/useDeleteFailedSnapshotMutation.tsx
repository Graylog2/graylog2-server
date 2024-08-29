import { useMutation } from '@tanstack/react-query';

import ApiRoutes from 'routing/ApiRoutes';
import { qualifyUrl } from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import UserNotification from 'util/UserNotification';

const deleteLicense = async (id : string) => {
  const url = qualifyUrl(ApiRoutes.IndexSetsApiController.deleteFailedSnapshot(id).url);

  return fetch('DELETE', url);
};

const useDeleteFailedSnapshotMutation = (id: string) => {
  const remove = useMutation(() => deleteLicense(id), {
    onError: (errorThrown) => {
      UserNotification.error(`Snapshot deletion failed: ${errorThrown}`,
        'Could not delete snapshot');
    },
    onSuccess: () => {
      UserNotification.success('Snapshot has been successfully deleted.', 'Success!');
    },
  });

  return { deleteFailedSnapshot: remove.mutateAsync };
};

export default useDeleteFailedSnapshotMutation;
