import React from 'react';
import styled from 'styled-components';

import FieldsByLink from './FieldsByLink';

const FieldsToggle = () => {
  return (
    <FieldsToggleWrap>
        List fields of
      <StyledLink mode="current"
                  text="current streams"
                  key="current-fields"
                  title="This shows fields which are (prospectively) included in the streams you have selected." />,

      <StyledLink mode="all"
                  text="all"
                  key="all-fields"
                  title="This shows all fields, but no reserved (gl2_*) fields." /> or

      <StyledLink mode="allreserved"
                  text="all including reserved"
                  key="allreserved-fields"
                  title="This shows all fields, including reserved (gl2_*) fields." />
    </FieldsToggleWrap>
  );
};

const FieldsToggleWrap = styled.div`
  margin: 12px 0;
`;

const StyledLink = styled(FieldsByLink)`
  margin-left: 3px;
`;

export default FieldsToggle;
