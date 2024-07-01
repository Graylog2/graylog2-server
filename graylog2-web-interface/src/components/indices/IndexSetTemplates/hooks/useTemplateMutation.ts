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
  IndexSetTemplate,
} from 'components/indices/IndexSetTemplates/types';

export const urlPrefix = '/system/indices/index_sets/templates';

const putTemplate = async ({ template, id }: { template: IndexSetTemplate, id: string }) => {
  const url = qualifyUrl(`${urlPrefix}/${id}`);
  const body: Omit<IndexSetTemplate, 'id' | 'built_in' | 'default' | 'enabled' | 'disabled_reason'> = {
    title: template.title,
    description: template.description,
    index_set_config: template.index_set_config,
  };

  return fetch('PUT', url, body);
};

const putTemplateDefault = async (id : string) => {
  const url = qualifyUrl('/system/indices/index_set_defaults');
  const body: Pick<IndexSetTemplate, 'id'> = {
    id,
  };

  return fetch('PUT', url, body);
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

const deleteProfile = async (id: string) => {
  const url = qualifyUrl(`${urlPrefix}/${id}`);

  return fetch('DELETE', url);
};

const useTemplate = () => {
  const queryClient = useQueryClient();

  const post = useMutation(postTemplate, {
    onError: (errorThrown) => {
      UserNotification.error(`Creating index set template failed with status: ${errorThrown}`,
        'Could not create index set template');
    },
    onSuccess: () => {
      UserNotification.success('Index set template has been successfully created.', 'Success!');

      return queryClient.refetchQueries({ queryKey: ['indexSetTemplates'], type: 'active' });
    },
  });

  const put = useMutation(putTemplate, {
    onError: (errorThrown) => {
      UserNotification.error(`Updating index set template failed with status: ${errorThrown}`,
        'Could not update index set template');
    },
    onSuccess: () => {
      UserNotification.success('Index set template has been successfully updated.', 'Success!');
      queryClient.invalidateQueries(['indexSetTemplate']);

      return queryClient.refetchQueries({ queryKey: ['indexSetTemplates'], type: 'active' });
    },
  });

  const setAsDefault = useMutation(putTemplateDefault, {
    onError: (errorThrown) => {
      UserNotification.error(`Setting template as default failed with status: ${errorThrown}`,
        'Could set template as default');
    },
    onSuccess: () => {
      UserNotification.success('Template has successfully been set as default.', 'Success!');

      return queryClient.refetchQueries({ queryKey: ['indexSetTemplates'], type: 'active' });
    },
  });

  const remove = useMutation(deleteProfile, {
    onError: (errorThrown) => {
      UserNotification.error(`Deleting index set template failed with status: ${errorThrown}`,
        'Could not delete index set template');
    },
    onSuccess: () => {
      UserNotification.success('Index set template has been successfully deleted.', 'Success!');

      return queryClient.refetchQueries({ queryKey: ['indexSetTemplates'], type: 'active' });
    },
  });

  return ({
    updateTemplate: put.mutateAsync,
    isEditLoading: put.isLoading,
    createTemplate: post.mutateAsync,
    isCreateLoading: post.isLoading,
    isLoading: post.mutateAsync || post.isLoading || remove.isLoading,
    deleteTemplate: remove.mutateAsync,
    setAsDefault: setAsDefault.mutateAsync,
  });
};

export default useTemplate;
