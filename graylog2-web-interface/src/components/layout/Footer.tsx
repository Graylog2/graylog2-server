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
import { PluginStore } from 'graylog-web-plugin/plugin';

import StandardFooter from './StandardFooter';

const StyledFooter = styled.footer(({ theme }) => css`
  text-align: center;
  font-size: ${theme.fonts.size.small};
  color: ${theme.colors.gray[70]};
  height: 20px;

  /* This combination of padding and box-sizing is required to fix a firefox flexbox bug */
  box-sizing: content-box;
  padding-bottom: 9px;

  @media print {
    display: none;
  }
`);

const Footer = () => {
  const customFooter = PluginStore.exports('pageFooter');

  if (customFooter.length === 1) {
    const CustomFooter = customFooter[0].component;

    return (
      <StyledFooter>
        <CustomFooter />
      </StyledFooter>
    );
  }

  return (
    <StyledFooter>
      <StandardFooter />
    </StyledFooter>
  );
};

Footer.propTypes = {};

export default Footer;
