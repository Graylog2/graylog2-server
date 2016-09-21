package org.graylog2.plugin;

public class DescriptorWithHumanName extends AbstractDescriptor {
    private final String humanName;

    public DescriptorWithHumanName(String name, boolean exclusive, String linkToDocs, String humanName) {
        super(name, exclusive, linkToDocs);
        this.humanName = humanName;
    }

    public String getHumanName() {
        return humanName;
    }
}
