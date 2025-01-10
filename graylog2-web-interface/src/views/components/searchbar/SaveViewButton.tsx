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
import { forwardRef } from 'react';
import styled, { css } from 'styled-components';

import { Icon } from 'components/common';
import useIsDirty from 'views/hooks/useIsDirty';
import { Button } from 'components/bootstrap';

const StyledIcon = styled(Icon)<{ $isDirty: boolean }>(({ theme, $isDirty }) => css`
  color: ${$isDirty ? theme.colors.variant.dark.warning : 'default'};
`);

type Props = {
  title: string,
  onClick: () => void,
  disabled?: boolean,
}

const SaveViewButton = forwardRef<HTMLButtonElement, Props>(({ title, onClick, disabled = false }, ref) => {
  const isDirty = useIsDirty();

  return (
    <Button title={title}
            ref={ref}
            onClick={onClick}
            disabled={disabled}>
      <StyledIcon name="save" type={isDirty ? 'solid' : 'regular'} $isDirty={!disabled && isDirty} /> Save
    </Button>
  );
});

export default SaveViewButton;
