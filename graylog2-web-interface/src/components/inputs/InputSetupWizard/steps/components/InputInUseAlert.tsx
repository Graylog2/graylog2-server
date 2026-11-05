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

import Routes from 'routing/Routes';
import { Link } from 'components/common';
import { Alert, Row, Col } from 'components/bootstrap';
import useInputReferences from 'components/inputs/InputSetupWizard/hooks/useInputReferences';

import { StyledList } from './StepWrapper';

type Props = {
  inputId?: string;
};

const StreamListWrapper = styled.div(
  ({ theme }) => css`
    margin-bottom: ${theme.spacings.sm};
  `,
);

const StyledAlert = styled(Alert)`
  margin-top: 0;
`;

const InputInUseAlert = ({ inputId }: Props = { inputId: undefined }) => {
  const { t } = useTranslation('inputs');
  const { data: inputReferencesData, isLoading: isLoadingInputReferences } = useInputReferences(inputId);

  if (isLoadingInputReferences) return null;

  if (!inputReferencesData.isInputAlreadyInUse) return null;

  return (
    <Row>
      <Col md={12}>
        <StyledAlert bsStyle="danger" title={t('setup_wizard.input_in_use.title')}>
          {inputReferencesData.stream_refs.length > 0 && (
            <StreamListWrapper>
              {t('setup_wizard.input_in_use.stream_refs')}
              <StyledList>
                {inputReferencesData.stream_refs.map((stream) => (
                  <li key={stream.id}>
                    <Link to={Routes.stream_view(stream.id)} target="_blank">
                      {stream.name}
                    </Link>
                  </li>
                ))}
              </StyledList>
            </StreamListWrapper>
          )}
          {inputReferencesData.pipeline_refs.length > 0 && (
            <>
              {t('setup_wizard.input_in_use.pipeline_refs')}
              <StyledList>
                {inputReferencesData.pipeline_refs.map((pipeline) => (
                  <li key={pipeline.id}>
                    <Link to={Routes.SYSTEM.PIPELINES.PIPELINE(pipeline.id)} target="_blank">
                      {pipeline.name}
                    </Link>
                  </li>
                ))}
              </StyledList>
            </>
          )}
          {t('setup_wizard.input_in_use.prevent_duplication')}
        </StyledAlert>
      </Col>
    </Row>
  );
};

export default InputInUseAlert;
