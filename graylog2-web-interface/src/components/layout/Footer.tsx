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
// @flow strict
import React, { useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import styled, { StyledComponent, css } from 'styled-components';

import type { ThemeInterface } from 'theme';
import Version from 'util/Version';
import connect from 'stores/connect';
import StoreProvider from 'injection/StoreProvider';

const SystemStore = StoreProvider.getStore('System');

type Props = {
  system?: {
    version: string,
    hostname: string,
  },
};

const StyledFooter: StyledComponent<{}, ThemeInterface, HTMLElement> = styled.footer(({ theme }) => css`
  text-align: center;
  font-size: ${theme.fonts.size.small};
  color: ${theme.colors.gray[70]};
  margin-bottom: 15px;
  height: 20px;

  @media print {
    display: none;
  }
`);

const Footer = ({ system }: Props) => {
  const [jvm, setJvm] = useState();

  useEffect(() => {
    let mounted = true;

    SystemStore.jvm().then((jvmInfo) => {
      if (mounted) {
        setJvm(jvmInfo);
      }
    });

    return () => {
      mounted = false;
    };
  }, []);

  if (!(system && jvm)) {
    return (
      <StyledFooter>
        Graylog {Version.getFullVersion()}
      </StyledFooter>
    );
  }

  return (
    <StyledFooter>
      {/* @ts-ignore jvm s checked in line 65 */}
      Graylog {system.version} on {system.hostname} ({jvm.info})
    </StyledFooter>
  );
};

Footer.propTypes = {
  system: PropTypes.exact({
    version: PropTypes.string,
    hostname: PropTypes.string,
  }),
};

Footer.defaultProps = {
  system: undefined,
};

export default connect(Footer, { system: SystemStore }, ({ system: { system } = {} }: { system }) => ({ system }));
