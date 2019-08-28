import React, { useContext, useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import { Button, Panel } from 'react-bootstrap';
import styled from 'styled-components';

import { Input } from 'components/bootstrap';

import FormWrap from '../common/FormWrap';
import useFetch from '../common/hooks/useFetch';
import { ApiRoutes } from '../common/Routes';
import Countdown from '../common/Countdown';

import { ApiContext } from './context/Api';
import { FormDataContext } from './context/FormData';
import SkipHealthCheck from './auto-setup-steps/SkipHealthCheck';

const StepHealthCheck = ({ onChange, onSubmit }) => {
  const { logData, setLogData } = useContext(ApiContext);
  const { formData } = useContext(FormDataContext);
  const [pauseCountdown, setPauseCountdown] = useState(false);

  const [logDataProgress, setLogDataUrl] = useFetch(
    null,
    (response) => {
      setLogData(response);
      onChange({ target: { name: 'awsCloudWatchKinesisInputType', value: response.type } });
    },
    'POST',
    {
      region: formData.awsCloudWatchAwsRegion.value,
      stream_name: formData.awsCloudWatchKinesisStream.value,
    },
  );

  const checkForLogs = () => {
    setPauseCountdown(true);
    setLogDataUrl(ApiRoutes.INTEGRATIONS.AWS.KINESIS.HEALTH_CHECK);
  };

  useEffect(() => {
    if (!logData) {
      checkForLogs();
    }
  }, []);

  useEffect(() => {
    if (!logDataProgress.loading && !logDataProgress.data) {
      setPauseCountdown(false);
      setLogDataUrl(null);
    }
  }, [logDataProgress.loading]);

  if (!logData) {
    return (
      <Panel bsStyle="warning"
             header={(
               <Notice><i className="fa fa-exclamation-triangle fa-2x" />
                 <span>We haven&apos;t received a response back from Amazon yet.</span>
               </Notice>
            )}>
        <p>Hang out for a few moments while we keep checking your AWS stream for logs. Amazon&apos;s servers parse logs every 10 minutes, so grab a cup of coffee because this may take some time!</p>

        <CheckAgain>
          <strong>Checking again in: <Countdown timeInSeconds={120} callback={checkForLogs} paused={pauseCountdown} /></strong>

          <Button type="button"
                  bsStyle="success"
                  bsSize="sm"
                  onClick={checkForLogs}
                  disabled={logDataProgress.loading}>
            {logDataProgress.loading ? 'Checking...' : 'Check Now'}
          </Button>
        </CheckAgain>

        <p><em>Do not refresh your browser, we are continually checking for your logs and this page will automatically refresh when your logs are available.</em></p>

        <div>
          <SkipHealthCheck onSubmit={onSubmit} onChange={onChange} />
        </div>
      </Panel>
    );
  }

  const unknownLog = logData.type === 'KINESIS_RAW';
  const iconClass = unknownLog ? 'times' : 'check';
  const acknowledgment = unknownLog ? 'Drats!' : 'Awesome!';
  const bsStyle = unknownLog ? 'warning' : 'success';
  const logType = unknownLog ? 'an unknown' : 'a Flow Log';
  const handleSubmit = () => {
    onSubmit();
    onChange({ target: { name: 'awsCloudWatchKinesisInputType', value: logData.type } });
  };

  return (
    <FormWrap onSubmit={handleSubmit}
              buttonContent="Review &amp; Finalize"
              disabled={false}
              title="Create Kinesis Stream"
              description={<p>We are going to attempt to parse a single log to help you out! If we are unable to, or you would like it parsed differently, head on over to <a href="/system/pipelines">Pipeline Rules</a> to set up your own parser!</p>}>

      <Panel bsStyle={bsStyle}
             header={(
               <Notice><i className={`fa fa-${iconClass} fa-2x`} />
                 <span>{acknowledgment} looks like <em>{logType}</em> log type.</span>
               </Notice>
             )}>
        {unknownLog ? 'Not to worry, we have parsed what we could and you can build Pipeline Rules to do the rest!' : 'Take a look at what we have parsed so far and you can create Pipeline Rules to handle even more!'}
      </Panel>

      <Input id="awsCloudWatchLog"
             type="textarea"
             label="Formatted CloudWatch Log"
             value={logData.message}
             rows={10}
             disabled />
    </FormWrap>
  );
};

StepHealthCheck.propTypes = {
  onSubmit: PropTypes.func.isRequired,
  onChange: PropTypes.func.isRequired,
};

const Notice = styled.span`
  display: flex;
  align-items: center;

  > span {
    margin-left: 6px;
  }
`;

const CheckAgain = styled.p`
  display: flex;
  align-items: center;

  > strong {
    margin-right: 9px;
  }
`;

export default StepHealthCheck;
