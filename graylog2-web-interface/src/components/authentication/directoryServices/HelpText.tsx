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
import { Trans } from 'react-i18next';

const COMPONENTS = {
  code: <code />,
  emphasis: <i />,
  exampleCode: <code className="text-nowrap" />,
  kbd: <kbd />,
  lineBreak: <br />,
};

type Props = {
  i18nKey: string;
  values?: { [key: string]: string };
};

// Translated at render time, so help texts can be defined as module-level constants.
const HelpText = ({ i18nKey, values = undefined }: Props) => (
  <span>
    <Trans ns="authentication" i18nKey={i18nKey} values={values} components={COMPONENTS} />
  </span>
);

export default HelpText;
