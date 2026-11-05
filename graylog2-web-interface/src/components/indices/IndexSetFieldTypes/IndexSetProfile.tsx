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
import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { styled } from 'styled-components';

import useIndexProfileWithMappingsByField from 'components/indices/IndexSetFieldTypes/hooks/useIndexProfileWithMappingsByField';
import { IconButton, Link } from 'components/common';
import SetProfileModal from 'components/indices/IndexSetFieldTypes/SetProfileModal';
import isIndexFieldTypeChangeAllowed from 'components/indices/helpers/isIndexFieldTypeChangeAllowed';
import Routes from 'routing/Routes';
import type { IndexSet } from 'stores/indices/IndexSetsStore';

const Container = styled.div`
  display: flex;
  gap: 5px;
  align-items: center;
`;

type Props = {
  indexSet: IndexSet;
};

const IndexSetProfile = ({ indexSet }: Props) => {
  const { t } = useTranslation('indexsets');
  const { name, id, description } = useIndexProfileWithMappingsByField();
  const [showSetModal, setShowSetModal] = useState(false);
  const toggleModal = () => setShowSetModal((cur) => !cur);
  const title = id ? description : t('field_types.profile_not_set_yet');

  return (
    <Container title={title}>
      <b>{t('field_types.profile_label')}</b>
      {id ? (
        <Link target="_blank" to={Routes.SYSTEM.INDICES.FIELD_TYPE_PROFILES.edit(id)}>
          {name}
        </Link>
      ) : (
        <i>{t('field_types.not_set')}</i>
      )}
      <IconButton name="edit_square" onClick={toggleModal} title={t('field_types.set_profile_title')} />
      {showSetModal && (
        <SetProfileModal
          show={showSetModal}
          onClose={toggleModal}
          currentProfile={id}
          indexSets={[{ ...indexSet, can_have_profile: isIndexFieldTypeChangeAllowed(indexSet) }]}
        />
      )}
    </Container>
  );
};

export default IndexSetProfile;
