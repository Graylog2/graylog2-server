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
import React, { useEffect, useState } from 'react';
import styled, { css } from 'styled-components';

import { Modal } from 'components/bootstrap';
import { Spinner } from 'components/common';
import useSelectedIndexSetTemplate from 'components/indices/IndexSetTemplates/hooks/useSelectedTemplate';
import useTemplates from 'components/indices/IndexSetTemplates/hooks/useTemplates';

type Props = {
  show: boolean,
  hideModal: () => void,
}

const SelectIndexSetTemplateModal = ({ hideModal, show }: Props) => {
  const { selectedIndexSetTemplate, setSelectedIndexSetTemplate } = useSelectedIndexSetTemplate();
  // const {
  //   isLoading,
  //   data: { list },
  // } = useTemplates(
  //   {
  //       page: number,
  // pageSize: 3,
  // query: string,
  // sort: Sort
  // filters?: UrlQueryFilters},
  //   { enabled: !isLoadingLayoutPreferences },
  // );

  return (
    <Modal show={show}
           title="Index Set Strategy"
           bsSize="large"
           onHide={hideModal}>
      <Modal.Header closeButton>
        <Modal.Title>Index Set Strategy</Modal.Title>
      </Modal.Header>
      Select a template apprpriate to the requirements for this data (and available storage).

    </Modal>
  );
};

export default SelectIndexSetTemplateModal;
