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

import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import UserNotification from 'util/UserNotification';
import type { ChangeFieldTypeBody } from 'views/logic/fieldactions/ChangeFieldType/types';
import { ChangeFieldTypeBodyJson } from 'views/logic/fieldactions/ChangeFieldType/types';

export const urlPrefix = '/system/indices/mappings';

const putFiledType = async ({
  indexSetSelection,
  newFieldType,
  rotated,
  field,
}: ChangeFieldTypeBody) => {
  const url = `${urlPrefix}`;
  const bodyJson = {
    index_sets_ids: indexSetSelection,
    new_type: newFieldType,
    rotate_immediately: rotated,
    field_name: field,
  };

  console.log({ bodyJson });

  return Promise.resolve();
  // return fetch<ChangeFieldTypeBodyJson>('PUT', qualifyUrl(url), body);
};

const usePutFiledTypeMutation = () => {
  const { mutateAsync, isLoading } = useMutation(putFiledType, {
    onError: (errorThrown) => {
      UserNotification.error(`Changing the field type failed with status: ${errorThrown}`,
        'Could not change the field type');
    },
    onSuccess: () => {
      UserNotification.success('The field type changed successfully');
    },
  });

  return { putFiledTypeMutation: mutateAsync, isLoading };
};

export default usePutFiledTypeMutation;
