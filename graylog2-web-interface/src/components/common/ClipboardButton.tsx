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
import { CopyButton, Tooltip } from '@mantine/core';

import { Button } from 'components/bootstrap';
import type { BsSize } from 'components/bootstrap/types';
import type { StyleProps } from 'components/bootstrap/Button';

/**
 * Component that renders a button to copy some text in the clipboard when pressed.
 * The text to be copied can be given in the `text` prop, or in an external element through a CSS selector in the `target` prop.
 */

type Props = {
  bsSize?: BsSize,
  bsStyle?: StyleProps,
  buttonTitle?: string,
  className?: string,
  disabled?: boolean,
  onSuccess?: () => void,
  text: string,
  title: React.ReactNode,
}

const ClipboardButton = ({ bsSize, bsStyle, buttonTitle, className, disabled, onSuccess, text, title }: Props) => {
  const button = (copy: () => void) => (
    <Button bsSize={bsSize}
            bsStyle={bsStyle}
            className={className}
            disabled={disabled}
            title={buttonTitle}
            onClick={() => {
              copy();
              onSuccess?.();
            }}>
      {title}
    </Button>
  );

  return (
    <CopyButton value={text} timeout={2000}>
      {({ copied, copy }) => (copied ? (
        <Tooltip label="Copied!" withArrow position="top" opened>
          {button(copy)}
        </Tooltip>
      ) : button(copy))}
    </CopyButton>
  );
};

ClipboardButton.defaultProps = {
  bsSize: undefined,
  bsStyle: undefined,
  buttonTitle: undefined,
  className: undefined,
  disabled: undefined,
  onSuccess: undefined,
};

export default ClipboardButton;
