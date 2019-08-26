import React, { useContext, useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import { Button, Panel } from 'react-bootstrap';
import styled from 'styled-components';

import { Input } from 'components/bootstrap';

import FormWrap from '../common/FormWrap';
import useFetch from '../common/hooks/useFetch';
import { ApiRoutes } from '../common/Routes';

import { ApiContext } from './context/Api';
import { FormDataContext } from './context/FormData';
import { SidebarContext } from './context/Sidebar';
import Countdown from '../common/Countdown';

const StepHealthCheck = ({ onSubmit }) => {
  const { logData, setLogData } = useContext(ApiContext);
  const { formData } = useContext(FormDataContext);
  const { clearSidebar } = useContext(SidebarContext);
  const [pauseCountdown, setPauseCountdown] = useState(false);

  const [logDataProgress, setLogDataUrl] = useFetch(
    null,
    (response) => {
      setLogData(response);
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
    clearSidebar();

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
                  bsStyle="primary"
                  bsSize="sm"
                  onClick={checkForLogs}
                  disabled={logDataProgress.loading}>
            {logDataProgress.loading ? 'Checking...' : 'Check Now'}
          </Button>
        </CheckAgain>


        <p><em>Do not refresh your browser, we are continually checking for your logs and this page will automatically refresh when your logs are available.</em></p>
      </Panel>
    );
  }

  const unknownLog = logData.type === 'KINESIS_RAW';
  const iconClass = unknownLog ? 'times' : 'check';
  const acknowledgment = unknownLog ? 'Drats!' : 'Awesome!';
  const bsStyle = unknownLog ? 'warning' : 'success';

  let logType;

  switch (logData.type) {
    case 'KINESIS_FLOW_LOGS':
      logType = 'a Flow Log';
      break;

    default:
      logType = 'an unknown';
      break;
  }

  return (
    <FormWrap onSubmit={onSubmit}
              buttonContent="Review &amp; Finalize"
              disabled={false}
              title="Create Kinesis Stream"
              description={<p>We&apos;re going to attempt to parse a single log to help you out! If we&apos;re unable to, or you would like it parsed differently, head on over to <a href="/system/pipelines">Pipeline Rules</a> to set up your own parser!</p>}>

      <Panel bsStyle={bsStyle}
             header={(
               <Notice><i className={`fa fa-${iconClass} fa-2x`} />
                 <span>{acknowledgment} looks like <em>{logType}</em> log type.</span>
               </Notice>
                  )}>
        {unknownLog ? "Not to worry, we've parsed what we could and you can build Pipeline Rules to do the rest!" : "Take a look at what we've parsed so far and you can create Pipeline Rules to handle even more!"}
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
