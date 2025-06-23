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
import { useMemo } from 'react';
import styled, { css } from 'styled-components';
import DOMPurify from 'dompurify';

import LoginBox from 'components/login/LoginBox';
import PublicNotifications from 'components/common/PublicNotifications';
import backgroundImage from 'images/auth/login-bg.svg';
import { Logo } from 'components/perspectives/DefaultBrand';
import AppConfig from 'util/AppConfig';
import useThemes from 'theme/hooks/useThemes';
import useCustomLogo from 'brand-customization/useCustomLogo';
import useProductName from 'brand-customization/useProductName';

const LogoContainer = styled.div`
  display: block;
  height: 5rem;
  width: auto;
  margin: auto;
  margin-bottom: 1.5rem;

  svg {
    width: 100%;
    height: 75px;
  }
`;

const Background = styled.div`
  position: relative;
  height: 100vh;
  width: 100%;
`;

const BackgroundText = styled.div<{ $backgroundImage: string }>(
  ({ $backgroundImage }) => css`
    z-index: -1;
    display: flex;
    flex-direction: column;
    position: absolute;
    vertical-align: middle;
    justify-content: center;
    height: 100%;
    width: 100%;
    padding: 0 30px;
    background-image: url(${$backgroundImage});
    background-position: center;
    background-size: cover;
  `,
);

const NotificationsContainer = styled.div`
  position: absolute;
  top: 0;
  margin-top: 5px;
  width: 100%;
`;

const LoginContainer = styled.div`
  display: flex;
  flex: 1 1 0%;
  flex-direction: row;
  min-width: 100%;
  min-height: 100%;
`;

const TextContainer = styled.div`
  vertical-align: middle;
  justify-content: center;
  place-self: center center;
  height: auto;
`;

const WelcomeMessage = styled.strong(
  ({ theme }) => css`
    display: block;
    font-size: ${theme.fonts.size.extraLarge};
    font-weight: 800;
    margin-top: 1.5rem;
    margin-bottom: 1.5rem;
  `,
);

const Claim = styled.h1(
  ({ theme }) => css`
    color: #fcfcfc;
    text-transform: uppercase;
    font-size: ${theme.fonts.size.huge};
    line-height: 1;
    font-weight: 600;
  `,
);
const Highlight = styled.span(
  ({ theme }) => css`
    color: ${theme.colors.brand.primary};
  `,
);

const CustomLogo = styled.div`
  svg {
    width: 100%;
    height: 200px;
  }
`;

const CustomizableLogo = () => {
  const { colorScheme } = useThemes('dark', false);
  const customLogo = useCustomLogo(colorScheme);

  return customLogo ? (
    <CustomLogo dangerouslySetInnerHTML={{ __html: customLogo }} />
  ) : (
    <>
      <LogoContainer>
        <Logo color="#ffffff" />
      </LogoContainer>
      <Claim>
        Data. Insights. <Highlight>Answers.</Highlight>
      </Claim>
    </>
  );
};

const sanitize = (content: string) =>
  DOMPurify.sanitize(content, { USE_PROFILES: { svg: true }, ADD_TAGS: ['use'], ADD_ATTR: ['xlink:href'] });

type Props = {
  children: React.ReactNode;
};

const svgDataUrl = (content: string) => `data:image/svg+xml;base64,${Buffer.from(content).toString('base64')}`;
const useLoginBackground = () =>
  useMemo(
    () =>
      AppConfig.branding()?.login?.background
        ? svgDataUrl(sanitize(AppConfig.branding()?.login?.background))
        : backgroundImage,
    [],
  );

const LoginChrome = ({ children }: Props) => {
  const productName = useProductName();
  const loginBackground = useLoginBackground();

  return (
    <LoginContainer>
      <LoginBox>
        <WelcomeMessage>Welcome to {productName}</WelcomeMessage>
        {children}
      </LoginBox>
      <Background>
        <NotificationsContainer>
          <PublicNotifications login />
        </NotificationsContainer>
        <BackgroundText $backgroundImage={loginBackground}>
          <TextContainer>
            <CustomizableLogo />
          </TextContainer>
        </BackgroundText>
      </Background>
    </LoginContainer>
  );
};

export default LoginChrome;
