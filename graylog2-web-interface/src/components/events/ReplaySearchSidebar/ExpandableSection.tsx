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
import React, { useState } from 'react';
import styled, { css } from 'styled-components';
import { useTranslation } from 'react-i18next';

import { IconButton } from 'components/common';

const Section = styled.div(
  ({ theme }) => css`
    display: flex;
    gap: ${theme.spacings.xs};
    flex-direction: column;
  `,
);

const Header = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
`;

const ExpandableSection = ({ title, children = null }: React.PropsWithChildren<{ title: string }>) => {
  const { t } = useTranslation('events');
  const [open, setOpen] = useState<boolean>(true);
  const onToggle = () => setOpen((cur) => !cur);

  return (
    <Section>
      <Header>
        <h2>{title}</h2>
        <IconButton
          onClick={onToggle}
          title={open ? t('alerts_replay.close_section', { title }) : t('alerts_replay.open_section', { title })}
          name={open ? 'keyboard_arrow_up' : 'keyboard_arrow_down'}
        />
      </Header>
      {open ? children : null}
    </Section>
  );
};

export default ExpandableSection;
