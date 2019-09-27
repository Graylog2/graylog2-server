import React from 'react';
import PropTypes from 'prop-types';

const Agree = ({ groupName, streamName }) => {
  return (
    <>
      <p>This auto setup will create the following AWS resources. Click below to acknowledge that you understand that these resources will be created and that you are solely responsible for any associated AWS fees incurred from them. Note that all resources must be manually deleted by you if they are not needed.</p>

      <ol>
        <li>Create a Kinesis stream with <strong>1</strong> shard.</li>
        <li>Create an IAM Role and Policy to allow the specified CloudWatch group <strong>{groupName}</strong> to publish log messages to the Kinesis stream <strong>{streamName}</strong></li>
        <li>Create a CloudWatch Subscription, which publishes log messages to the Kinesis stream.</li>
      </ol>
    </>
  );
};

Agree.propTypes = {
  groupName: PropTypes.string.isRequired,
  streamName: PropTypes.string.isRequired,
};

export default Agree;
