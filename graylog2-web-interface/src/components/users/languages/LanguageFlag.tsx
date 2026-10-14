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
