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

import { Col, Row } from 'components/bootstrap';
import { Section, PaginatedEntityTable } from 'components/common';
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
  defaultDisplayedAttributes: ['rule', 'pipeline', 'stage', 'connected_streams'],
  defaultColumnOrder: ['rule', 'pipeline', 'stage', 'connected_streams'],
};

const STREAM_RULES_LAYOUT = {
  entityTableId: 'input-stream-rules',
  defaultPageSize: 20,
  defaultSort: { attributeId: 'stream', direction: 'asc' } as Sort,
  defaultDisplayedAttributes: ['stream', 'rule'],
  defaultColumnOrder: ['stream', 'rule'],
};

const ListCol = styled(Col)(
  ({ theme }) => css`
    padding-top: ${theme.spacings.lg};
  `,
);

const InputDiagnosisRulesTab = ({ inputId }: Props) => (
  <>
    <Section title="Pipeline Rules">
      <Row>
        <ListCol md={12}>
          <PaginatedEntityTable<InputPipelineRule>
            humanName="pipeline rules"
            tableLayout={PIPELINE_RULES_LAYOUT}
            fetchEntities={(searchParams) => fetchInputPipelineRules(inputId, searchParams)}
            keyFn={(searchParams) => pipelineRulesKeyFn(inputId, searchParams)}
            entityAttributesAreCamelCase={false}
            searchPlaceholder="Search for pipeline rule"
            columnRenderers={pipelineRulesColumnRenderers}
            withoutURLParams
          />
        </ListCol>
      </Row>
    </Section>

    <Section title="Stream Rules">
      <Row>
        <ListCol md={12}>
          <PaginatedEntityTable<InputStreamRule>
            humanName="stream rules"
            tableLayout={STREAM_RULES_LAYOUT}
            fetchEntities={(searchParams) => fetchInputStreamRules(inputId, searchParams)}
            keyFn={(searchParams) => streamRulesKeyFn(inputId, searchParams)}
            entityAttributesAreCamelCase={false}
            searchPlaceholder="Search for stream rule"
            columnRenderers={streamRulesColumnRenderers}
            withoutURLParams
          />
        </ListCol>
      </Row>
    </Section>
  </>
);

export default InputDiagnosisRulesTab;
