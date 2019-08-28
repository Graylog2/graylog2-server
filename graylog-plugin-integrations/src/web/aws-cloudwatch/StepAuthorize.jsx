import React, { useContext, useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import { FormDataContext } from './context/FormData';
import { ApiContext } from './context/Api';
import { SidebarContext } from './context/Sidebar';

import ValidatedInput from '../common/ValidatedInput';
import MaskedInput from '../common/MaskedInput';
import FormWrap from '../common/FormWrap';
import Permissions from '../common/Permissions';
import { renderOptions } from '../common/Options';
import { ApiRoutes } from '../common/Routes';
import useFetch from '../common/hooks/useFetch';

import formValidation from '../utils/formValidation';

const StepAuthorize = ({ onChange, onSubmit }) => {
  const { formData } = useContext(FormDataContext);
  const { clearSidebar, setSidebar } = useContext(SidebarContext);
  const { availableRegions, setRegions, setStreams } = useContext(ApiContext);
  const [formError, setFormError] = useState(null);
  const [fetchRegionsStatus] = useFetch(ApiRoutes.INTEGRATIONS.AWS.REGIONS, setRegions, 'GET');
  const [fetchStreamsStatus, setStreamsFetch] = useFetch(
    null,
    (response) => {
      setStreams(response);
      onSubmit();
    },
    'POST',
    { region: formData.awsCloudWatchAwsRegion ? formData.awsCloudWatchAwsRegion.value : '' },
  );

  useEffect(() => {
    setStreamsFetch(null);
    if (fetchRegionsStatus.error) {
      setFormError({ full_message: fetchRegionsStatus.error });
    } else if (fetchStreamsStatus.error) {
      const badKey = /security token/g;
      const badSecret = /signing method/g;
      const noStreams = /No Kinesis streams/g;
      if (fetchStreamsStatus.error.match(badKey)) {
        setFormError({ full_message: fetchStreamsStatus.error, nice_message: 'Invalid AWS Key, check out your AWS account for the 20-character long, alphanumeric string that usually starts with the letters "AK"' });
      } else if (fetchStreamsStatus.error.match(badSecret)) {
        setFormError({ full_message: fetchStreamsStatus.error, nice_message: 'Invalid AWS Secret, it is usually a 40-character long, base-64 encoded string, but you only get to view it once when you create the Key' });
      } else if (fetchStreamsStatus.error.match(noStreams)) {
        // NOTE: If no streams are present we want to move to the KinesisSetup screen
        setStreams({ streams: [] });
        onSubmit();
      } else {
        setFormError({ full_message: fetchStreamsStatus.error });
      }
    }

    return () => {
      setFormError(null);
    };
  }, [fetchRegionsStatus.error, fetchStreamsStatus.error]);

  const handleSubmit = () => {
    setStreamsFetch(ApiRoutes.INTEGRATIONS.AWS.KINESIS.STREAMS);
  };

  useEffect(() => {
    setSidebar(<Permissions />);

    return () => {
      clearSidebar();
    };
  }, []);

  return (
    <>
      <FormWrap onSubmit={handleSubmit}
                buttonContent="Authorize &amp; Choose Stream"
                loading={fetchRegionsStatus.loading || fetchStreamsStatus.loading}
                disabled={formValidation.isFormValid([
                  'awsCloudWatchName',
                  'awsCloudWatchAwsKey',
                  'awsCloudWatchAwsSecret',
                  'awsCloudWatchAwsRegion',
                ], formData)}
                error={formError}
                title="Create Input &amp; Authorize AWS"
                description="This integration allows Graylog to read messages directly from a Kinesis stream. CloudWatch messages can optionally be forwarded to Kinesis via CloudWatch subscriptions and then read by Graylog.">

        {/* Fighting AutoComplete Forms */}
        <DisappearingInput id="name" type="text" />
        <DisappearingInput id="password" type="password" />
        {/* Continue on, Nothing to See Here */}

        <ValidatedInput id="awsCloudWatchName"
                        type="text"
                        fieldData={formData.awsCloudWatchName}
                        onChange={onChange}
                        placeholder="Kinesis Input Name"
                        label="Name"
                        autoComplete="off"
                        required />

        <ValidatedInput id="awsCloudWatchDescription"
                        type="textarea"
                        label="Description"
                        placeholder="Kinesis Input Description"
                        onChange={onChange}
                        fieldData={formData.awsCloudWatchDescription}
                        rows={4} />

        <ValidatedInput id="awsCloudWatchAwsKey"
                        type="text"
                        label="AWS Access Key"
                        placeholder="AK****************"
                        onChange={onChange}
                        fieldData={formData.awsCloudWatchAwsKey}
                        autoComplete="off"
                        maxLength="512"
                        help='Your AWS Key should be a 20-character long, alphanumeric string that starts with the letters "AK".'
                        required />

        <MaskedInput id="awsCloudWatchAwsSecret"
                     label="AWS Secret Key"
                     placeholder="***********"
                     onChange={onChange}
                     fieldData={formData.awsCloudWatchAwsSecret}
                     autoComplete="off"
                     maxLength="512"
                     help="Your AWS Secret is usually a 40-character long, base-64 encoded string."
                     required />

        <ValidatedInput id="awsCloudWatchAwsRegion"
                        type="select"
                        fieldData={formData.awsCloudWatchAwsRegion}
                        onChange={onChange}
                        label="AWS Region"
                        help="The AWS Region where Kinesis is running."
                        disabled={fetchRegionsStatus.loading}
                        required>
          {renderOptions(availableRegions, 'Choose AWS Region', fetchRegionsStatus.loading)}
        </ValidatedInput>
      </FormWrap>
    </>
  );
};

StepAuthorize.propTypes = {
  onSubmit: PropTypes.func.isRequired,
  onChange: PropTypes.func.isRequired,
};

const DisappearingInput = styled.input`
  position: fixed;
  top: -500vh;
  left: -500vw;
`;

export default StepAuthorize;
