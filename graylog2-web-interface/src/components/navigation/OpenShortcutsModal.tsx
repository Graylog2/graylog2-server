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
import styled, { css } from 'styled-components';

import { IconButton } from 'components/common';
import useHotkeysContext from 'hooks/useHotkeysContext';

const StyledIconButton = styled(IconButton)(({ theme }) => css`
  color: ${theme.colors.global.textDefault};
  margin-right: 9px;
`);

const OpenShortcutsModal = () => {
  const { setShowHotkeysModal } = useHotkeysContext();

  return <StyledIconButton onClick={() => setShowHotkeysModal(true)} name="keyboard" title="Keyboard Shortcuts" size="lg" />;
};

export default OpenShortcutsModal;
