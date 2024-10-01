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
import styled, { css } from 'styled-components';

import { Switch } from 'components/common';

const DestinationSwitch = styled(Switch)(({ theme }) => css`
  > label {
    margin-bottom: 0;
  }

  .mantine-Switch-input[disabled] + .mantine-Switch-track {
    background: ${theme.colors.gray[90]};

    .mantine-Switch-thumb {
      background: ${theme.colors.gray[40]};
      border-color: ${theme.colors.gray[40]};
    }
  }


  .mantine-Switch-label[data-disabled] { 
    color: ${theme.colors.text.primary};
  }
`);

export default DestinationSwitch;
