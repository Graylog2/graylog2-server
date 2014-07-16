package org.graylog2.plugin;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public interface GenericHost {
    public boolean isProcessing();
    public boolean isServer();
    public boolean isRadio();
}
