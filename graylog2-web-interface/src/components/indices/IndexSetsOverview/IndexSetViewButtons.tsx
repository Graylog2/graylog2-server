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
import { useTranslation } from 'react-i18next';
import styled, { css } from 'styled-components';

import SegmentedControl from 'components/bootstrap/SegmentedControl';
import useLayoutVariant from 'components/common/PaginatedEntityTable/hooks/useLayoutVariant';

import { INDEX_SET_VIEW_VARIANTS } from './Constants';

const Container = styled.div(
  ({ theme }) => css`
    display: flex;
    align-items: center;
    gap: ${theme.spacings.sm};
  `,
);

const Label = styled.span(
  ({ theme }) => css`
    color: ${theme.colors.text.secondary};
    white-space: nowrap;
  `,
);

const NarrowSegmentedControl = styled(SegmentedControl)`
  width: fit-content;
`;

type VariantValue = (typeof INDEX_SET_VIEW_VARIANTS)[keyof typeof INDEX_SET_VIEW_VARIANTS];

const IndexSetViewButtons = () => {
  const { t } = useTranslation('indexsets');
  const { activeLayoutVariant, selectLayoutVariant } = useLayoutVariant();

  const segments = [
    { label: t('overview_table.view_default'), value: INDEX_SET_VIEW_VARIANTS.default },
    { label: t('overview_table.view_configuration'), value: INDEX_SET_VIEW_VARIANTS.configuration },
    { label: t('overview_table.view_routing'), value: INDEX_SET_VIEW_VARIANTS.routing },
  ];

  const onChange = (value: VariantValue) => {
    if (value === INDEX_SET_VIEW_VARIANTS.default) {
      if (activeLayoutVariant) selectLayoutVariant(activeLayoutVariant);
    } else {
      selectLayoutVariant(value);
    }
  };

  return (
    <Container>
      <Label>{t('overview_table.view')}</Label>
      <NarrowSegmentedControl<VariantValue>
        data={segments}
        value={activeLayoutVariant as VariantValue}
        onChange={onChange}
      />
    </Container>
  );
};

export default IndexSetViewButtons;
