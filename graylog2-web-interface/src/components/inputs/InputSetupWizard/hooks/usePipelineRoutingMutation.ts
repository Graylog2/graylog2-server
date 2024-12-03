import { useMutation } from '@tanstack/react-query';

import { qualifyUrl } from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import UserNotification from 'util/UserNotification';
import ApiRoutes from 'routing/ApiRoutes';

type RoutingParams = {
  stream_id?: string,
  input_id: string,
}

const putRouting = async (params : RoutingParams) => {
  const url = qualifyUrl(ApiRoutes.PipelinesController.updateRouting().url);

  return fetch('PUT', url, { ...params, remove_from_default: false }); // TODO: remove remove_from_default after API change
};

const usePipelineRoutingMutation = () => {
  const update = useMutation(putRouting, {
    onError: (errorThrown) => {
      UserNotification.error(`Updating routing failed with status: ${errorThrown}`,
        'Could not update routing');
    },
    onSuccess: () => {
      UserNotification.success('Routing has succesfully been updated', 'Success!');
    },
  });

  return ({
    updateRouting: update.mutateAsync,
  });
};

export default usePipelineRoutingMutation;
