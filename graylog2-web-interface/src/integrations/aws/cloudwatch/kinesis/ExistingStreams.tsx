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

import { Button, Modal, Panel } from 'components/bootstrap';
import DocumentationLink from 'components/support/DocumentationLink';
import { FormDataContext } from 'integrations/aws/context/FormData';
import { ApiContext } from 'integrations/aws/context/Api';
import { SidebarContext } from 'integrations/aws/context/Sidebar';
import useFetch from 'integrations/aws/common/hooks/useFetch';
import FormWrap from 'integrations/aws/common/FormWrap';
import ValidatedInput from 'integrations/aws/common/ValidatedInput';
import { ApiRoutes, DocsRoutes } from 'integrations/aws/common/Routes';
import { renderOptions } from 'integrations/aws/common/Options';
import formValidation from 'integrations/aws/utils/formValidation';
import Spinner from 'components/common/Spinner';

import FormAdvancedOptions from '../FormAdvancedOptions';

type KinesisStreamsProps = {
  onSubmit: (...args: any[]) => void;
  onChange: (...args: any[]) => void;
  toggleSetup?: (...args: any[]) => void;
};

const KinesisStreams = ({
  onChange,
  onSubmit,

  toggleSetup = () => {
  },
}: KinesisStreamsProps) => {
  const { formData } = useContext(FormDataContext);
  const [formError, setFormError] = useState(null);
  const { availableStreams, setLogData } = useContext(ApiContext);
  const { clearSidebar, setSidebar } = useContext(SidebarContext);
  const [logDataStatus, setLogDataUrl] = useFetch(
    null,
    (response) => {
      setLogData(response);
      onSubmit();
    },
    'POST',
    {
      region: formData.awsCloudWatchAwsRegion.value,
      stream_name: formData.awsCloudWatchKinesisStream ? formData.awsCloudWatchKinesisStream.value : '',
    },
  );

  useEffect(() => {
    setSidebar(
      <Panel bsStyle="info" header={<span>Don&apos;t see the stream you need?</span>}>
        <AutoSetupContent>
          <p>At least one Kinesis stream must exist in the specified region in order to continue with the setup. The log stream must contain at least a few log messages.</p>

          <p>
            Graylog also supports the ability to create a Kinesis stream for you and subscribe it to a CloudWatch log group of your choice. Please be aware that this option will create additional resources in your AWS environment that will incur billing charges.
          </p>
        </AutoSetupContent>

        <Button onClick={() => {
          clearSidebar();
          toggleSetup();
        }}
                type="button">
          Setup Kinesis Automatically
        </Button>
      </Panel>,
    );
  }, []);

  useEffect(() => {
    if (logDataStatus.error) {
      setLogDataUrl(null);

      setFormError({
        full_message: logDataStatus.error,
        nice_message: <span>We were unable to find any logs in this Kinesis stream. Please select a different Kinesis stream.</span>,
      });
    }
  }, [logDataStatus.error]);

  const handleSubmit = () => {
    setLogDataUrl(ApiRoutes.INTEGRATIONS.AWS.KINESIS.HEALTH_CHECK);
  };

  return (
    <>
      <LoadingModal show={logDataStatus.loading}
                    backdrop="static"
                    keyboard={false}
                    onHide={() => {}}
                    bsSize="small">
        <LoadingContent>
          <StyledSpinner />
          <LoadingMessage>This request may take a few moments.</LoadingMessage>
        </LoadingContent>
      </LoadingModal>

      <FormWrap onSubmit={handleSubmit}
                buttonContent="Verify Stream &amp; Format"
                loading={logDataStatus.loading}
                error={formError}
                disabled={formValidation.isFormValid(['awsCloudWatchKinesisStream'], formData)}
                title="Select Kinesis Stream"
                description={(
                  <>
                    <p>
                      Below is a list of all Kinesis streams found within the specified AWS account.
                    </p>
                    <p>
                      Please select the stream you would like to read messages from, or follow the&nbsp;
                      <DocumentationLink page={DocsRoutes.INTEGRATIONS.AWS.AWS_KINESIS_CLOUDWATCH_INPUTS} text="AWS Kinesis/CloudWatch Input " />
                      documentation for more details on this set up.
                    </p>
                  </>
                )}>

        <ValidatedInput id="awsCloudWatchKinesisStream"
                        type="select"
                        fieldData={formData.awsCloudWatchKinesisStream}
                        onChange={onChange}
                        label="Select Stream"
                        required>
          {renderOptions(availableStreams, 'Select Kinesis Stream')}
        </ValidatedInput>

        <FormAdvancedOptions onChange={onChange} />
      </FormWrap>
    </>
  );
};

const AutoSetupContent = styled.div`
  margin-bottom: 9px;
`;

const LoadingModal = styled(Modal)`
  > .modal-dialog {
    width: 400px;
    margin-left: auto;
    margin-right: auto;
  }
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

export default KinesisStreams;
