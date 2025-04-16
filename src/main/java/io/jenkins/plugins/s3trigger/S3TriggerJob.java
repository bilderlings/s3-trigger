package io.jenkins.plugins.s3trigger;

import hudson.Extension;
import hudson.model.*;
import java.io.IOException;
import java.util.logging.Logger;
import jenkins.model.Jenkins;

@Extension
public class S3TriggerJob extends AsyncPeriodicWork {
    private static final Logger LOGGER = Logger.getLogger(S3TriggerJob.class.getName());

    public S3TriggerJob() {
        super(S3TriggerJob.class.getName());
    }

    public S3TriggerJob(String name) {
        super(name);
    }

    @Override
    protected void execute(TaskListener listener) throws IOException, InterruptedException {
        LOGGER.info("Checking is s3 triggered");

        var config = S3TriggerConfiguration.get();

        String name;
        while ((name = config.pollFromQueue()) != null) {
            LOGGER.info("Pulled " + name + " from queue");

            var job = Jenkins.get().getItemByFullName(name, BuildableItem.class);
            if (job == null) {
                LOGGER.warning("Unable to find job with name " + name);
                continue;
            }

            var res = job.scheduleBuild(new Cause.RemoteCause("s3", "S3 triggered"));
            if (!res) {
                LOGGER.warning("Unable to schedule build for name " + name);
                return;
            }

            LOGGER.warning("Build scheduled for job " + name);
        }
    }

    @Override
    public long getRecurrencePeriod() {
        return 60 * 1000;
    }
}
