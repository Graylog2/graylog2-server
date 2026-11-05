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
import React, { useContext, useEffect, useState } from 'react';
import styled from 'styled-components';
import { Trans, useTranslation } from 'react-i18next';

import { Button, Modal, Panel } from 'components/bootstrap';
import DocumentationLink from 'components/support/DocumentationLink';
import FormDataContext from 'integrations/contexts/FormDataContext';
import { ApiContext } from 'integrations/aws/context/Api';
import { SidebarContext } from 'integrations/aws/context/Sidebar';
import useFetch from 'integrations/hooks/useFetch';
import FormWrap from 'integrations/aws/common/FormWrap';
import ValidatedInput from 'integrations/aws/common/ValidatedInput';
import { ApiRoutes, DocsRoutes } from 'integrations/aws/common/Routes';
import { renderOptions } from 'integrations/aws/common/Options';
import formValidation from 'integrations/aws/utils/formValidation';
import Spinner from 'components/common/Spinner';
import useProductName from 'brand-customization/useProductName';
import { toAWSRequest } from 'integrations/aws/common/formDataAdapter';

import FormAdvancedOptions from '../FormAdvancedOptions';

const AutoSetupContent = styled.div`
  margin-bottom: 9px;
`;

const LoadingContent = styled(Modal.Body)`
  text-align: center;
`;

const StyledSpinner = styled(Spinner)`
  font-size: 48px;
  color: #702785;
`;

const LoadingMessage = styled.p`
  font-size: 16px;
  font-weight: bold;
  padding-top: 15px;
  color: #a6afbd;
`;

type KinesisStreamsProps = {
  onSubmit: (...args: any[]) => void;
  onChange: (...args: any[]) => void;
  toggleSetup?: (...args: any[]) => void;
};

const KinesisStreams = ({ onChange, onSubmit, toggleSetup = () => {} }: KinesisStreamsProps) => {
  const { t } = useTranslation('integrations');
  const { formData } = useContext(FormDataContext);
  const [formError, setFormError] = useState(null);
  const { availableStreams, setLogData } = useContext(ApiContext);
  const { clearSidebar, setSidebar } = useContext(SidebarContext);
  const productName = useProductName();
  const [logDataStatus, setLogDataUrl] = useFetch(
    null,
    (response) => {
      setLogData(response);
      onSubmit();
    },
    'POST',
    toAWSRequest(formData, {
      region: formData.awsCloudWatchAwsRegion.value,
      stream_name: formData.awsCloudWatchKinesisStream ? formData.awsCloudWatchKinesisStream.value : '',
    }),
  );

  useEffect(() => {
    setSidebar(
      <Panel bsStyle="info" header={<span>{t('aws_kinesis_existing_streams.sidebar_title')}</span>}>
        <AutoSetupContent>
          <p>{t('aws_kinesis_existing_streams.sidebar_requirement')}</p>

          <p>{t('aws_kinesis_existing_streams.sidebar_auto_setup', { productName })}</p>
        </AutoSetupContent>

        <Button
          onClick={() => {
            clearSidebar();
            toggleSetup();
          }}
          type="button">
          {t('aws_kinesis_existing_streams.setup_automatically_button')}
        </Button>
      </Panel>,
    );
  }, [clearSidebar, productName, setSidebar, t, toggleSetup]);

  useEffect(() => {
    if (logDataStatus.error) {
      setLogDataUrl(null);

      setFormError({
        full_message: logDataStatus.error,
        nice_message: <span>{t('aws_kinesis_existing_streams.no_logs_error')}</span>,
      });
    }
  }, [logDataStatus.error, setLogDataUrl, t]);

  const handleSubmit = () => {
    setLogDataUrl(ApiRoutes.INTEGRATIONS.AWS.KINESIS.HEALTH_CHECK);
  };

  return (
    <>
      <Modal show={logDataStatus.loading} bsSize="small" onHide={() => {}} closable={false}>
        <LoadingContent>
          <StyledSpinner />
          <LoadingMessage>{t('aws_kinesis_existing_streams.loading_message')}</LoadingMessage>
        </LoadingContent>
      </Modal>

      <FormWrap
        onSubmit={handleSubmit}
        buttonContent={t('aws_kinesis_existing_streams.verify_button')}
        loading={logDataStatus.loading}
        error={formError}
        disabled={formValidation.isFormValid(['awsCloudWatchKinesisStream'], formData)}
        title={t('aws_kinesis_existing_streams.title')}
        description={
          <>
            <p>{t('aws_kinesis_existing_streams.description_list')}</p>
            <p>
              <Trans
                t={t}
                i18nKey="aws_kinesis_existing_streams.description_docs"
                components={{
                  docsLink: (
                    <DocumentationLink
                      page={DocsRoutes.INTEGRATIONS.AWS.AWS_KINESIS_CLOUDWATCH_INPUTS}
                      text={t('aws_kinesis_existing_streams.docs_link_text')}
                    />
                  ),
                }}
              />
            </p>
          </>
        }>
        <ValidatedInput
          id="awsCloudWatchKinesisStream"
          type="select"
          fieldData={formData.awsCloudWatchKinesisStream}
          onChange={onChange}
          label={t('aws_kinesis_existing_streams.select_stream_label')}
          required>
          {renderOptions(availableStreams, t('aws_kinesis_existing_streams.select_stream_placeholder'))}
        </ValidatedInput>

        <FormAdvancedOptions onChange={onChange} />
      </FormWrap>
    </>
  );
};

export default KinesisStreams;
