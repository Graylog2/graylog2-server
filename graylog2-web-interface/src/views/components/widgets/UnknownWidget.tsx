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
import { Trans, useTranslation } from 'react-i18next';
import styled from 'styled-components';

import type { EditWidgetComponentProps, WidgetComponentProps } from 'views/types';
import { Icon } from 'components/common';
import ClipboardButton from 'components/common/ClipboardButton';
import useProductName from 'brand-customization/useProductName';

const Container = styled.div`
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
`;

const IconContainer = styled.div`
  margin: 3px 15px 0 0;
`;

const Description = styled.div`
  max-width: 700px;
`;

const Row = styled.div`
  margin-bottom: 5px;

  &:last-child {
    margin-bottom: 0;
  }
`;

const OrderedList = styled.ol`
  padding: 0;
  list-style: decimal inside none;
`;

type Props = WidgetComponentProps & EditWidgetComponentProps;
const UnknownWidget = ({ config, type }: Props) => {
  const { t } = useTranslation('widgets');
  const productName = useProductName();

  return (
    <Container>
      <IconContainer>
        <Icon name="help" size="3x" />
      </IconContainer>
      <Description>
        <Row>
          <strong>{t('unknown_widget.title', { type })}</strong>
        </Row>
        <Row>
          <Trans t={t} i18nKey="unknown_widget.cannot_render" values={{ type }} components={{ strong: <strong /> }} />
        </Row>

        <Row>
          <OrderedList>
            <li>{t('unknown_widget.missing_plugin')}</li>
            <li>{t('unknown_widget.legacy_dashboard')}</li>
          </OrderedList>
        </Row>

        <Row>{t('unknown_widget.what_to_do', { productName })}</Row>
        <Row>
          {t('unknown_widget.copy_config')}{' '}
          <ClipboardButton
            title={<Icon name="content_copy" size="sm" />}
            text={JSON.stringify(config, null, 2)}
            bsSize="xsmall"
          />
        </Row>
      </Description>
    </Container>
  );
};

export default UnknownWidget;
