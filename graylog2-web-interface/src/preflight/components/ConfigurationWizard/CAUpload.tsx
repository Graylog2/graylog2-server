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
import * as React from 'react';
import styled from 'styled-components';
import { useCallback } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Formik, Form, Field } from 'formik';

import Icon from 'components/common/Icon';
import { fetchMultiPartFormData } from 'logic/rest/FetchProvider';
import UserNotification from 'preflight/util/UserNotification';
import { Input, FormikInput, Button, Space } from 'preflight/components/common';
import { qualifyUrl } from 'util/URLUtils';
import { QUERY_KEY as DATA_NODES_CA_QUERY_KEY } from 'preflight/hooks/useDataNodesCA';
import UnsecureConnectionAlert from 'preflight/components/ConfigurationWizard/UnsecureConnectionAlert';
import Dropzone from 'components/common/Dropzone';

type FormValues = {
  files?: Array<File>,
  password?: string,
}

const CADropzone = styled(Dropzone)`
  height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
`;

const DropzoneInner = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
`;

const Files = styled.div`
  margin-top: 5px;
  margin-bottom: 5px;
`;

const File = styled.div`
  gap: 5px;
  display: flex;
  align-items: center;
`;

const DeleteIcon = styled(Icon)`
  cursor: pointer;
`;

const submitUpload = (formValues: FormValues) => {
  const formData = new FormData();

  if (formValues.password !== undefined) {
    formData.append('password', formValues.password);
  }

  formValues.files.forEach((file) => formData.append('files', file));

  return fetchMultiPartFormData(qualifyUrl('/api/ca/upload'), formData, false);
};

const validate = (formValues: FormValues) => {
  let errors = {};

  if (!formValues.files?.length) {
    errors = { ...errors, files: 'Please upload a file.' };
  }

  if (formValues.files?.length > 1) {
    errors = { ...errors, files: 'Please upload only a single file.' };
  }

  return errors;
};

const Explanation = styled.p`
  margin-bottom: 10px;
`;

const CAUpload = () => {
  const queryClient = useQueryClient();
  const onRejectUpload = useCallback(() => {
    UserNotification.error('CA upload failed');
  }, []);

  const { mutateAsync: onProcessUpload, isLoading } = useMutation(submitUpload, {
    onSuccess: () => {
      UserNotification.success('CA uploaded successfully');
      queryClient.invalidateQueries(DATA_NODES_CA_QUERY_KEY);
    },
    onError: (error) => {
      UserNotification.error(`CA upload failed with error: ${error}`);
    },
  });

  const onSubmit = useCallback((formValues: FormValues) => onProcessUpload(formValues).catch(() => {}), [onProcessUpload]);

  return (
    <Formik<FormValues> initialValues={{}} onSubmit={onSubmit} validate={validate}>
      {({ isSubmitting, isValid }) => (
        <Form>
          <Explanation>
            Here you can upload your existing CA. You need to upload a single file containing both private key
            (encrypted or unencrypted), the CA certificate as well as any intermediate certificates. The file can be in PEM
            or in PKCS#12 format. If your private key is encrypted, you also need to supply its password.
          </Explanation>
          <Field name="files">
            {({ field: { name, onChange, value }, meta: { error } }) => (
              <>
                <Input.Label required htmlFor="ca-dropzone">Certificate Authority</Input.Label>
                <CADropzone onDrop={(files) => onChange({ target: { name, value: files } })}
                            onReject={onRejectUpload}
                            data-testid="upload-dropzone"
                            loading={isLoading}>
                  <DropzoneInner>
                    <Dropzone.Accept>
                      <Icon name="draft" type="solid" size="2x" />
                    </Dropzone.Accept>
                    <Dropzone.Reject>
                      <Icon name="warning" size="2x" />
                    </Dropzone.Reject>
                    <Dropzone.Idle>
                      <Icon name="draft" type="regular" size="2x" />
                    </Dropzone.Idle>
                    <div>
                      Drag CA here or click to select file
                    </div>
                  </DropzoneInner>
                </CADropzone>
                <Files>
                  {value?.filter((file) => !!file).map(({ name: fileName }, index) => (
                    <File key={fileName}>
                      <Icon name="draft" /> {fileName} <DeleteIcon name="cancel"
                                                                   onClick={() => {
                                                                     const newValue = value.filter((_ignored, idx) => idx !== index);
                                                                     onChange({ target: { name, value: newValue } });
                                                                   }} />
                    </File>
                  ))}
                </Files>
                {error && <Input.Error>{error}</Input.Error>}
              </>
            )}
          </Field>

          <FormikInput placeholder="Password"
                       name="password"
                       type="password"
                       label="Password" />
          <UnsecureConnectionAlert renderIfSecure={<Space h="md" />} />
          <Button disabled={!isValid} type="submit">
            {isSubmitting ? 'Uploading CA...' : 'Upload CA'}
          </Button>
        </Form>
      )}
    </Formik>
  );
};

export default CAUpload;
