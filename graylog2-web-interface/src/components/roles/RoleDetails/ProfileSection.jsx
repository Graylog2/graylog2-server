// @flow strict
import * as React from 'react';

import Role from 'logic/roles/Role';
import SectionComponent from 'components/common/Section/SectionComponent';
import ReadOnlyFormGroup from 'components/common/ReadOnlyFormGroup';

type Props = {
  role: Role,
};

const ProfileSection = ({
  role: {
    name,
    description,
  },
}: Props) => (
  <SectionComponent title="Profile">
    <ReadOnlyFormGroup label="Name" value={name} />
    <ReadOnlyFormGroup label="Description" value={description} />
  </SectionComponent>
);

export default ProfileSection;
