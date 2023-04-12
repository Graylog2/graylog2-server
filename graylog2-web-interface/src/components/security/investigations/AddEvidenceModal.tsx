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

import usePluginEntities from 'hooks/usePluginEntities';

import type { EvidenceTypes } from './types';

type Props = {
  index?: string,
  id: string,
  type: EvidenceTypes,
};

const AddEvidenceModal = React.forwardRef(({ index, id, type }: Props, ref: React.MutableRefObject<{ toggle: () => void }>) => {
  const investigations = usePluginEntities('investigationsPlugin');

  if (!investigations || !investigations.length) return null;

  const SecAddEvidenceModal = investigations[0].components.AddEvidenceModal;

  return (
    <SecAddEvidenceModal id={id} index={index} type={type} ref={ref} />
  );
});

AddEvidenceModal.defaultProps = {
  index: undefined,
};

export default AddEvidenceModal;
