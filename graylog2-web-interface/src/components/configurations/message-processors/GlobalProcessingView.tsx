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
import { useTranslation } from 'react-i18next';

type Props = {
  gracePeriod: string;
};

const Wrapper = styled.div(
  ({ theme }) => css`
    margin-bottom: ${theme.spacings.md};
    overflow: auto;
  `,
);

const StyledDefList = styled.dl.attrs({ className: 'deflist' })(
  ({ theme }) => css`
    &&.deflist {
      dd {
        padding-left: ${theme.spacings.md};
        display: table-cell;
      }
    }
  `,
);

const GlobalProcessingView = ({ gracePeriod }: Props) => {
  const { t } = useTranslation('configurations');

  return (
    <Wrapper>
      <h2>{t('message_processors.global_title')}</h2>
      <p>{t('message_processors.global_description')}</p>
      <StyledDefList>
        <dt>{t('message_processors.future_timestamp_normalization')}:</dt>
        <dd>{gracePeriod ? t('markdown.enabled') : t('markdown.disabled')}</dd>
        <dt>{t('message_processors.grace_period')}:</dt>
        <dd>{gracePeriod}</dd>
      </StyledDefList>
    </Wrapper>
  );
};

export default GlobalProcessingView;
