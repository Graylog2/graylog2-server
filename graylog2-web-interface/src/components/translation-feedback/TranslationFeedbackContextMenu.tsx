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
import { createPortal } from 'react-dom';
import styled, { css } from 'styled-components';
import { useTranslation } from 'react-i18next';

import i18n from 'i18n';
import zIndices from 'theme/z-indices';
import { findTranslations } from 'util/i18n/renderedTranslations';

import { domPathOfTarget, isEditableTarget, textOfTarget } from './captureTarget';
import type { FeedbackTarget } from './TranslationFeedbackModal';
import TranslationFeedbackModal from './TranslationFeedbackModal';

const FEEDBACK_ELEMENT = '[data-translation-feedback]';

const Menu = styled.div<{ $x: number; $y: number }>(
  ({ theme, $x, $y }) => css`
    position: fixed;
    left: ${$x}px;
    top: ${$y}px;
    z-index: ${zIndices.dropdownMenu};
    min-width: 200px;
    max-width: 320px;
    padding: ${theme.spacings.xxs} 0;
    background-color: ${theme.colors.global.contentBackground};
    border: 1px solid ${theme.colors.variant.lighter.default};
    border-radius: 4px;
    box-shadow: 0 4px 12px rgb(0 0 0 / 20%);
  `,
);

const MenuItem = styled.button(
  ({ theme }) => css`
    display: block;
    width: 100%;
    padding: ${theme.spacings.xs} ${theme.spacings.sm};
    border: 0;
    background: none;
    text-align: left;
    color: ${theme.colors.text.primary};

    &:hover,
    &:focus {
      background-color: ${theme.colors.variant.lightest.default};
    }
  `,
);

const MenuHint = styled.div(
  ({ theme }) => css`
    padding: 0 ${theme.spacings.sm} ${theme.spacings.xxs};
    color: ${theme.colors.gray[50]};
    font-size: ${theme.fonts.size.small};
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  `,
);

const MENU_WIDTH = 320;
const MENU_HEIGHT = 90;

type MenuState = { x: number; y: number; target: FeedbackTarget };

/**
 * Lets proofreaders report a translation by right-clicking it. Shift + right-click and form fields keep the
 * browser's context menu.
 */
const TranslationFeedbackContextMenu = () => {
  const { t } = useTranslation('translation_feedback');
  const [menu, setMenu] = useState<MenuState | null>(null);
  const [reported, setReported] = useState<FeedbackTarget | null>(null);

  useEffect(() => {
    const onContextMenu = (event: MouseEvent) => {
      const element = event.target instanceof Element ? event.target : null;

      if (
        event.shiftKey ||
        event.defaultPrevented ||
        isEditableTarget(event.target) ||
        element?.closest(FEEDBACK_ELEMENT)
      ) {
        return;
      }

      event.preventDefault();

      const text = textOfTarget(event.target);

      setMenu({
        x: Math.min(event.clientX, window.innerWidth - MENU_WIDTH),
        y: Math.min(event.clientY, window.innerHeight - MENU_HEIGHT),
        target: {
          text,
          candidates: findTranslations(text),
          language: i18n.language,
          path: `${window.location.pathname}${window.location.search}`,
          domPath: domPathOfTarget(event.target),
        },
      });
    };

    document.addEventListener('contextmenu', onContextMenu);

    return () => document.removeEventListener('contextmenu', onContextMenu);
  }, []);

  useEffect(() => {
    if (!menu) {
      return undefined;
    }

    const close = () => setMenu(null);
    const onMouseDown = (event: MouseEvent) => {
      if (!(event.target instanceof Element && event.target.closest(FEEDBACK_ELEMENT))) {
        close();
      }
    };
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        close();
      }
    };

    document.addEventListener('mousedown', onMouseDown);
    document.addEventListener('keydown', onKeyDown);
    window.addEventListener('scroll', close, true);
    window.addEventListener('resize', close);

    return () => {
      document.removeEventListener('mousedown', onMouseDown);
      document.removeEventListener('keydown', onKeyDown);
      window.removeEventListener('scroll', close, true);
      window.removeEventListener('resize', close);
    };
  }, [menu]);

  const onReport = () => {
    setReported(menu.target);
    setMenu(null);
  };

  return (
    <>
      {menu &&
        createPortal(
          <Menu $x={menu.x} $y={menu.y} role="menu" data-translation-feedback>
            <MenuItem type="button" role="menuitem" onClick={onReport} autoFocus>
              {t('menu.report')}
            </MenuItem>
            {menu.target.text && <MenuHint title={menu.target.text}>{menu.target.text}</MenuHint>}
            <MenuHint>{t('menu.native_menu_hint')}</MenuHint>
          </Menu>,
          document.body,
        )}
      {reported && <TranslationFeedbackModal target={reported} onClose={() => setReported(null)} />}
    </>
  );
};

export default TranslationFeedbackContextMenu;
