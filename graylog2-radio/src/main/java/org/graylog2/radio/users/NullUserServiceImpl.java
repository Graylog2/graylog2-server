package org.graylog2.radio.users;

import org.graylog2.plugin.database.Persisted;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.database.users.User;
import org.graylog2.plugin.database.validators.ValidationResult;
import org.graylog2.plugin.database.validators.Validator;
import org.graylog2.shared.security.ldap.LdapEntry;
import org.graylog2.shared.security.ldap.LdapSettings;
import org.graylog2.shared.users.UserService;

import java.util.List;
import java.util.Map;

public class NullUserServiceImpl implements UserService {
    @Override
    public User load(String username) {
        return null;
    }

    @Override
    public User create() {
        return null;
    }

    @Override
    public List<User> loadAll() {
        return null;
    }

    @Override
    public User syncFromLdapEntry(LdapEntry userEntry, LdapSettings ldapSettings, String username) {
        return null;
    }

    @Override
    public void updateFromLdap(User user, LdapEntry userEntry, LdapSettings ldapSettings, String username) {

    }

    @Override
    public User getAdminUser() {
        return null;
    }

    @Override
    public <T extends Persisted> int destroy(T model) {
        return 0;
    }

    @Override
    public <T extends Persisted> int destroyAll(Class<T> modelClass) {
        return 0;
    }

    @Override
    public <T extends Persisted> String save(T model) throws ValidationException {
        return null;
    }

    @Override
    public <T extends Persisted> String saveWithoutValidation(T model) {
        return null;
    }

    @Override
    public <T extends Persisted> Map<String, List<ValidationResult>> validate(T model, Map<String, Object> fields) {
        return null;
    }

    @Override
    public <T extends Persisted> Map<String, List<ValidationResult>> validate(T model) {
        return null;
    }

    @Override
    public Map<String, List<ValidationResult>> validate(Map<String, Validator> validators, Map<String, Object> fields) {
        return null;
    }
}
