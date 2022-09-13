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
import PropTypes from 'prop-types';
import styled, { useTheme } from 'styled-components';

const Link = styled.a(({ theme }) => `
  display: flex;
  color: ${theme.colors.variant.default};
  align-items: center;
  justify-content: center;
  padding: 0 15px;
  min-height: 50px;

  &:hover,
  &:focus {
    color: ${theme.colors.variant.dark.default};
    background-color: transparent;
  }
`);

type Props = {
  active?: boolean,
  className?: string,
}

// Don't pass active prop, since `a` tag doesn't support it.
// eslint-disable-next-line no-unused-vars
const BrandComponent = ({ active, ...props }: Props) => {
  const theme = useTheme();

  return (
    <Link {...props} className="navbar-brand">
      <svg height="28" width="90" viewBox="0 0 287 92" fill="none" xmlns="http://www.w3.org/2000/svg" aria-labelledby="logoTitleId">
        <title id="logoTitleId">Graylog Logo</title>
        <path d="M0.479675 45.8302C0.479675 58.7145 8.75571 68.7774 21.8281 68.7774C29.0696 68.7774 35.0885 65.5799 38.098 60.2192V69.4357C38.098 78.182 32.4552 83.8247 23.803 83.8247C16.0913 83.8247 11.0128 79.8748 9.88426 73.1035H1.04395C2.73677 84.7652 11.2949 91.8186 23.803 91.8186C37.9099 91.8186 46.8443 82.6021 46.8443 68.1191V24.0116H38.9444L38.2861 31.8174C35.3707 26.0806 29.6339 22.695 22.2043 22.695C8.84975 22.695 0.479675 32.8519 0.479675 45.8302ZM9.31998 45.6421C9.31998 37.178 14.5865 30.4067 23.4269 30.4067C32.4552 30.4067 37.7218 36.8018 37.7218 45.6421C37.7218 54.6705 32.2672 61.0656 23.3328 61.0656C14.6806 61.0656 9.31998 54.2944 9.31998 45.6421Z"
              fill={theme.colors.variant.darker.default} />
        <path d="M81.2327 23.6354C79.4458 23.2592 78.1292 23.0711 76.5304 23.0711C70.2293 23.0711 65.6211 26.2687 63.7402 31.065L63.1759 24.1056H54.8999V70H63.7402V45.6421C63.7402 36.8018 68.9127 31.7234 77.2828 31.7234H81.2327V23.6354Z"
              fill={theme.colors.variant.darker.default} />
        <path d="M98.2704 71.1285C105.7 71.1285 112.001 67.8369 114.352 62.5704L115.293 70H122.816V41.1279C122.816 28.7139 115.105 22.695 103.725 22.695C91.8753 22.695 83.9754 28.996 83.9754 38.5887H91.6872C91.6872 33.04 95.9193 29.7484 103.349 29.7484C109.556 29.7484 114.164 32.4757 114.164 40.1875V41.5041L99.6811 42.6327C88.8658 43.4791 82.5648 48.7456 82.5648 57.2098C82.5648 65.5798 88.3956 71.1285 98.2704 71.1285ZM100.81 64.2632C95.0728 64.2632 91.4991 61.818 91.4991 56.9276C91.4991 52.4134 94.6967 49.404 102.502 48.6516L114.258 47.7111V50.1563C114.258 58.8085 109.18 64.2632 100.81 64.2632Z"
              fill={theme.colors.variant.darker.default} />
        <path d="M124.996 90.596C127.347 91.1603 129.793 91.5365 132.708 91.5365C139.762 91.5365 144.37 88.2449 147.473 80.345L169.198 24.0116H160.075L146.439 61.0656L133.084 24.0116H123.774L142.301 72.4452L140.702 76.8653C138.539 82.6962 135.153 83.3545 130.921 83.3545H124.996V90.596Z"
              fill={theme.colors.variant.darker.default} />
        <path d="M183.423 70V0.782288H174.583V70H183.423Z"
              fill="#F44040" />
        <path d="M239.31 45.8302C239.31 58.7145 247.586 68.7774 260.659 68.7774C267.9 68.7774 273.919 65.5798 276.929 60.2192V69.4357C276.929 78.182 271.286 83.8247 262.634 83.8247C254.922 83.8247 249.843 79.8748 248.715 73.1035H239.874C241.567 84.7652 250.125 91.8186 262.634 91.8186C276.74 91.8186 285.675 82.6021 285.675 68.1191V24.0116H277.775L277.117 31.8174C274.201 26.0806 268.464 22.695 261.035 22.695C247.68 22.695 239.31 32.8519 239.31 45.8302ZM248.151 45.6421C248.151 37.178 253.417 30.4067 262.257 30.4067C271.286 30.4067 276.552 36.8018 276.552 45.6421C276.552 54.6705 271.098 61.0657 262.163 61.0657C253.511 61.0657 248.151 54.2944 248.151 45.6421Z"
              fill="#F44040" />
        <path fillRule="evenodd"
              clipRule="evenodd"
              d="M212.5 62C221.06 62 228 55.0604 228 46.5C228 37.9396 221.06 31 212.5 31C203.94 31 197 37.9396 197 46.5C197 55.0604 203.94 62 212.5 62ZM212.5 70C225.479 70 236 59.4787 236 46.5C236 33.5213 225.479 23 212.5 23C199.521 23 189 33.5213 189 46.5C189 59.4787 199.521 70 212.5 70Z"
              fill="#F44040" />
        <path fillRule="evenodd"
              clipRule="evenodd"
              d="M213.772 36.0561C213.892 35.6114 214.499 35.5522 214.703 35.9652L219.03 44.7215C219.114 44.892 219.288 45 219.478 45H223C223.828 45 224.5 45.6716 224.5 46.5C224.5 47.3284 223.828 48 223 48H217.614C217.424 48 217.25 47.892 217.166 47.7215L214.941 43.22L211.228 56.9439C211.108 57.3886 210.501 57.4478 210.297 57.0348L205.97 48.2785C205.886 48.108 205.712 48 205.522 48H202C201.172 48 200.5 47.3284 200.5 46.5C200.5 45.6716 201.172 45 202 45H207.386C207.576 45 207.75 45.108 207.834 45.2785L210.059 49.78L213.772 36.0561Z"
              fill={theme.colors.variant.darker.default} />
      </svg>
    </Link>
  );
};

BrandComponent.propTypes = {
  active: PropTypes.bool,
  className: PropTypes.string,
};

BrandComponent.defaultProps = {
  active: false,
  className: undefined,
};

export default BrandComponent;
