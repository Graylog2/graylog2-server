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

import { SystemIndexSetDefaults, SystemIndexSetsTemplates } from '@graylog/server-api';

import i18n from 'i18n';
import { qualifyUrl } from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import UserNotification from 'util/UserNotification';
import type { IndexSetTemplate } from 'components/indices/IndexSetTemplates/types';

export const urlPrefix = '/system/indices/index_sets/templates';

const putTemplate = async ({ template, id }: { template: IndexSetTemplate; id: string }) => {
  const url = qualifyUrl(`${urlPrefix}/${id}`);
  const body: Omit<IndexSetTemplate, 'id' | 'built_in' | 'default' | 'enabled' | 'disabled_reason'> = {
    title: template.title,
    description: template.description,
    index_set_config: template.index_set_config,
  };

  return fetch('PUT', url, body);
};

const putTemplateDefault = async (id: string) => {
  const body: Pick<IndexSetTemplate, 'id'> = {
    id,
  };

  return SystemIndexSetDefaults.update(body);
};

const postTemplate = async (template: IndexSetTemplate) => {
  const url = qualifyUrl(urlPrefix);
  const body: Omit<IndexSetTemplate, 'id' | 'built_in' | 'default' | 'enabled' | 'disabled_reason'> = {
    title: template.title,
    description: template.description,
    index_set_config: template.index_set_config,
  };

  return fetch('POST', url, body);
};

const deleteProfile = async (id: string) => SystemIndexSetsTemplates.remove(id);

const useTemplate = () => {
  const queryClient = useQueryClient();

  const post = useMutation({
    mutationFn: postTemplate,

    onError: (errorThrown) => {
      UserNotification.error(
        i18n.t('hooks.template_create_failed', { ns: 'indexsets', error: errorThrown }),
        i18n.t('hooks.template_create_failed_title', { ns: 'indexsets' }),
      );
    },

    onSuccess: () => {
      UserNotification.success(
        i18n.t('hooks.template_create_success', { ns: 'indexsets' }),
        i18n.t('hooks.success_title', { ns: 'indexsets' }),
      );

      return queryClient.refetchQueries({ queryKey: ['indexSetTemplates'], type: 'active' });
    },
  });

  const put = useMutation({
    mutationFn: putTemplate,

    onError: (errorThrown) => {
      UserNotification.error(
        i18n.t('hooks.template_update_failed', { ns: 'indexsets', error: errorThrown }),
        i18n.t('hooks.template_update_failed_title', { ns: 'indexsets' }),
      );
    },

    onSuccess: () => {
      UserNotification.success(
        i18n.t('hooks.template_update_success', { ns: 'indexsets' }),
        i18n.t('hooks.success_title', { ns: 'indexsets' }),
      );
      queryClient.invalidateQueries({
        queryKey: ['indexSetTemplate'],
      });

      return queryClient.refetchQueries({ queryKey: ['indexSetTemplates'], type: 'active' });
    },
  });

  const setAsDefault = useMutation({
    mutationFn: putTemplateDefault,

    onError: (errorThrown) => {
      UserNotification.error(
        i18n.t('hooks.template_default_failed', { ns: 'indexsets', error: errorThrown }),
        i18n.t('hooks.template_default_failed_title', { ns: 'indexsets' }),
      );
    },

    onSuccess: () => {
      UserNotification.success(
        i18n.t('hooks.template_default_success', { ns: 'indexsets' }),
        i18n.t('hooks.success_title', { ns: 'indexsets' }),
      );

      return queryClient.refetchQueries({ queryKey: ['indexSetTemplates'], type: 'active' });
    },
  });

  const remove = useMutation({
    mutationFn: deleteProfile,

    onError: (errorThrown) => {
      UserNotification.error(
        i18n.t('hooks.template_delete_failed', { ns: 'indexsets', error: errorThrown }),
        i18n.t('hooks.template_delete_failed_title', { ns: 'indexsets' }),
      );
    },

    onSuccess: () => {
      UserNotification.success(
        i18n.t('hooks.template_delete_success', { ns: 'indexsets' }),
        i18n.t('hooks.success_title', { ns: 'indexsets' }),
      );

      return queryClient.refetchQueries({ queryKey: ['indexSetTemplates'], type: 'active' });
    },
  });

  return {
    updateTemplate: put.mutateAsync,
    isEditLoading: put.isPending,
    createTemplate: post.mutateAsync,
    isCreateLoading: post.isPending,
    isLoading: post.mutateAsync || post.isPending || remove.isPending,
    deleteTemplate: remove.mutateAsync,
    setAsDefault: setAsDefault.mutateAsync,
  };
};

export default useTemplate;
