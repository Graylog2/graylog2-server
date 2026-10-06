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
import DisclosureToggle from './DisclosureToggle';
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

const Command = styled.pre`
  white-space: pre-wrap;
  word-break: break-all;
  margin: 4px 0 0;
`;

const Warnings = styled.div(
  ({ theme }) => css`
    color: ${theme.colors.variant.darker.warning};
    margin-bottom: ${theme.spacings.xs};

    ul {
      margin-bottom: 0;
    }
  `,
);

const Section = styled.details(
  ({ theme }) => css`
    margin-bottom: ${theme.spacings.sm};
  `,
);

const SectionToggle = styled(DisclosureToggle)`
  font-weight: bold;
`;

type Props = {
  diagnosis: AllocationDiagnosis;
  context: ShardContext;
};

// The plain-language part of a shard's explanation: what happened first; what can be done (most data kept first)
// folded away until opened; then the warnings.
const DiagnosisSummary = ({ diagnosis, context }: Props) => {
  const actionable = diagnosis.options.filter((option) => option.action !== 'WAIT');
  const warnings = diagnosis.avoid.map(avoidText).filter((text) => text !== null);

  return (
    <div>
      <Headline>{headline(diagnosis, context)}</Headline>
      {actionable.length > 0 && (
        <Section>
          <SectionToggle>
            What you can do ({actionable.length === 1 ? '1 option' : `${actionable.length} options, most data kept first`})
          </SectionToggle>
          <Options>
            {actionable.map((option) => {
              const text = optionText(option, context);

              return (
                <li key={option.action}>
                  {text.title} {text.dataLoss && <Muted>{text.dataLoss}</Muted>}{' '}
                  {text.where && <Label bsStyle="default">{text.where}</Label>}
                  {option.command && (
                    <details>
                      <DisclosureToggle>Show command</DisclosureToggle>
                      <Command>{option.command}</Command>
                    </details>
                  )}
                </li>
              );
            })}
          </Options>
        </Section>
      )}
      {warnings.length === 1 && (
        <Warnings>
          <strong>Warning:</strong> {warnings[0]}
        </Warnings>
      )}
      {warnings.length > 1 && (
        <Warnings>
          <strong>Warnings:</strong>
          <ul>
            {warnings.map((text) => (
              <li key={text}>{text}</li>
            ))}
          </ul>
        </Warnings>
      )}
    </div>
  );
};

export default DiagnosisSummary;
