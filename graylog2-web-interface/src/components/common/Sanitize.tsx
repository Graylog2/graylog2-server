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
import DOMPurify from 'dompurify';

type Props = {
  html: string | null | undefined;
  config?: DOMPurify.Config;
} & Omit<React.HTMLAttributes<HTMLSpanElement>, 'dangerouslySetInnerHTML' | 'children'>;

const Sanitize = ({ html, config = undefined, ...rest }: Props) => {
  const sanitized = DOMPurify.sanitize(html ?? '', config ?? {});

  // eslint-disable-next-line react/no-danger
  return <span {...rest} dangerouslySetInnerHTML={{ __html: sanitized }} />;
};

export default Sanitize;
