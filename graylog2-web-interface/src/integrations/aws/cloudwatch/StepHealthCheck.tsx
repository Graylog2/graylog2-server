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
import React, { useContext, useEffect, useState, useCallback } from 'react';
import styled from 'styled-components';
import { Trans, useTranslation } from 'react-i18next';

import { Button, Panel, Input } from 'components/bootstrap';
import FormWrap from 'integrations/aws/common/FormWrap';
import SkipHealthCheck from 'integrations/aws/common/SkipHealthCheck';
import useFetch from 'integrations/hooks/useFetch';
import { ApiRoutes } from 'integrations/aws/common/Routes';
import Countdown from 'integrations/aws/common/Countdown';
import { KINESIS_LOG_TYPES } from 'integrations/aws/common/constants';
import { ApiContext } from 'integrations/aws/context/Api';
import FormDataContext from 'integrations/contexts/FormDataContext';
import Icon from 'components/common/Icon';
import { toAWSRequest } from 'integrations/aws/common/formDataAdapter';

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

type StepHealthCheckProps = {
  onSubmit: (...args: any[]) => void;
  onChange: (...args: any[]) => void;
};

const StepHealthCheck = ({ onChange, onSubmit }: StepHealthCheckProps) => {
  const { t } = useTranslation('integrations');
  const { logData, setLogData } = useContext(ApiContext);
  const { formData } = useContext(FormDataContext);
  const [pauseCountdown, setPauseCountdown] = useState(false);

  const [fetchStreamArnStatus, setStreamArnFetch] = useFetch(
    null,
    (response) => {
      onChange({ target: { name: 'awsCloudwatchKinesisStreamArn', value: response.result } });
    },
    'POST',
    toAWSRequest(formData, {
      region: formData.awsCloudWatchAwsRegion.value,
      stream_name: formData.awsCloudWatchKinesisStream.value,
    }),
  );

  useEffect(() => {
    setStreamArnFetch(ApiRoutes.INTEGRATIONS.AWS.KINESIS.STREAM_ARN);
  }, [setStreamArnFetch]);

  useEffect(() => {
    if (fetchStreamArnStatus.error) {
      // Proceed even if fetching the stream ARN fails — failure is non-blocking and intentionally ignored
    }
  }, [fetchStreamArnStatus]);

  const [logDataProgress, setLogDataUrl] = useFetch(
    null,
    (response) => {
      setLogData(response);
      onChange({ target: { name: 'awsCloudWatchKinesisInputType', value: response.type } });
    },
    'POST',
    toAWSRequest(formData, {
      region: formData.awsCloudWatchAwsRegion.value,
      stream_name: formData.awsCloudWatchKinesisStream.value,
    }),
  );

  const checkForLogs = useCallback(() => {
    setPauseCountdown(true);
    setLogDataUrl(ApiRoutes.INTEGRATIONS.AWS.KINESIS.HEALTH_CHECK);
  }, [setPauseCountdown, setLogDataUrl]);

  useEffect(() => {
    if (!logData) {
      checkForLogs();
    }
  }, [checkForLogs, logData]);

  useEffect(() => {
    if (!logDataProgress.loading && !logDataProgress.data) {
      setPauseCountdown(false);
      setLogDataUrl(null);
    }
  }, [logDataProgress.data, logDataProgress.loading, setLogDataUrl]);

  if (!logData) {
    return (
      <Panel
        bsStyle="warning"
        header={
          <Notice>
            <Icon name="warning" size="2x" />
            <span>{t('aws_step_health_check.no_response_title')}</span>
          </Notice>
        }>
        <p>{t('aws_step_health_check.waiting_description')}</p>

        <CheckAgain>
          <strong>
            <Trans
              t={t}
              i18nKey="aws_step_health_check.checking_again_in"
              components={{
                countdown: <Countdown timeInSeconds={120} callback={checkForLogs} paused={pauseCountdown} />,
              }}
            />
          </strong>

          <Button type="button" bsStyle="primary" bsSize="sm" onClick={checkForLogs} disabled={logDataProgress.loading}>
            {logDataProgress.loading
              ? t('aws_step_health_check.checking_button')
              : t('aws_step_health_check.check_now_button')}
          </Button>
        </CheckAgain>

        <p>
          <em>{t('aws_step_health_check.do_not_refresh')}</em>
        </p>

        <div>
          <SkipHealthCheck onSubmit={onSubmit} onChange={onChange} />
        </div>
      </Panel>
    );
  }
  const handleSubmit = () => {
    onSubmit();
    onChange({ target: { name: 'awsCloudWatchKinesisInputType', value: logData.type } });
  };
  const logTypeLabel = KINESIS_LOG_TYPES.find((type) => type.value === logData.type).label;

  return (
    <FormWrap
      onSubmit={handleSubmit}
      buttonContent={t('aws_step_health_check.review_button')}
      disabled={false}
      title={t('aws_step_health_check.title')}
      description={<p>{t('aws_step_health_check.sample_description', { logType: logTypeLabel })}</p>}>
      <Input
        id="awsCloudWatchLog"
        type="textarea"
        label={t('aws_step_health_check.formatted_log_label')}
        value={logData.message || t('aws_step_health_check.no_messages')}
        rows={10}
        disabled
      />
    </FormWrap>
  );
};

export default StepHealthCheck;
