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

import i18n from 'i18n';
import { qualifyUrl } from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import UserNotification from 'util/UserNotification';
import type {
  IndexSetFieldTypeProfileForm,
  IndexSetFieldTypeProfileRequestJson,
} from 'components/indices/IndexSetFieldTypeProfiles/types';

export const urlPrefix = '/system/indices/index_sets/profiles';

const putProfile = async ({ profile, id }: { profile: IndexSetFieldTypeProfileForm; id: string }) => {
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

const deleteProfile = async (id: string) => SystemIndexSetsFieldTypeProfiles.remove(id);

const useProfileMutation = () => {
  const queryClient = useQueryClient();

  const post = useMutation({
    mutationFn: postProfile,

    onError: (errorThrown) => {
      UserNotification.error(
        i18n.t('hooks.profile_create_failed', { ns: 'indexsets', error: errorThrown }),
        i18n.t('hooks.profile_create_failed_title', { ns: 'indexsets' }),
      );
    },

    onSuccess: () => {
      UserNotification.success(
        i18n.t('hooks.profile_create_success', { ns: 'indexsets' }),
        i18n.t('hooks.success_title', { ns: 'indexsets' }),
      );

      return queryClient.refetchQueries({ queryKey: ['indexSetFieldTypeProfiles'], type: 'active' });
    },
  });
  const put = useMutation({
    mutationFn: putProfile,

    onError: (errorThrown) => {
      UserNotification.error(
        i18n.t('hooks.profile_update_failed', { ns: 'indexsets', error: errorThrown }),
        i18n.t('hooks.profile_update_failed_title', { ns: 'indexsets' }),
      );
    },

    onSuccess: () => {
      UserNotification.success(
        i18n.t('hooks.profile_update_success', { ns: 'indexsets' }),
        i18n.t('hooks.success_title', { ns: 'indexsets' }),
      );

      return queryClient.refetchQueries({ queryKey: ['indexSetFieldTypeProfiles'], type: 'active' });
    },
  });
  const remove = useMutation({
    mutationFn: deleteProfile,

    onError: (errorThrown) => {
      UserNotification.error(
        i18n.t('hooks.profile_delete_failed', { ns: 'indexsets', error: errorThrown }),
        i18n.t('hooks.profile_delete_failed_title', { ns: 'indexsets' }),
      );
    },

    onSuccess: () => {
      UserNotification.success(
        i18n.t('hooks.profile_delete_success', { ns: 'indexsets' }),
        i18n.t('hooks.success_title', { ns: 'indexsets' }),
      );

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
