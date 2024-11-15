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

import UserNotification from 'util/UserNotification';
import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';

export type ClientCertFormValues = {
  principal : string,
  role : string,
  password : string,
  lifetimeValue: number,
  lifetimeUnit: string,
}
export type CreateClientCertRequest = {
  principal : string,
  role : string,
  password : string,
  certificate_lifetime: string,
}

export type ClientCertCreateResponse = {
  principal: string,
  role: string,
  ca_certificate: string,
  private_key: string,
  certificate: string,
}
const createClientCa = (clientCertFormData: CreateClientCertRequest) => fetch<ClientCertCreateResponse>(
  'POST',
  qualifyUrl('ca/clientcert'),
  clientCertFormData,
  false,
);

const useCreateDataNodeClientCert = (): {
  onCreateClientCert: (values: CreateClientCertRequest) => Promise<ClientCertCreateResponse>,
  isLoading: boolean,
  isError: boolean,
  error: Error,
} => {
  const { mutateAsync: onTriggerNextState, isLoading, error, isError } = useMutation(createClientCa, {
    onError: (err: Error) => UserNotification.error(err.message),
  });

  return {
    onCreateClientCert: onTriggerNextState,
    isLoading,
    isError,
    error,
  };
};

export default useCreateDataNodeClientCert;
