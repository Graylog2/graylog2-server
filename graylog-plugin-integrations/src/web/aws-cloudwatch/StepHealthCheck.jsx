import React, { useContext } from 'react';
import PropTypes from 'prop-types';
import { Panel } from 'react-bootstrap';
import styled from 'styled-components';

import { Input } from 'components/bootstrap';

import { ApiContext } from './context/Api';

import FormWrap from '../common/FormWrap';

const StepHealthCheck = ({ onSubmit }) => {
  const { logData } = useContext(ApiContext);

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

export default StepHealthCheck;
