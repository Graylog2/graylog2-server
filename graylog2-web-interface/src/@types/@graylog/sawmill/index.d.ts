declare module '@graylog/sawmill/styled-components' {
  import SawmillSC from '@graylog/sawmill/dist/styled-components/Sawmill';

  export type { StyledComponentsTheme } from '@graylog/sawmill/dist/styled-components/Sawmill';
  export default SawmillSC;
}

declare module '@graylog/sawmill/mantine' {
  import SawmillMantine from '@graylog/sawmill/dist/mantine/Sawmill';

  export type { MantineTheme } from '@graylog/sawmill/dist/styled-components/Sawmill';
  export default SawmillMantine;
}
