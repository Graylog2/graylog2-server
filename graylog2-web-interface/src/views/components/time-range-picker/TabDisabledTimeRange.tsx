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
import styled from 'styled-components';

const Wrapper = styled.div`
  padding: 12px 6px;

  h5 {
    font-weight: bold;
    margin: 0 0 6px;
  }
`;

const TabDisabledTimeRange = () => {
  const { t } = useTranslation('search');

  return (
    <Wrapper>
      <h5>{t('time_range.no_override_title')}</h5>
      <p>{t('time_range.no_override_hint')}</p>
    </Wrapper>
  );
};

export default TabDisabledTimeRange;
