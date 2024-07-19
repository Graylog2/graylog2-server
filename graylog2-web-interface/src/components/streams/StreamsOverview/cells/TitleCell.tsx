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
import styled, { css } from 'styled-components';

import Routes from 'routing/Routes';
import { Link } from 'components/common/router';
import type { Stream } from 'stores/streams/StreamsStore';
import { Label } from 'components/bootstrap';
import { Text } from 'components/common';

type Props = {
  stream: Stream,
};
const DefaultLabel = styled(Label)`
  display: inline-flex;
  margin-left: 5px;
  vertical-align: inherit;
`;

const StyledText = styled(Text)(({ theme }) => css`
  color: ${theme.colors.gray[50]};
`);

const TitleCell = ({ stream }: Props) => (
  <>
    <Link to={Routes.stream_search(stream.id)}>{stream.title}</Link>
    {stream.is_default && <DefaultLabel bsStyle="primary" bsSize="xsmall">Default</DefaultLabel>}
    <StyledText>{stream.description}</StyledText>
  </>
);

export default TitleCell;
