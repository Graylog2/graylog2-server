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
import * as React from 'react';
import { useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import styled, { css } from 'styled-components';
import camelCase from 'lodash/camelCase';
import upperCase from 'lodash/upperCase';

import { Button } from 'components/bootstrap';
import { Icon } from 'components/common';
import FilterRuleForm from 'components/streams/StreamDetails/output-filter/FilterRuleForm';
import type { StreamOutputFilterRule } from 'components/streams/StreamDetails/output-filter/Types';
import useStreamOutputRuleMutation from 'components/streams/hooks/useStreamOutputRuleMutation';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

type Props ={
  filterRule: Partial<StreamOutputFilterRule>,
  streamId: string,
  destinationType: string,
};

const StyledButton = styled(Button)(({ theme }) => css`
  margin: 0 ${theme.spacings.xxs};
`);

const FilterRuleEditButton = ({ streamId, filterRule, destinationType }: Props) => {
  const [showForm, setShowForm] = useState(false);
  const { createStreamOutputRule, updateStreamOutputRule } = useStreamOutputRuleMutation();
  const sendTelemetry = useSendTelemetry();

  const queryClient = useQueryClient();
  const isNew = !filterRule?.id;

  const onClick = () => {
    setShowForm(true);

    sendTelemetry(TELEMETRY_EVENT_TYPE.STREAMS[`STREAM_ITEM_DATA_ROUTING_${upperCase(camelCase(destinationType))}_FILTER_${isNew ? 'CREATE_OPENED' : 'UPDATE_OPENED'}`], {
      app_pathname: 'streams',
    });
  };

  const handleSubmit = (filterOutputRule: Partial<StreamOutputFilterRule>) => {
    const submitFilterHandler = isNew ? createStreamOutputRule : updateStreamOutputRule;

    submitFilterHandler({ streamId, filterOutputRule }).then(() => {
      queryClient.invalidateQueries(['streams']);
      setShowForm(false);
    });
  };

  const title = isNew ? 'Create Filter Rule' : 'Edit Filter Rule';

  return (
    <>
      <StyledButton bsStyle={isNew ? 'default' : 'default'}
                    bsSize={isNew ? 'sm' : 'xs'}
                    onClick={onClick}
                    title="Edit">
        {isNew ? (<><Icon name="add" size="sm" /> Create rule</>) : (<Icon name="edit_square" />)}
      </StyledButton>
      {showForm && (
        <FilterRuleForm title={title}
                        filterRule={filterRule}
                        destinationType={destinationType}
                        onCancel={() => setShowForm(false)}
                        handleSubmit={handleSubmit} />
      )}
    </>
  );
};

export default FilterRuleEditButton;
