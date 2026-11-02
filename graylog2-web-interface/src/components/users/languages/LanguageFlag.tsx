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
import styled from 'styled-components';

import { PSEUDO_LANGUAGE } from 'util/pseudoLocalize';

import type { LanguageCode } from './Languages';

const FlagSvg = styled.svg`
  flex-shrink: 0;
  width: 22px;
  height: 14px;
  box-shadow: 0 0 0 1px rgb(0 0 0 / 12%);
`;

const FLAGS: { [code in LanguageCode]: React.ReactNode } = {
  en: (
    <>
      <clipPath id="language-flag-en-clip">
        <path d="M30,15 h30 v15 z v15 h-30 z h-30 v-15 z v-15 h30 z" />
      </clipPath>
      <rect width="60" height="30" fill="#012169" />
      <path d="M0,0 L60,30 M60,0 L0,30" stroke="#ffffff" strokeWidth="6" />
      <path d="M0,0 L60,30 M60,0 L0,30" clipPath="url(#language-flag-en-clip)" stroke="#C8102E" strokeWidth="4" />
      <path d="M30,0 v30 M0,15 h60" stroke="#ffffff" strokeWidth="10" />
      <path d="M30,0 v30 M0,15 h60" stroke="#C8102E" strokeWidth="6" />
    </>
  ),
  de: (
    <>
      <rect width="60" height="10" fill="#000000" />
      <rect y="10" width="60" height="10" fill="#DD0000" />
      <rect y="20" width="60" height="10" fill="#FFCE00" />
    </>
  ),
  es: (
    <>
      <rect width="60" height="30" fill="#AA151B" />
      <rect y="7.5" width="60" height="15" fill="#F1BF00" />
    </>
  ),
  fr: (
    <>
      <rect width="20" height="30" fill="#0055A4" />
      <rect x="20" width="20" height="30" fill="#FFFFFF" />
      <rect x="40" width="20" height="30" fill="#EF4135" />
    </>
  ),
  'pt-BR': (
    <>
      <rect width="60" height="30" fill="#009C3B" />
      <path d="M30,3 L55,15 L30,27 L5,15 z" fill="#FFDF00" />
      <circle cx="30" cy="15" r="7" fill="#002776" />
    </>
  ),
  'zh-CN': (
    <>
      <rect width="60" height="30" fill="#DE2910" />
      <path d="M10,4 L12,10 L18,10 L13,14 L15,20 L10,16 L5,20 L7,14 L2,10 L8,10 z" fill="#FFDE00" />
    </>
  ),
  [PSEUDO_LANGUAGE]: <rect width="60" height="30" fill="#8C8C8C" />,
};

type Props = {
  code: LanguageCode;
};

const LanguageFlag = ({ code }: Props) => (
  <FlagSvg viewBox="0 0 60 30" aria-hidden="true">
    {FLAGS[code]}
  </FlagSvg>
);

export default LanguageFlag;
