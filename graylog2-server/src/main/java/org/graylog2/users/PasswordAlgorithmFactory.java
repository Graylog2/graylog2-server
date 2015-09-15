package org.graylog2.users;

import org.graylog2.plugin.security.PasswordAlgorithm;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Set;

public class PasswordAlgorithmFactory {
    private final Set<PasswordAlgorithm> passwordAlgorithms;
    private final PasswordAlgorithm defaultPasswordAlgorithm;

    @Inject
    public PasswordAlgorithmFactory(Set<PasswordAlgorithm> passwordAlgorithms,
                                    @DefaultPasswordAlgorithm PasswordAlgorithm defaultPasswordAlgorithm) {
        this.passwordAlgorithms = passwordAlgorithms;
        this.defaultPasswordAlgorithm = defaultPasswordAlgorithm;
    }

    @Nullable
    public PasswordAlgorithm forPassword(String hashedPassword) {
        for (PasswordAlgorithm passwordAlgorithm : passwordAlgorithms) {
            if (passwordAlgorithm.supports(hashedPassword))
                return passwordAlgorithm;
        }

        return null;
    }

    public PasswordAlgorithm defaultPasswordAlgorithm() {
        return defaultPasswordAlgorithm;
    }
}
