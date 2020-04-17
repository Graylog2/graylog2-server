// @flow strict
import React, { useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import styled, { type StyledComponent, css } from 'styled-components';

import { type ThemeInterface } from 'theme';

import Version from 'util/Version';
import connect from 'stores/connect';
import StoreProvider from 'injection/StoreProvider';

const SystemStore = StoreProvider.getStore('System');

type Props = {
  system?: {
    version: string,
    hostname: string
  },
};

const StyledFooter: StyledComponent<{}, ThemeInterface, HTMLElement> = styled.footer(({ theme }) => css`
  text-align: center;
  font-size: 11px;
  color: ${theme.color.gray[70]};
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

export default connect(Footer, { system: SystemStore }, ({ system: { system } = {} }) => ({ system }));
