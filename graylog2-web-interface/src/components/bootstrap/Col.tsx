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
import { forwardRef } from 'react';
import styled, { css } from 'styled-components';

// Direct port of Bootstrap 3's `.col-*-*` rules. Breakpoint values are taken
// from `public/stylesheets/bootstrap-config.json` (`@screen-xs: 480px`,
// `@screen-sm: 992px`, `@screen-md: 992px`, `@screen-lg: 1200px`). `sm` and
// `md` resolve to the same breakpoint in this project; the `md` block is
// emitted after `sm` so it wins ties, matching Bootstrap's CSS source order.
const BP_SM = 992;
const BP_MD = 992;
const BP_LG = 1200;

const widthPct = (n: number) => `${(n / 12) * 100}%`;

type ColSize = number;
type ColOffset = number;
type ColShift = number;

type Props = React.PropsWithChildren<{
  xs?: ColSize;
  sm?: ColSize;
  md?: ColSize;
  lg?: ColSize;
  xsOffset?: ColOffset;
  smOffset?: ColOffset;
  mdOffset?: ColOffset;
  lgOffset?: ColOffset;
  xsPush?: ColShift;
  smPush?: ColShift;
  mdPush?: ColShift;
  lgPush?: ColShift;
  xsPull?: ColShift;
  smPull?: ColShift;
  mdPull?: ColShift;
  lgPull?: ColShift;
  xsHidden?: boolean;
  smHidden?: boolean;
  mdHidden?: boolean;
  lgHidden?: boolean;
  componentClass?: React.ElementType;
  className?: string;
  style?: React.CSSProperties;
  id?: string;
  'data-testid'?: string;
}>;

type StyledProps = {
  $xs?: ColSize;
  $sm?: ColSize;
  $md?: ColSize;
  $lg?: ColSize;
  $xsOffset?: ColOffset;
  $smOffset?: ColOffset;
  $mdOffset?: ColOffset;
  $lgOffset?: ColOffset;
  $xsPush?: ColShift;
  $smPush?: ColShift;
  $mdPush?: ColShift;
  $lgPush?: ColShift;
  $xsPull?: ColShift;
  $smPull?: ColShift;
  $mdPull?: ColShift;
  $lgPull?: ColShift;
  $xsHidden?: boolean;
  $mdHidden?: boolean;
  $lgHidden?: boolean;
};

// Bootstrap scopes the col defaults (relative positioning, 1px min-height,
// 15px horizontal padding) to elements with an actual `col-*-N` class. Apply
// them only when at least one size prop is set, so a bare `<Col>` (used in
// places as a generic container) renders as a plain div like in RB.
const hasAnySize = ({ $xs, $sm, $md, $lg }: Pick<StyledProps, '$xs' | '$sm' | '$md' | '$lg'>) =>
  $xs !== undefined || $sm !== undefined || $md !== undefined || $lg !== undefined;

const StyledCol = styled.div<StyledProps>`
  ${(p) =>
    hasAnySize(p) &&
    css`
      position: relative;
      min-height: 1px;
      padding-left: 15px;
      padding-right: 15px;
    `}

  ${({ $xs }) =>
    $xs !== undefined &&
    css`
      float: left;
      width: ${widthPct($xs)};
    `}
  ${({ $xsOffset }) =>
    $xsOffset !== undefined &&
    css`
      margin-left: ${widthPct($xsOffset)};
    `}
  ${({ $xsPush }) =>
    $xsPush !== undefined &&
    css`
      left: ${widthPct($xsPush)};
    `}
  ${({ $xsPull }) =>
    $xsPull !== undefined &&
    css`
      right: ${widthPct($xsPull)};
    `}
  ${({ $xsHidden }) =>
    $xsHidden &&
    css`
      @media (max-width: ${BP_SM - 1}px) {
        display: none !important;
      }
    `}

  @media (min-width: ${BP_SM}px) {
    ${({ $sm }) =>
      $sm !== undefined &&
      css`
        float: left;
        width: ${widthPct($sm)};
      `}
    ${({ $smOffset }) =>
      $smOffset !== undefined &&
      css`
        margin-left: ${widthPct($smOffset)};
      `}
    ${({ $smPush }) =>
      $smPush !== undefined &&
      css`
        left: ${widthPct($smPush)};
      `}
    ${({ $smPull }) =>
      $smPull !== undefined &&
      css`
        right: ${widthPct($smPull)};
      `}
  }

  @media (min-width: ${BP_MD}px) {
    ${({ $md }) =>
      $md !== undefined &&
      css`
        float: left;
        width: ${widthPct($md)};
      `}
    ${({ $mdOffset }) =>
      $mdOffset !== undefined &&
      css`
        margin-left: ${widthPct($mdOffset)};
      `}
    ${({ $mdPush }) =>
      $mdPush !== undefined &&
      css`
        left: ${widthPct($mdPush)};
      `}
    ${({ $mdPull }) =>
      $mdPull !== undefined &&
      css`
        right: ${widthPct($mdPull)};
      `}
  }
  ${({ $mdHidden }) =>
    $mdHidden &&
    css`
      @media (min-width: ${BP_MD}px) and (max-width: ${BP_LG - 1}px) {
        display: none !important;
      }
    `}

  @media (min-width: ${BP_LG}px) {
    ${({ $lg }) =>
      $lg !== undefined &&
      css`
        float: left;
        width: ${widthPct($lg)};
      `}
    ${({ $lgOffset }) =>
      $lgOffset !== undefined &&
      css`
        margin-left: ${widthPct($lgOffset)};
      `}
    ${({ $lgPush }) =>
      $lgPush !== undefined &&
      css`
        left: ${widthPct($lgPush)};
      `}
    ${({ $lgPull }) =>
      $lgPull !== undefined &&
      css`
        right: ${widthPct($lgPull)};
      `}
  }
  ${({ $lgHidden }) =>
    $lgHidden &&
    css`
      @media (min-width: ${BP_LG}px) {
        display: none !important;
      }
    `}
`;

const Col = (
  {
    xs = undefined,
    sm = undefined,
    md = undefined,
    lg = undefined,
    xsOffset = undefined,
    smOffset = undefined,
    mdOffset = undefined,
    lgOffset = undefined,
    xsPush = undefined,
    smPush = undefined,
    mdPush = undefined,
    lgPush = undefined,
    xsPull = undefined,
    smPull = undefined,
    mdPull = undefined,
    lgPull = undefined,
    xsHidden = false,
    // `sm` resolves to the same breakpoint as `md` in this project's RB
    // config, and the .hidden-sm media query (min-width: 992px and
    // max-width: 991px) is an empty range — so `smHidden` is a no-op
    // visually, matching what Bootstrap renders.
    smHidden = false,
    mdHidden = false,
    lgHidden = false,
    componentClass = undefined,
    className = undefined,
    style = undefined,
    id = undefined,
    children = undefined,
    'data-testid': dataTestid = undefined,
  }: Props,
  ref: React.ForwardedRef<HTMLDivElement>,
) => {
  // `smHidden` is intentionally ignored (see above).
  void smHidden;

  return (
    <StyledCol
      ref={ref}
      as={componentClass}
      $xs={xs}
      $sm={sm}
      $md={md}
      $lg={lg}
      $xsOffset={xsOffset}
      $smOffset={smOffset}
      $mdOffset={mdOffset}
      $lgOffset={lgOffset}
      $xsPush={xsPush}
      $smPush={smPush}
      $mdPush={mdPush}
      $lgPush={lgPush}
      $xsPull={xsPull}
      $smPull={smPull}
      $mdPull={mdPull}
      $lgPull={lgPull}
      $xsHidden={xsHidden}
      $mdHidden={mdHidden}
      $lgHidden={lgHidden}
      className={className}
      style={style}
      id={id}
      data-testid={dataTestid}>
      {children}
    </StyledCol>
  );
};

/** @component */
export default forwardRef(Col);
