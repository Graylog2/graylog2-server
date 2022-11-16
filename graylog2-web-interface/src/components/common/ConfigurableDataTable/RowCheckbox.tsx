import styled from 'styled-components';

import { Checkbox } from 'components/bootstrap';

const RowCheckbox = styled(Checkbox)`
  &.checkbox {
    margin: 0;
  
    label {
      display: flex;
      align-items: center;
      padding: 0;
      
      input {
        cursor: pointer;
        margin: 0;
        position: relative;
      }
    }
  }
`;

export default RowCheckbox;
