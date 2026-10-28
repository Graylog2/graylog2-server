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
import { useState } from 'react';
import { useTranslation } from 'react-i18next';

import { Button } from 'components/bootstrap';
import ChangeFieldTypeModal from 'views/logic/fieldactions/ChangeFieldType/ChangeFieldTypeModal';

type Props = {
  indexSetId: string;
};

const ChangeFieldTypeButton = ({ indexSetId }: Props) => {
  const { t } = useTranslation('indexsets');
  const [showModal, setShowModal] = useState(false);
  const toggleModal = () => setShowModal((cur) => !cur);

  return (
    <>
      <Button bsStyle="primary" onClick={toggleModal}>
        {t('field_types.change_field_type')}
      </Button>
      {showModal && (
        <ChangeFieldTypeModal
          initialSelectedIndexSets={[indexSetId]}
          onClose={toggleModal}
          showFieldSelect
          show
          showSelectionTable={false}
        />
      )}
    </>
  );
};

export default ChangeFieldTypeButton;
