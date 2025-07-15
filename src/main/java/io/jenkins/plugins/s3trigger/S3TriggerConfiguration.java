package io.jenkins.plugins.s3trigger;

import hudson.Extension;
import hudson.ExtensionList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import jenkins.model.GlobalConfiguration;
import org.kohsuke.stapler.DataBoundSetter;

@Extension
public class S3TriggerConfiguration extends GlobalConfiguration {
    private String token;
    private final Set<String> queue = Collections.synchronizedSet(new HashSet<>());

    public static S3TriggerConfiguration get() {
        return ExtensionList.lookupSingleton(S3TriggerConfiguration.class);
    }

    public S3TriggerConfiguration() {
        load();
    }

    @DataBoundSetter
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
        save();
    }

    public void addToQueue(String item) {
        queue.add(item);
    }

    public String pollFromQueue() {
        synchronized (queue) {
            String item = queue.stream().findFirst().orElse(null);
            if (item != null) {
                queue.remove(item);
            }
            return item;
        }
    }

    public Set<String> getQueueSnapshot() {
        synchronized (queue) {
            return new HashSet<>(queue);
        }
    }
}
