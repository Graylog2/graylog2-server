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

import { useMutation } from '@tanstack/react-query';

import i18n from 'i18n';
import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import UserNotification from 'util/UserNotification';
import type { ChangeFieldTypeBody, ChangeFieldTypeBodyJson } from 'views/logic/fieldactions/ChangeFieldType/types';

export const urlPrefix = '/system/indices/mappings';

const putFieldType = async ({ indexSetSelection, newFieldType, rotated, field }: ChangeFieldTypeBody) => {
  const url = qualifyUrl(urlPrefix);
  const body: ChangeFieldTypeBodyJson = {
    index_sets: indexSetSelection,
    type: newFieldType,
    rotate: rotated,
    field: field,
  };

  return fetch('PUT', url, body);
};

const usePutFieldTypeMutation = () => {
  const put = useMutation({
    mutationFn: putFieldType,

    onError: (errorThrown) => {
      UserNotification.error(
        i18n.t('change_field_type.change_failed', { ns: 'search', error: errorThrown }),
        i18n.t('change_field_type.change_failed_title', { ns: 'search' }),
      );
    },

    onSuccess: () => {
      UserNotification.success(
        i18n.t('change_field_type.change_success', { ns: 'search' }),
        i18n.t('change_field_type.success_title', { ns: 'search' }),
      );
    },
  });

  return { putFieldTypeMutation: put.mutateAsync, isLoading: put.isPending };
};

export default usePutFieldTypeMutation;
