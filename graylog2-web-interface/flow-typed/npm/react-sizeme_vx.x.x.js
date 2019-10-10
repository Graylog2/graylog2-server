// flow-typed signature: 7c522c7906f90428fb6015d99103ab11
// flow-typed version: <<STUB>>/react-sizeme_v2.6.7/flow_v0.103.0

/**
 * Adapted from https://github.com/ctrlplusb/react-sizeme/blob/v2.6.7/react-sizeme.d.ts
 */

declare module 'react-sizeme' {
  declare type Omit<T, K> = $Rest<T, K>;

  declare export type SizeMeProps = {
    size: {
      width: number | null,
      height: number | null,
    }
  };

  declare export type SizeMeOptions = {
    monitorWidth?: boolean,
    monitorHeight?: boolean,
    monitorPosition?: boolean,
    refreshRate?: number,
    refreshMode?: 'throttle' | 'debounce',
    noPlaceholder?: boolean,
  };

  declare export type SizeMeRenderProps = SizeMeOptions & {
    children: (props: SizeMeProps) => ReactElement,
  };

  declare export class SizeMe extends Component<SizeMeRenderProps> {}

  declare export function withSize(options?: SizeMeOptions): (<P>(component: ComponentType<P>) => ComponentType<Omit<P, 'size'>>);
}
