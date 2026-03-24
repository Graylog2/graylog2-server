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

import useInputStateSummary from 'hooks/useInputStateSummary';
import MenuItemDotBadge from 'components/navigation/MenuItemDotBadge';

type Props = {
  text: string;
  hasExternalIssues?: boolean;
  externalIssuesTitle?: string;
};

const InputsDotBadge = ({ text, hasExternalIssues = false, externalIssuesTitle = '' }: Props) => {
  const { hasProblematicInputs, isLoading } = useInputStateSummary();

  if (isLoading) {
    return <>{text}</>;
  }

  const showDot = hasProblematicInputs || hasExternalIssues;
  const title = hasProblematicInputs ? 'Some inputs are in failed state or in setup mode.' : externalIssuesTitle;

  return <MenuItemDotBadge text={text} title={title} showDot={showDot} />;
};

export default InputsDotBadge;
