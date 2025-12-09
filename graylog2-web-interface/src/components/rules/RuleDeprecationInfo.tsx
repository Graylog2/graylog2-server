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
import styled, { css } from 'styled-components';
import * as React from 'react';

import { Label } from 'components/bootstrap';
import { HoverForHelp } from 'components/common';
import usePipelineRulesMetadata from 'components/rules/hooks/usePipelineRulesMetadata';

type Props = {
  pipelineId: string;
  showFor?: 'pipeline' | 'rule';
};

const DeprecatedLabel = styled(Label)(
  ({ theme }) => css`
    display: inline-flex;
    margin-left: ${theme.spacings.xs};
    vertical-align: inherit;
    gap: ${theme.spacings.xxs};
  `,
);

const StyledList = styled.ul(
  ({ theme }) => css`
    display: flex;
    flex-direction: column;
    gap: ${theme.spacings.xxs};
    margin-bottom: ${theme.spacings.xs};
    margin-left: ${theme.spacings.xs};
    list-style-type: none;
  `,
);

const RuleDeprecationInfo = ({ pipelineId, showFor = 'rule' }: Props) => {
  const { data, isLoading } = usePipelineRulesMetadata(pipelineId);

  if (isLoading || !data) return null;
  const deprecatedFunctions = data.deprecated_functions;

  if (deprecatedFunctions?.length === 0) return null;

  return (
    <DeprecatedLabel bsStyle="warning">
      <span>Deprecated Function</span>
      <HoverForHelp trigger="hover" type="info" title={`This ${showFor} contains at least one deprecated function:`}>
        <StyledList>
          {deprecatedFunctions?.map((func) => (
            <li key={func}>{func}</li>
          ))}
        </StyledList>
      </HoverForHelp>
    </DeprecatedLabel>
  );
};

export default RuleDeprecationInfo;
