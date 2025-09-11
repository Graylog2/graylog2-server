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
import { useState, useCallback, useEffect } from 'react';
import styled, { css } from 'styled-components';
import * as React from 'react';

import { Label } from 'components/bootstrap';
import DEPRECATED_PIPELINE_FUNCTIONS from 'components/pipelines/constants';
import type { RuleType } from 'stores/rules/RulesStore';
import { HoverForHelp } from 'components/common';

type Props = {
  rule: RuleType;
};

const DeprecatedLabel = styled(Label)(
  ({ theme }) => css`
    display: inline-flex;
    margin-left: ${theme.spacings.sm};
    font-size: ${theme.fonts.size.small};
    gap: ${theme.spacings.xxs};
    align-items: center;
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

const RuleDeprecationInfo = ({ rule }: Props) => {
  const [deprecatedFunctions, setDeprecatedFunctions] = useState<string[]>([]);

  const findDeprecatedFunctions = useCallback(() => {
    setDeprecatedFunctions(
      rule.rule_builder.actions
        .filter((act) => DEPRECATED_PIPELINE_FUNCTIONS.includes(act.function))
        .map((act) => act.function),
    );
  }, [rule]);

  useEffect(() => findDeprecatedFunctions(), [findDeprecatedFunctions]);

  if (deprecatedFunctions.length === 0) return null;

  return (
    <DeprecatedLabel bsStyle="warning">
      <span>Deprecated Function</span>
      <HoverForHelp trigger="hover" type="info" title="This rule contains at least one deprecated function:">
        <StyledList>
          {deprecatedFunctions.map((func) => (
            <li>{func}</li>
          ))}
        </StyledList>
      </HoverForHelp>
    </DeprecatedLabel>
  );
};

export default RuleDeprecationInfo;
