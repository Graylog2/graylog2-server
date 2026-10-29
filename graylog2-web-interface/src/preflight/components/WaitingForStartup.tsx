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
import styled from 'styled-components';
import { useEffect } from 'react';
import { Space } from '@mantine/core';
import { useTranslation } from 'react-i18next';

import useProductName from 'brand-customization/useProductName';
import Spinner from 'components/common/Spinner';
import { Section } from 'preflight/components/common';
import useServerAvailability from 'preflight/hooks/useServerAvailability';
import reloadPage from 'preflight/components/reloadPage';

const P = styled.p`
  max-width: 700px;
`;

const WaitingForStartup = () => {
  const { t } = useTranslation('preflight');
  const productName = useProductName();
  const { data: serverIsAvailable } = useServerAvailability();

  useEffect(() => {
    if (serverIsAvailable) {
      reloadPage();
    }
  }, [serverIsAvailable]);

  return (
    <Section title={t('waiting_for_startup.title')}>
      <P>{t('waiting_for_startup.description', { productName })}</P>

      <Space h="md" />
      <b>
        <Spinner delay={0} text={t('waiting_for_startup.spinner', { productName })} />
      </b>
    </Section>
  );
};

export default WaitingForStartup;
