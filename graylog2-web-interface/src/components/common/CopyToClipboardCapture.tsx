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

type Props = {
  children: React.ReactNode,
  className?: string,
  formatSelection: (originalSelection: Selection) => Selection | string
}

/**
 * This component calls `formatSelection` every time the user copies encapsulated content (`children`) to the clipboard.
 * `formatSelection` allows to format the selected content before it gets stored in the clipboard.
 */
const CopyToClipboardCapture = React.forwardRef<HTMLDivElement, Props>(({ formatSelection, children, className }: Props, ref) => {
  const _onCopy = (event) => {
    const selection = formatSelection(document.getSelection());
    event.clipboardData.setData('text/plain', selection);
    event.preventDefault();
  };

  return (
    <div className={className} ref={ref} onCopy={_onCopy}>
      {children}
    </div>
  );
});

CopyToClipboardCapture.defaultProps = {
  className: undefined,
};

export default CopyToClipboardCapture;
