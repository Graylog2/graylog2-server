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
import React, { useContext, useEffect } from 'react';
import { Trans, useTranslation } from 'react-i18next';
import styled, { css } from 'styled-components';

import { Button } from 'components/bootstrap';
import RenderCompletionCallback from 'views/components/widgets/RenderCompletionCallback';

import { useIsInteractiveMode } from '../contexts/InteractiveContext';

type Props = {
  toggleEdit: () => void;
  editing?: boolean;
};

const Container = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: inherit;
`;

const SpacedHeading = styled.h2(
  ({ theme }) => css`
    margin-bottom: ${theme.spacings.sm};
  `,
);

const EmptyAggregationContent = ({ toggleEdit, editing = false }: Props) => {
  const { t } = useTranslation('aggregation');
  const onRenderComplete = useContext(RenderCompletionCallback);

  useEffect(() => {
    if (onRenderComplete) {
      onRenderComplete();
    }
  }, [onRenderComplete]);

  const interactive = useIsInteractiveMode();
  const text = editing ? (
    <p>
      {t('builder_empty.editing')}
      <br />
      {t('builder_empty.add_metric_hint')}
      <br />
      {t('builder_empty.preview_hint', { button: t('wizard_common.update_preview') })}
      <br />
      {t('builder_empty.finish_hint', { update: t('widgets:widget_edit.update'), cancel: t('common:actions.cancel') })}
    </p>
  ) : (
    <p>
      {interactive ? (
        <Trans
          t={t}
          i18nKey="builder_empty.edit_hint_interactive"
          components={{ editButton: <Button bsStyle="info" onClick={toggleEdit} /> }}
        />
      ) : (
        t('builder_empty.edit_hint')
      )}
    </p>
  );

  return (
    <Container>
      <div>
        <SpacedHeading>{t('builder_empty.title')}</SpacedHeading>

        {text}
      </div>
    </Container>
  );
};

export default EmptyAggregationContent;
