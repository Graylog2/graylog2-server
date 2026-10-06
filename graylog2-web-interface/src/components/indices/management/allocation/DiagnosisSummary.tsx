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
import styled, { css } from 'styled-components';

import Label from 'components/bootstrap/Label';

import { avoidText, headline, optionText } from './diagnosisText';
import type { ShardContext } from './diagnosisText';

import type { AllocationDiagnosis } from '../types';

const Headline = styled.p(
  ({ theme }) => css`
    font-size: ${theme.fonts.size.large};
    margin-bottom: ${theme.spacings.sm};
  `,
);

const Options = styled.ol(
  ({ theme }) => css`
    margin-bottom: ${theme.spacings.sm};

    li {
      margin-bottom: ${theme.spacings.xs};
    }
  `,
);

const Muted = styled.span(
  ({ theme }) => css`
    color: ${theme.colors.gray[60]};
  `,
);

const CommandToggle = styled.summary(
  ({ theme }) => css`
    cursor: pointer;
    color: ${theme.colors.global.link};
  `,
);

const Command = styled.pre`
  white-space: pre-wrap;
  word-break: break-all;
  margin: 4px 0 0;
`;

const Avoid = styled.p(
  ({ theme }) => css`
    color: ${theme.colors.variant.darker.warning};
    margin-bottom: ${theme.spacings.xs};
  `,
);

type Props = {
  diagnosis: AllocationDiagnosis;
  context: ShardContext;
};

// The plain-language part of a shard's explanation: what happened, then what can be done, most data kept first.
const DiagnosisSummary = ({ diagnosis, context }: Props) => {
  const actionable = diagnosis.options.filter((option) => option.action !== 'WAIT');

  return (
    <div>
      <Headline>{headline(diagnosis, context)}</Headline>
      {actionable.length > 0 && (
        <>
          <strong>What you can do{actionable.length > 1 ? ', most data kept first' : ''}:</strong>
          <Options>
            {actionable.map((option) => {
              const text = optionText(option, context);

              return (
                <li key={option.action}>
                  {text.title} {text.dataLoss && <Muted>{text.dataLoss}</Muted>}{' '}
                  {text.where && <Label bsStyle="default">{text.where}</Label>}
                  {option.command && (
                    <details>
                      <CommandToggle>Show command</CommandToggle>
                      <Command>{option.command}</Command>
                    </details>
                  )}
                </li>
              );
            })}
          </Options>
        </>
      )}
      {diagnosis.avoid.map((action) => {
        const text = avoidText(action);

        return text ? <Avoid key={action}>{text}</Avoid> : null;
      })}
    </div>
  );
};

export default DiagnosisSummary;
