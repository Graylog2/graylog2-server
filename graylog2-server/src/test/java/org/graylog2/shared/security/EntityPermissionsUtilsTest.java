package org.graylog2.shared.security;

import org.apache.shiro.subject.Subject;
import org.graylog2.database.DbEntity;
import org.graylog2.database.dbcatalog.DbEntitiesCatalog;
import org.graylog2.database.dbcatalog.DbEntityCatalogEntry;
import org.graylog2.streams.StreamImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class EntityPermissionsUtilsTest {

    @Mock
    private DbEntitiesCatalog catalog;
    @Mock
    private EntityPermissionsUtils toTest;
    @Mock
    private Subject subject;

    @BeforeEach
    void setUp() {
        toTest = new EntityPermissionsUtils(catalog);
    }

    @Test
    void hasReadPermissionForWholeCollectionReturnsFalseOnNoEntryInCatalog() {
        doReturn(Optional.empty()).when(catalog).getByCollectionName("streams");

        final boolean hasReadPermissions = toTest.hasReadPermissionForWholeCollection(subject, "streams");
        assertFalse(hasReadPermissions);
    }

    @Test
    void hasReadPermissionForWholeCollectionReturnsFalseWhenCatalogHasNullPermission() {
        doReturn(Optional.of(
                new DbEntityCatalogEntry("streams", "title", StreamImpl.class, null))
        ).when(catalog)
                .getByCollectionName("streams");

        final boolean hasReadPermissions = toTest.hasReadPermissionForWholeCollection(subject, "streams");
        assertFalse(hasReadPermissions);
    }

    @Test
    void hasReadPermissionForWholeCollectionReturnsTrueWhenCatalogHasAllAllowedPermission() {
        doReturn(Optional.of(
                new DbEntityCatalogEntry("streams", "title", StreamImpl.class, DbEntity.ALL_ALLOWED))
        ).when(catalog)
                .getByCollectionName("streams");

        final boolean hasReadPermissions = toTest.hasReadPermissionForWholeCollection(subject, "streams");
        assertTrue(hasReadPermissions);
    }

    @Test
    void hasReadPermissionForWholeCollectionReturnsFalseWhenSubjectMissesPermission() {
        doReturn(Optional.of(
                new DbEntityCatalogEntry("streams", "title", StreamImpl.class, "streams:read"))
        ).when(catalog)
                .getByCollectionName("streams");

        doReturn(false).when(subject).isPermitted("streams:read:*");
        final boolean hasReadPermissions = toTest.hasReadPermissionForWholeCollection(subject, "streams");
        assertFalse(hasReadPermissions);
    }

    @Test
    void hasReadPermissionForWholeCollectionReturnsTrueWhenSubjectHasPermission() {
        doReturn(Optional.of(
                new DbEntityCatalogEntry("streams", "title", StreamImpl.class, "streams:read"))
        ).when(catalog)
                .getByCollectionName("streams");

        doReturn(true).when(subject).isPermitted("streams:read:*");
        final boolean hasReadPermissions = toTest.hasReadPermissionForWholeCollection(subject, "streams");
        assertTrue(hasReadPermissions);
    }

}
