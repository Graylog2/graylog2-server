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
import React, { useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';

import Version from 'util/Version';
import connect from 'stores/connect';
import StoreProvider from 'injection/StoreProvider';
import { Store } from 'stores/StoreTypes';

const SystemStore = StoreProvider.getStore('System');

type SystemStoreState = {
  system: {
    version?: string,
    hostname?: string,
  };
};

type Props = {
  system?: {
    version?: string,
    hostname?: string,
  },
};

type Jvm = {
  info: string;
};

const StyledFooter = styled.footer(({ theme }) => css`
  text-align: center;
  font-size: ${theme.fonts.size.small};
  color: ${theme.colors.gray[70]};
  height: 20px;

  @media print {
    display: none;
  }
`);

const Footer = ({ system }: Props) => {
  const [jvm, setJvm] = useState<Jvm | undefined>();

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
      Graylog {system.version} on {system.hostname} ({jvm.info})
    </StyledFooter>
  );
};

Footer.propTypes = {
  system: PropTypes.shape({
    version: PropTypes.string,
    hostname: PropTypes.string,
  }),
};

Footer.defaultProps = {
  system: undefined,
};

export default connect(
  Footer,
  { system: SystemStore as Store<SystemStoreState> },
  ({ system: { system } = { system: undefined } }) => ({ system }),
);
