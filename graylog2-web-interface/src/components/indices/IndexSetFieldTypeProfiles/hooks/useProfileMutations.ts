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
import { useMutation, useQueryClient } from '@tanstack/react-query';

import { qualifyUrl } from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import UserNotification from 'util/UserNotification';
import type {
  IndexSetFieldTypeProfileForm,
  IndexSetFieldTypeProfileRequestJson,
} from 'components/indices/IndexSetFieldTypeProfiles/types';

export const urlPrefix = '/system/indices/index_sets/profiles';

const putProfile = async ({ profile, id }: { profile: IndexSetFieldTypeProfileForm, id: string }) => {
  const url = qualifyUrl(urlPrefix);
  const body: IndexSetFieldTypeProfileRequestJson = {
    id,
    name: profile.name,
    description: profile.description,
    custom_field_mappings: profile.customFieldMappings,
  };

  return fetch('PUT', url, body);
};

const postProfile = async (profile: IndexSetFieldTypeProfileForm) => {
  const url = qualifyUrl(urlPrefix);
  const body: IndexSetFieldTypeProfileRequestJson = {
    name: profile.name,
    description: profile.description,
    custom_field_mappings: profile.customFieldMappings,
  };

  return fetch('POST', url, body);
};

const deleteProfile = async (id: string) => {
  const url = qualifyUrl(`${urlPrefix}/${id}`);

  return fetch('DELETE', url);
};

const useProfileMutation = () => {
  const queryClient = useQueryClient();

  const post = useMutation(postProfile, {
    onError: (errorThrown) => {
      UserNotification.error(`Creating index set field type profile failed with status: ${errorThrown}`,
        'Could not create index set field type profile');
    },
    onSuccess: () => {
      UserNotification.success('Index set field type profile has been successfully created.', 'Success!');

      return queryClient.refetchQueries({ queryKey: ['indexSetFieldTypeProfiles'], type: 'active' });
    },
  });
  const put = useMutation(putProfile, {
    onError: (errorThrown) => {
      UserNotification.error(`Updating index set field type profile failed with status: ${errorThrown}`,
        'Could not update index set field type profile');
    },
    onSuccess: () => {
      UserNotification.success('Index set field type profile has been successfully updated.', 'Success!');

      return queryClient.refetchQueries({ queryKey: ['indexSetFieldTypeProfiles'], type: 'active' });
    },
  });
  const remove = useMutation(deleteProfile, {
    onError: (errorThrown) => {
      UserNotification.error(`Deleting index set field type profile failed with status: ${errorThrown}`,
        'Could not delete index set field type profile');
    },
    onSuccess: () => {
      UserNotification.success('Index set field type profile has been successfully deleted.', 'Success!');

      return queryClient.refetchQueries({ queryKey: ['indexSetFieldTypeProfiles'], type: 'active' });
    },
  });

  return ({
    editProfile: put.mutateAsync,
    isEditLoading: put.isLoading,
    createProfile: post.mutateAsync,
    isCreateLoading: post.isLoading,
    isLoading: post.mutateAsync || post.isLoading || remove.isLoading,
    deleteProfile: remove.mutateAsync,
  });
};

export default useProfileMutation;
