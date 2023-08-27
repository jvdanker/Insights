package net.vdanker;

import org.eclipse.jgit.lib.ProgressMonitor;

public class SimpleProgressMonitor implements ProgressMonitor {
    @Override
    public void start(int totalTasks) {
        System.out.println(totalTasks);
    }

    @Override
    public void beginTask(String title, int totalWork) {
        System.out.printf("%s %d\n", title, totalWork);
    }

    @Override
    public void update(int completed) {
        System.out.println(completed);
    }

    @Override
    public void endTask() {

    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public void showDuration(boolean enabled) {

    }
}
