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

import { SystemIndexSetsFieldTypeProfiles } from '@graylog/server-api';

import UserNotification from 'util/UserNotification';
import type { IndexSetFieldTypeProfileForm } from 'components/indices/IndexSetFieldTypeProfiles/types';

const putProfile = async ({ profile, id }: { profile: IndexSetFieldTypeProfileForm; id: string }) =>
  SystemIndexSetsFieldTypeProfiles.update({
    id,
    name: profile.name,
    description: profile.description,
    custom_field_mappings: profile.customFieldMappings,
  });

const postProfile = async (profile: IndexSetFieldTypeProfileForm) =>
  SystemIndexSetsFieldTypeProfiles.create({
    name: profile.name,
    description: profile.description,
    custom_field_mappings: profile.customFieldMappings,
  });

const deleteProfile = async (id: string) => SystemIndexSetsFieldTypeProfiles.remove(id);

const useProfileMutation = () => {
  const queryClient = useQueryClient();

  const post = useMutation({
    mutationFn: postProfile,

    onError: (errorThrown) => {
      UserNotification.error(
        `Creating index set field type profile failed with status: ${errorThrown}`,
        'Could not create index set field type profile',
      );
    },

    onSuccess: () => {
      UserNotification.success('Index set field type profile has been successfully created.', 'Success!');

      return queryClient.refetchQueries({ queryKey: ['indexSetFieldTypeProfiles'], type: 'active' });
    },
  });
  const put = useMutation({
    mutationFn: putProfile,

    onError: (errorThrown) => {
      UserNotification.error(
        `Updating index set field type profile failed with status: ${errorThrown}`,
        'Could not update index set field type profile',
      );
    },

    onSuccess: () => {
      UserNotification.success('Index set field type profile has been successfully updated.', 'Success!');

      return queryClient.refetchQueries({ queryKey: ['indexSetFieldTypeProfiles'], type: 'active' });
    },
  });
  const remove = useMutation({
    mutationFn: deleteProfile,

    onError: (errorThrown) => {
      UserNotification.error(
        `Deleting index set field type profile failed with status: ${errorThrown}`,
        'Could not delete index set field type profile',
      );
    },

    onSuccess: () => {
      UserNotification.success('Index set field type profile has been successfully deleted.', 'Success!');

      return queryClient.refetchQueries({ queryKey: ['indexSetFieldTypeProfiles'], type: 'active' });
    },
  });

  return {
    editProfile: put.mutateAsync,
    isEditLoading: put.isPending,
    createProfile: post.mutateAsync,
    isCreateLoading: post.isPending,
    isLoading: post.mutateAsync || post.isPending || remove.isPending,
    deleteProfile: remove.mutateAsync,
  };
};

export default useProfileMutation;
