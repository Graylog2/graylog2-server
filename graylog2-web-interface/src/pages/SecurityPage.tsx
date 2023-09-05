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
import styled, { createGlobalStyle, css } from 'styled-components';
import { COLOR_SCHEME_DARK } from '@graylog/sawmill';
import { useContext } from 'react';

import SecurityNoLicenseImage from 'assets/security-no-license-bg.png';
import SecurityNoLicenseImageDark from 'assets/security-no-license-bg-dark.png';
import SecurityNoLicenseOverlay from 'assets/security-no-license-overlay.png';
import SecurityNoLicenseOverlayDark from 'assets/security-no-license-overlay-dark.png';
import ColorSchemeContext from 'theme/ColorSchemeContext';

const generateStyles = () => css<{ backgroundImage: string }>`
  body {
    background: url(${({ backgroundImage }) => backgroundImage}) no-repeat center center fixed;
    background-size: cover;
  }
`;

const PageLayout = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
`;

const SecurityPageStyles = createGlobalStyle`
  ${generateStyles()}
`;

const SecurityPage = () => {
  const colorScheme = useContext(ColorSchemeContext);

  return (
    <PageLayout>
      <SecurityPageStyles backgroundImage={colorScheme === COLOR_SCHEME_DARK ? SecurityNoLicenseImageDark : SecurityNoLicenseImage} />

      <div>
        <a href="https://www.graylog.org/explore-security/" target="_blank" rel="noreferrer">
          <img style={{ height: '500px' }}
               src={colorScheme === COLOR_SCHEME_DARK ? SecurityNoLicenseOverlayDark : SecurityNoLicenseOverlay}
               alt="security-overlay" />
        </a>
      </div>
    </PageLayout>
  );
};

export default SecurityPage;
