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

import ClipboardContainer from 'components/common/ClipboardContainer';
import { IconButton } from 'components/common';

import type { IconName } from './Icon';

/**
 * Component that renders an icon button to copy some text in the clipboard when pressed.
 * The text to be copied can be given in the `text` prop, or in an external element through a CSS selector in the `target` prop.
 */

type Props = {
  buttonTitle?: string,
  className?: string,
  disabled?: boolean,
  onSuccess?: () => void,
  text: string,
  name: IconName,
}

const ClipboardIconButton = ({ buttonTitle, className, disabled, onSuccess, text, name }: Props) => (
  <ClipboardContainer text={text}>
    {({ copy }) => (
      <IconButton className={className}
                  name={name}
                  disabled={disabled}
                  title={buttonTitle}
                  onClick={() => {
                    copy();
                    onSuccess?.();
                  }} />
    )}
  </ClipboardContainer>
);

export default ClipboardIconButton;
