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

export const DIAGRAM = `flowchart TD
  A(["User's current view"])
  A --> B{{"\`Should users remain
in their current workflow?\`"}}
  B -->|Yes| MODAL
  B -->|No| PAGE

subgraph Surface["Surface"]
MODAL(["\`**Modal**
Current context preserved\`"])
PAGE(["\`**Page**
Dedicated creation experience\`"])
end

subgraph Method["Method"]
MW(["\`**Form or Wizard**
Single-step or guided workflow\`"])
FW(["\`**Form or Wizard**
Single-step or guided workflow\`"])
end

MODAL --> MW
PAGE --> FW

MW --> RC(["\`**Return to current workflow**
Modal closes, toast shown\`"])
FW --> NC(["\`**Navigate to entity**
Details page, toast shown\`"])
`;
