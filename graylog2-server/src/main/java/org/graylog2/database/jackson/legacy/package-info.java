/**
 * This package contains a compatibility layer to support old code using the Mongojack 2.x API. It is destined for
 * removal as soon as all code has been migrated to use the MongoDB driver API directly.
 * <p>
 * Instead of the classes from this package, use {@link org.graylog2.database.MongoCollections} as an entrypoint for
 * interacting with MongoDB.
 */
package org.graylog2.database.jackson.legacy;
