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
import { useTranslation } from 'react-i18next';
import styled from 'styled-components';

import { Select } from 'components/common';

import { MS_DAY, MS_HOUR, MS_MINUTE, MS_SECOND } from './timeoutConstants';

const TimeoutSelect = styled(Select)`
  width: 150px;
`;

const OPTIONS = [
  { value: `${MS_SECOND}`, key: 'seconds' },
  { value: `${MS_MINUTE}`, key: 'minutes' },
  { value: `${MS_HOUR}`, key: 'hours' },
  { value: `${MS_DAY}`, key: 'days' },
];

const TimeoutUnitSelect = (props) => {
  const { t } = useTranslation('users');
  const options = OPTIONS.map(({ value, key }) => ({ value, label: t(`timeout.units.${key}`) }));

  return <TimeoutSelect {...props} aria-label={t('timeout.unit_label')} options={options} />;
};

export default TimeoutUnitSelect;
