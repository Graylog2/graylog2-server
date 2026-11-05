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
import { useEffect, useState } from 'react';

import i18n from 'i18n';
import Spinner from 'components/common/Spinner';
import applyLanguage from 'util/applyLanguage';

type Props = React.PropsWithChildren<{
  language: string;
}>;

/**
 * Applies the language before rendering the children for the first time, so they don't show up in English first.
 * Later changes keep the children rendered and switch once the translations are loaded.
 */
const LanguageProvider = ({ language, children = undefined }: Props) => {
  const [isReady, setIsReady] = useState(() => i18n.language === language);

  useEffect(() => {
    // If the translations can't be loaded, the current language stays active.
    applyLanguage(language)
      .catch(() => {})
      .finally(() => setIsReady(true));
  }, [language]);

  if (!isReady) {
    return <Spinner />;
  }

  return children;
};

export default LanguageProvider;
