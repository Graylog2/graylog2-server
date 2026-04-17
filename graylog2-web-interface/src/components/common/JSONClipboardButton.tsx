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

import * as JSON from 'util/json';

import ClipboardButton from './ClipboardButton';

/**
 * Component that renders a button to copy the JSON value of an object into the clipboard when pressed.
 * The content to be copied can be given in the `content` prop, it can be any value that is serializable using `JSON.stringify`.
 */

type Props = Omit<React.ComponentProps<typeof ClipboardButton>, 'text'> & {
  content: any;
};

const JSONClipboardButton = ({ content, ...rest }: Props) => (
  <ClipboardButton text={JSON.stringify(content, null, 2)} {...rest} />
);
export default JSONClipboardButton;
