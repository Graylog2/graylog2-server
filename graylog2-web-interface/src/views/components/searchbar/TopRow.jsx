// @flow strict
import styled from 'styled-components';
import type { StyledComponent } from 'styled-components';

import { Row } from 'components/graylog';

const TopRow: StyledComponent<{}, void, Row> = styled(Row)`
  margin-bottom: 10px;
`;

export default TopRow;
