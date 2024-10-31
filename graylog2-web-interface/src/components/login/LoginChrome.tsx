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
import styled, { css } from 'styled-components';

import LoginBox from 'components/login/LoginBox';
import PublicNotifications from 'components/common/PublicNotifications';
import backgroundImage from 'images/auth/login-bg.svg';
import { Logo } from 'components/perspectives/DefaultBrand';

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

const BackgroundText = styled.div`
  z-index: -1;
  display: flex;
  flex-direction: column;
  position: absolute;
  vertical-align: middle;
  justify-content: center;
  height: 100%;
  width: 100%;
  padding: 0 30px;
  background-image: url(${backgroundImage});
  background-position: center;
  background-size: cover;
`;

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

const WelcomeMessage = styled.strong(({ theme }) => css`
  display: block;
  font-size: ${theme.fonts.size.extraLarge};
  font-weight: 800;
  margin-top: 1.5rem;
  margin-bottom: 1.5rem;
`);

const Claim = styled.h1(({ theme }) => css`
  color: #fcfcfc;
  text-transform: uppercase;
  font-size: ${theme.fonts.size.huge};
  line-height: 1;
  font-weight: 600;
`);
const Highlight = styled.span(({ theme }) => css`
  color: ${theme.colors.brand.primary};
`);

type Props = {
  children: React.ReactNode,
};

const LoginChrome = ({ children }: Props) => (
  <LoginContainer>
    <LoginBox>
      <WelcomeMessage>Welcome to Graylog</WelcomeMessage>
      {children}
    </LoginBox>
    <Background>
      <NotificationsContainer>
        <PublicNotifications readFromConfig />
      </NotificationsContainer>
      <BackgroundText>
        <TextContainer>
          <LogoContainer>
            <Logo color="#ffffff" />
          </LogoContainer>
          <Claim>Data. Insights. <Highlight>Answers.</Highlight></Claim>
        </TextContainer>
      </BackgroundText>
    </Background>
  </LoginContainer>
);

export default LoginChrome;
