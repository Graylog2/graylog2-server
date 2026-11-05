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
import styled, { css } from 'styled-components';
import { useTranslation } from 'react-i18next';

import { Col, Row } from 'components/bootstrap';
import { Section, PaginatedEntityTable } from 'components/common';
import SectionGrid from 'components/common/Section/SectionGrid';
import type { Sort } from 'stores/PaginationTypes';

import { fetchInputPipelineRules, keyFn as pipelineRulesKeyFn } from './hooks/useInputPipelineRules';
import { fetchInputStreamRules, keyFn as streamRulesKeyFn } from './hooks/useInputStreamRules';
import { pipelineRulesColumnRenderers, streamRulesColumnRenderers } from './customColumnRenderers';
import type { InputPipelineRule, InputStreamRule } from './types';

type Props = {
  inputId: string;
};

const PIPELINE_RULES_LAYOUT = {
  entityTableId: 'input-pipeline-rules',
  defaultPageSize: 20,
  defaultSort: { attributeId: 'rule', direction: 'asc' } as Sort,
  defaultDisplayedAttributes: ['rule', 'pipeline', 'connected_streams'],
  defaultColumnOrder: ['rule', 'pipeline', 'connected_streams'],
};

const STREAM_RULES_LAYOUT = {
  entityTableId: 'input-stream-rules',
  defaultPageSize: 20,
  defaultSort: { attributeId: 'stream', direction: 'asc' } as Sort,
  defaultDisplayedAttributes: ['stream', 'rule'],
  defaultColumnOrder: ['stream', 'rule'],
};

const StyledSectionGrid = styled(SectionGrid)<{ $rows?: string }>(
  ({ $rows, theme }) => css`
    grid-template-columns: 1fr;
    grid-template-rows: ${$rows || '1fr'};
    gap: ${theme.spacings.xs};
  `,
);

const InputDiagnosisRulesTab = ({ inputId }: Props) => {
  const { t } = useTranslation('inputs');

  return (
    <StyledSectionGrid $rows="1fr 1fr">
      <Section title={t('diagnosis.rules.pipeline_rules_title')}>
        <p>{t('diagnosis.rules.pipeline_rules_description')}</p>
        <Row>
          <Col md={12}>
            <PaginatedEntityTable<InputPipelineRule>
              humanName={t('diagnosis.rules.pipeline_rules_human_name')}
              tableLayout={PIPELINE_RULES_LAYOUT}
              fetchEntities={(searchParams) => fetchInputPipelineRules(inputId, searchParams)}
              keyFn={(searchParams) => pipelineRulesKeyFn(inputId, searchParams)}
              entityAttributesAreCamelCase={false}
              searchPlaceholder={t('diagnosis.rules.pipeline_rules_search')}
              columnRenderers={pipelineRulesColumnRenderers}
              fetchOptions={{ refetchInterval: 5000 }}
              withoutURLParams
            />
          </Col>
        </Row>
      </Section>

      <Section title={t('diagnosis.rules.stream_rules_title')}>
        <p>{t('diagnosis.rules.stream_rules_description')}</p>
        <Row>
          <Col md={12}>
            <PaginatedEntityTable<InputStreamRule>
              humanName={t('diagnosis.rules.stream_rules_human_name')}
              tableLayout={STREAM_RULES_LAYOUT}
              fetchEntities={(searchParams) => fetchInputStreamRules(inputId, searchParams)}
              keyFn={(searchParams) => streamRulesKeyFn(inputId, searchParams)}
              entityAttributesAreCamelCase={false}
              searchPlaceholder={t('diagnosis.rules.stream_rules_search')}
              columnRenderers={streamRulesColumnRenderers}
              fetchOptions={{ refetchInterval: 5000 }}
              withoutURLParams
            />
          </Col>
        </Row>
      </Section>
    </StyledSectionGrid>
  );
};

export default InputDiagnosisRulesTab;
