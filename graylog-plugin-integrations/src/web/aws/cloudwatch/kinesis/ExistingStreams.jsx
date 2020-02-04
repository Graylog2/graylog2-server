import React, { useContext, useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import { Button, Modal, Panel } from 'components/graylog';

import { FormDataContext } from 'aws/context/FormData';
import { ApiContext } from 'aws/context/Api';
import { SidebarContext } from 'aws/context/Sidebar';

import useFetch from 'aws/common/hooks/useFetch';
import FormWrap from 'aws/common/FormWrap';
import ValidatedInput from 'aws/common/ValidatedInput';
import { ApiRoutes, DocsRoutes } from 'aws/common/Routes';
import { renderOptions } from 'aws/common/Options';
import formValidation from 'aws/utils/formValidation';
import DocumentationLink from 'components/support/DocumentationLink';

import FormAdvancedOptions from '../FormAdvancedOptions';

const KinesisStreams = ({ onChange, onSubmit, toggleSetup }) => {
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
                    bsSize="small">
        <LoadingContent>
          <i className="fa fa-spin fa-spinner" />
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

KinesisStreams.propTypes = {
  onSubmit: PropTypes.func.isRequired,
  onChange: PropTypes.func.isRequired,
  toggleSetup: PropTypes.func,
};

KinesisStreams.defaultProps = {
  toggleSetup: () => {
  },
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

  i.fa {
    font-size: 48px;
    color: #702785;
  }
`;

const LoadingMessage = styled.p`
  font-size: 16px;
  font-weight: bold;
  padding-top: 15px;
  color: #A6AFBD;
`;

export default KinesisStreams;
