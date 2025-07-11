package org.graylog2.commands.Token;

import com.github.rvesse.airline.annotations.Arguments;
import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.restrictions.Required;
import com.google.inject.Module;
import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import org.graylog2.featureflag.FeatureFlags;
import org.graylog2.security.AccessTokenService;
import org.graylog2.security.AccessTokenServiceImpl;

/**
 * This command will insert/update an automation API token with a fixed ID for the admin user
 */
@Command(name = "automation-token", description = "Adds an automation API token for the local admin user.")
public class AutomationTokenCommand extends AbstractTokenCommand {

    @Arguments(description = "Value of the automation token to be created (if it doesn't exist yet).")
    @Required
    private String value;

    public AutomationTokenCommand() {
        super("automation-token");
    }

    @Override
    protected @Nonnull List<Module> getNodeCommandBindings(FeatureFlags featureFlags) {
        final ArrayList<Module> modules = new ArrayList<>(super.getNodeCommandBindings(featureFlags));
        modules.add(binder -> binder.bind(AccessTokenService.class).to(AccessTokenServiceImpl.class));
        return modules;
    }

    @Override
    protected void startCommand() {
        injector.getInstance(TokenCommandExecution.class).run(value);
    }
}
