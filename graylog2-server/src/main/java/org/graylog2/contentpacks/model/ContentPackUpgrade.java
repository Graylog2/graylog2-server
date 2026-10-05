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
package org.graylog2.contentpacks.model;

/**
 * Result of {@link org.graylog2.contentpacks.ContentPackService#upgradeContentPack}.
 * Bundles the new installation with a snapshot of old entity state captured before the
 * in-place update, so callers can preserve user customizations (notifications, search filters, etc.).
 */
public record ContentPackUpgrade(ContentPackInstallation installation, ContentPackUninstallation oldEntitySnapshots) {
}
