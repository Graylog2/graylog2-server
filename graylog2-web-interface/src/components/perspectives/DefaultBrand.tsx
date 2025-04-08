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
import styled, { useTheme } from 'styled-components';
import DOMPurify from 'dompurify';

import { NAV_LOGO_HEIGHT } from 'theme/constants';
import AppConfig from 'util/AppConfig';

export const Logo = ({ color }: { color: string }) => (
  <svg
    width="90"
    height="27"
    viewBox="0 0 90 27"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
    aria-labelledby="logoTitleId">
    <title id="logoTitleId">Graylog Logo</title>
    <path
      d="M4.58121 13.452C4.58121 17.0932 6.92009 19.9371 10.6145 19.9371C12.661 19.9371 14.362 19.0334 15.2125 17.5185V20.1231C15.2125 22.5949 13.6178 24.1896 11.1726 24.1896C8.99319 24.1896 7.55796 23.0733 7.23902 21.1597H4.74068C5.21908 24.4554 7.63768 26.4487 11.1726 26.4487C15.1593 26.4487 17.6843 23.8441 17.6843 19.751V7.28589H15.4517L15.2656 9.49187C14.4417 7.8706 12.8204 6.9138 10.7208 6.9138C6.94666 6.9138 4.58121 9.78423 4.58121 13.452ZM7.07955 13.3989C7.07955 11.0068 8.56792 9.0932 11.0663 9.0932C13.6178 9.0932 15.1062 10.9005 15.1062 13.3989C15.1062 15.9504 13.5646 17.7577 11.0397 17.7577C8.59451 17.7577 7.07955 15.8441 7.07955 13.3989Z"
      fill={color}
    />
    <path
      d="M27.4027 7.17957C26.8977 7.07325 26.5256 7.0201 26.0738 7.0201C24.2931 7.0201 22.9908 7.92377 22.4592 9.27924L22.2997 7.31245H19.9608V20.2826H22.4592V13.3989C22.4592 10.9005 23.921 9.46531 26.2864 9.46531H27.4027V7.17957Z"
      fill={color}
    />
    <path
      d="M32.2177 20.6015C34.3174 20.6015 36.0981 19.6713 36.7625 18.1829L37.0284 20.2826H39.1545V12.1231C39.1545 8.6148 36.9753 6.9138 33.7592 6.9138C30.4104 6.9138 28.1778 8.69452 28.1778 11.4055H30.3572C30.3572 9.83739 31.5533 8.90716 33.653 8.90716C35.4071 8.90716 36.7094 9.67791 36.7094 11.8573V12.2294L32.6164 12.5484C29.5599 12.7876 27.7792 14.2759 27.7792 16.668C27.7792 19.0334 29.427 20.6015 32.2177 20.6015ZM32.9354 18.6613C31.314 18.6613 30.3041 17.9703 30.3041 16.5882C30.3041 15.3125 31.2078 14.462 33.4136 14.2494L36.736 13.9836V14.6746C36.736 17.1198 35.3009 18.6613 32.9354 18.6613Z"
      fill={color}
    />
    <path
      d="M39.7706 26.1032C40.435 26.2627 41.1263 26.369 41.9501 26.369C43.9436 26.369 45.2459 25.4388 46.1228 23.2062L52.2625 7.28589H49.6842L45.8306 17.7577L42.0563 7.28589H39.4253L44.6612 20.9736L44.2093 22.2228C43.598 23.8707 42.6411 24.0567 41.4451 24.0567H39.7706V26.1032Z"
      fill={color}
    />
    <path d="M56.2826 20.2826V0.721085H53.7843V20.2826H56.2826Z" fill={color} />
    <path
      d="M72.0767 13.452C72.0767 17.0932 74.4156 19.9371 78.1101 19.9371C80.1565 19.9371 81.8575 19.0334 82.7082 17.5185V20.1231C82.7082 22.5949 81.1134 24.1896 78.6683 24.1896C76.4888 24.1896 75.0535 23.0733 74.7347 21.1597H72.2361C72.7146 24.4554 75.1332 26.4487 78.6683 26.4487C82.6548 26.4487 85.1799 23.8441 85.1799 19.751V7.28589H82.9473L82.7613 9.49187C81.9372 7.8706 80.3159 6.9138 78.2164 6.9138C74.4422 6.9138 72.0767 9.78423 72.0767 13.452ZM74.5753 13.3989C74.5753 11.0068 76.0635 9.0932 78.5618 9.0932C81.1134 9.0932 82.6017 10.9005 82.6017 13.3989C82.6017 15.9504 81.0603 17.7577 78.5352 17.7577C76.0901 17.7577 74.5753 15.8441 74.5753 13.3989Z"
      fill={color}
    />
    <path
      fillRule="evenodd"
      clipRule="evenodd"
      d="M64.5 18.0217C66.9191 18.0217 68.8804 16.0605 68.8804 13.6413C68.8804 11.2221 66.9191 9.26087 64.5 9.26087C62.0809 9.26087 60.1196 11.2221 60.1196 13.6413C60.1196 16.0605 62.0809 18.0217 64.5 18.0217ZM64.5 20.2826C68.168 20.2826 71.1413 17.3092 71.1413 13.6413C71.1413 9.97341 68.168 7 64.5 7C60.832 7 57.8587 9.97341 57.8587 13.6413C57.8587 17.3092 60.832 20.2826 64.5 20.2826Z"
      fill={color}
    />
    <path
      fillRule="evenodd"
      clipRule="evenodd"
      d="M64.8595 10.6898C64.8934 10.5641 65.0649 10.5474 65.1226 10.6641L66.3454 13.1387C66.3692 13.1869 66.4184 13.2174 66.4721 13.2174H67.4674C67.7014 13.2174 67.8913 13.4072 67.8913 13.6413C67.8913 13.8754 67.7014 14.0652 67.4674 14.0652H65.9453C65.8916 14.0652 65.8424 14.0347 65.8187 13.9865L65.1899 12.7143L64.1405 16.5928C64.1066 16.7185 63.9351 16.7352 63.8774 16.6185L62.6546 14.1439C62.6308 14.0957 62.5817 14.0652 62.528 14.0652H61.5326C61.2986 14.0652 61.1087 13.8754 61.1087 13.6413C61.1087 13.4072 61.2986 13.2174 61.5326 13.2174H63.0547C63.1084 13.2174 63.1576 13.2479 63.1814 13.2961L63.8102 14.5683L64.8595 10.6898Z"
      fill={color}
    />
  </svg>
);

const Container = styled.div`
  display: inline-flex;
`;

type Props = {
  className?: string;
};

const StyledSvgContainer = styled.div`
  svg {
    width: 100%;
    height: ${NAV_LOGO_HEIGHT};
    display: block;
  }
`;
export const BrandNavLogo = () => {
  const theme = useTheme();
  const { mode } = theme;
  const customLogo = AppConfig.branding()?.logo?.[mode];

  // eslint-disable-next-line react/no-danger
  if (customLogo) return <StyledSvgContainer dangerouslySetInnerHTML={{ __html: DOMPurify().sanitize(customLogo) }} />;

  return <Logo color={theme.colors.brand.logo} />;
};

const DefaultBrand = ({ className = '' }: Props) => (
  <Container className={`${className}`}>
    <BrandNavLogo />
  </Container>
);

export default DefaultBrand;
