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
import { useCallback, useState } from 'react';
import { useTimeout } from '@mantine/hooks';

import copyToClipboard from 'util/copyToClipboard';
import Tooltip from 'components/common/Tooltip';

/**
 * This component can be used as a wrapper for other components. When users click on the children,
 * the provided text will be copied to the clipboard, and they see a tooltip for visual feedback.
 */

type Props = {
  children: (props: { copy: () => void }) => JSX.Element,
  text: string,
}

type Args = {
  copied: boolean,
  copy: () => void,
}

type CopyProps = {
  value: string,
  timeout: number,
  children: (args: Args) => React.ReactElement,
};

const Copy = ({ children, value, timeout }: CopyProps) => {
  const [copied, setCopied] = useState(false);
  const { start } = useTimeout(() => setCopied(false), timeout);
  const copy = useCallback(() => copyToClipboard(value).then(() => { setCopied(true); start(); }), [start, value]);

  return children({ copied, copy });
};

const ClipboardContainer = ({ children, text }: Props) => (
  <Copy value={text} timeout={2000}>
    {({ copied, copy }) => (copied ? (
      <Tooltip label="Copied!" withArrow position="top" opened>
        {children({ copy })}
      </Tooltip>
    ) : children({ copy }))}
  </Copy>
);

export default ClipboardContainer;
