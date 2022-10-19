import * as React from 'react';
import styled, { css } from 'styled-components';

import LoginBox from 'components/login/LoginBox';
import PublicNotifications from 'components/common/PublicNotifications';
import bgImage from 'images/auth/banner-bg.jpeg';
import graylogLogo from 'images/auth/gl_logo_horiz.svg';

const Logo = styled.img`
  display: block;
  height: 3rem;
  width: auto;
`;

const Background = styled.div`
  position: relative;
  height: 100vh;
  width: 100%;
`;

const BackgroundText = styled.div`
  display: flex;
  flex-direction: column;
  position: absolute;
  vertical-align: middle;
  justify-content: center;
  height: 100%;
  width: 100%;
`;

const BackgroundImage = styled.img`
  height: 100%;
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
  justify-self: center;
  align-self: center;
  height: auto;
  width: 50%;
`;

const WelcomeMessage = styled.strong(({ theme }) => css`
  display: block;
  font-size: ${theme.fonts.size.huge};
  font-weight: 800;
  margin-top: 1.5rem;
  margin-bottom: 1.5rem;
`);

const BrandName = styled.h3(({ theme }) => css`
  color: ${theme.colors.gray['60']};
  font-size: 1.5rem;
  line-height: 2rem;
`);
const Claim = styled.h1(({ theme }) => css`
  color: ${theme.colors.brand.secondary};
  text-transform: uppercase;
  font-size: 2.5rem;
  line-height: 1;
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
      <Logo alt="logo" src={graylogLogo} />
      <WelcomeMessage>Welcome to Graylog</WelcomeMessage>
      {children}
    </LoginBox>
    <Background>
      <BackgroundText>
        <TextContainer>
          <BrandName>Graylog</BrandName>
          <Claim><Highlight>Log Management</Highlight> Done Right</Claim>
        </TextContainer>
        <PublicNotifications />
      </BackgroundText>
      <BackgroundImage alt="background" src={bgImage} />
    </Background>
  </LoginContainer>
);

export default LoginChrome;
