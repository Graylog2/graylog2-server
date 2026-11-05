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
import { useTranslation } from 'react-i18next';

import i18n from 'i18n';
import Icon from 'components/common/Icon';
import { fetchMultiPartFormData } from 'logic/rest/FetchProvider';
import UserNotification from 'util/UserNotification';
import { Input, FormikInput, Space } from 'preflight/components/common';
import Button from 'components/bootstrap/Button';
import { qualifyUrl } from 'util/URLUtils';
import { QUERY_KEY as DATA_NODES_CA_QUERY_KEY } from 'preflight/hooks/useDataNodesCA';
import UnsecureConnectionAlert from 'preflight/components/ConfigurationWizard/UnsecureConnectionAlert';
import Dropzone from 'components/common/Dropzone';

type FormValues = {
  files?: Array<File>;
  password?: string;
};

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
    errors = { ...errors, files: i18n.t('ca_upload.validation_missing_file', { ns: 'preflight' }) };
  }

  if (formValues.files?.length > 1) {
    errors = { ...errors, files: i18n.t('ca_upload.validation_multiple_files', { ns: 'preflight' }) };
  }

  return errors;
};

const Explanation = styled.p`
  margin-bottom: 10px;
`;

const CAUpload = () => {
  const { t } = useTranslation('preflight');
  const queryClient = useQueryClient();
  const onRejectUpload = useCallback(() => {
    UserNotification.error(t('ca_upload.rejected'));
  }, [t]);

  const { mutateAsync: onProcessUpload, isPending: isLoading } = useMutation({
    mutationFn: submitUpload,

    onSuccess: () => {
      UserNotification.success(t('ca_upload.success'));
      queryClient.invalidateQueries({ queryKey: DATA_NODES_CA_QUERY_KEY });
    },

    onError: (error) => {
      UserNotification.error(t('ca_upload.error', { error: String(error) }));
    },
  });

  const onSubmit = useCallback(
    (formValues: FormValues) => onProcessUpload(formValues).catch(() => {}),
    [onProcessUpload],
  );

  return (
    <Formik<FormValues> initialValues={{}} onSubmit={onSubmit} validate={validate}>
      {({ isSubmitting, isValid }) => (
        <Form>
          <Explanation>{t('ca_upload.explanation')}</Explanation>
          <Field name="files">
            {({ field: { name, onChange, value }, meta: { error } }) => (
              <>
                <Input.Label required htmlFor="ca-dropzone">
                  {t('ca_upload.certificate_authority')}
                </Input.Label>
                <CADropzone
                  onDrop={(files) => onChange({ target: { name, value: files } })}
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
                    <div>{t('ca_upload.drop_hint')}</div>
                  </DropzoneInner>
                </CADropzone>
                <Files>
                  {value
                    ?.filter((file) => !!file)
                    .map(({ name: fileName }, index) => (
                      <File key={fileName}>
                        <Icon name="draft" /> {fileName}{' '}
                        <DeleteIcon
                          name="cancel"
                          onClick={() => {
                            const newValue = value.filter((_ignored, idx) => idx !== index);
                            onChange({ target: { name, value: newValue } });
                          }}
                        />
                      </File>
                    ))}
                </Files>
                {error && <Input.Error>{error}</Input.Error>}
              </>
            )}
          </Field>

          <FormikInput
            placeholder={t('ca_upload.password')}
            name="password"
            type="password"
            label={t('ca_upload.password')}
          />
          <UnsecureConnectionAlert renderIfSecure={<Space h="md" />} />
          <Button disabled={!isValid} type="submit" bsStyle="info">
            {isSubmitting ? t('ca_upload.uploading') : t('ca_upload.upload')}
          </Button>
        </Form>
      )}
    </Formik>
  );
};

export default CAUpload;
