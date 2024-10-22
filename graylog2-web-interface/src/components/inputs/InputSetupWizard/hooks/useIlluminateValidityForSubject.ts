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
import { useQuery } from '@tanstack/react-query';

import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import UserNotification from 'util/UserNotification';
import type FetchError from 'logic/errors/FetchError';
import type { InputSetupWizardCategory, InputSetupWizardSubcategory } from 'components/inputs/InputSetupWizard';

export const ILLUMINATE_CATEGORY_VALIDITY_KEY = 'illuminate_category_validity';

const INITIAL_DATA = {
  category_valid: true,
  subcategory_valid: true,
};

const getIlluminateValidityForSubject = (category: InputSetupWizardCategory, subcategory: InputSetupWizardSubcategory) => fetch(
  'GET',
  qualifyUrl(`TODO_foo?category=${category}&subcategory=${subcategory}`),
);

type IlluminateCategoryValidity = {
  category_valid: boolean,
  subcategory_valid: boolean,
}

const useIlluminateValidityForSubject = (category: InputSetupWizardCategory, subcategory: InputSetupWizardSubcategory): {
  data: IlluminateCategoryValidity,
  isFetching: boolean,
  isError: boolean,
  refetch: () => void
} => {
  const { data, isFetching, isError, refetch } = useQuery<IlluminateCategoryValidity, FetchError>([ILLUMINATE_CATEGORY_VALIDITY_KEY, category, subcategory],
    () => getIlluminateValidityForSubject(category, subcategory),
    {
      enabled: !!category || !!subcategory,
      onError: (errorThrown) => {
        if (!(errorThrown.status === 404)) {
          UserNotification.error(`Loading illuminate validity for category ${category} and subcategory ${subcategory} failed with: ${errorThrown}`);
        }
      },
      retry: 0,
    });

  return ({
    data: data ?? INITIAL_DATA,
    isFetching,
    isError,
    refetch,
  });
};

export default useIlluminateValidityForSubject;
