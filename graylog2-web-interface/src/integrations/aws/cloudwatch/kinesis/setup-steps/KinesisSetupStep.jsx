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
import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

const KinesisSetupStep = ({ label, progress }) => {
  const { data, error, loading } = progress;

  const waitingText = !data && !loading && !error && 'Waiting...';
  const loadingText = loading ? `Creating ${label}` : waitingText;
  const successText = data ? data.result : loadingText;
  const defaultText = error || successText;

  return (
    <StepItem>
      <IconWrap>
        {!data && !loading && !error && <i className="fa fa-hourglass-start fa-2x" style={{ color: '#dce1e5' }} />}
        {loading && <i className="fa fa-spinner fa-2x fa-spin" style={{ color: '#0063be' }} />}
        {data && <i className="fa fa-check fa-2x" style={{ color: '#00ae42' }} />}
        {error && <i className="fa fa-times fa-2x" style={{ color: '#ad0707' }} />}
      </IconWrap>

      <Content>
        <StepHeader>Create {label}</StepHeader>

        <StepDetails>
          {defaultText}
        </StepDetails>
      </Content>
    </StepItem>
  );
};

KinesisSetupStep.propTypes = {
  progress: PropTypes.shape({
    data: PropTypes.object,
    error: PropTypes.object,
    loading: PropTypes.bool,
  }).isRequired,
  label: PropTypes.string.isRequired,
};

const StepItem = styled.li`
  display: flex;
  margin: 0 0 12px;
`;

const IconWrap = styled.div`
  min-width: 36px;
`;

const Content = styled.div`
  flex-grow: 1;
`;

const StepHeader = styled.span`
  font-size: 18px;
`;

const StepDetails = styled.p`
  margin: 3px 0 0;
`;

export default KinesisSetupStep;
