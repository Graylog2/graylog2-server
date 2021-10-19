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
import PropTypes from 'prop-types';
import styled from 'styled-components';
import { Button, Panel, Input } from 'components/bootstrap';

import FormWrap from 'aws/common/FormWrap';
import SkipHealthCheck from 'aws/common/SkipHealthCheck';
import useFetch from 'aws/common/hooks/useFetch';
import { ApiRoutes } from 'aws/common/Routes';
import Countdown from 'aws/common/Countdown';
import { DEFAULT_KINESIS_LOG_TYPE, KINESIS_LOG_TYPES } from 'aws/common/constants';
import { ApiContext } from 'aws/context/Api';
import { FormDataContext } from 'aws/context/FormData';

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

  const knownLog = logData.type === DEFAULT_KINESIS_LOG_TYPE;
  const iconClass = knownLog ? 'check' : 'exclamation-triangle';
  const acknowledgment = knownLog ? 'Awesome!' : 'Drats!';
  const bsStyle = knownLog ? 'success' : 'warning';
  const logTypeLabel = KINESIS_LOG_TYPES.find((type) => type.value === logData.type).label;
  const logType = knownLog ? `a ${logTypeLabel}` : 'an unknown';

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
                 <span>{acknowledgment} looks like <em>{logType}</em> message type.</span>
               </Notice>
             )}>
        {knownLog ? 'Take a look at what we have parsed so far and you can create Pipeline Rules to handle even more!' : 'Not to worry, Graylog can still read in these log messages. We have parsed what we could and you can build Pipeline Rules to do the rest!'}
      </Panel>

      <Input id="awsCloudWatchLog"
             type="textarea"
             label="Formatted Log Message"
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
