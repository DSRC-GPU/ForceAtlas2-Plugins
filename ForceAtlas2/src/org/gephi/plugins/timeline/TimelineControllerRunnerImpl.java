/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.gephi.plugins.timeline;

import java.util.Iterator;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import org.gephi.timeline.TimelineControllerImpl;
import org.gephi.timeline.TimelineModelImpl;
import org.gephi.timeline.api.TimelineController;
import org.gephi.timeline.api.TimelineModel;
import org.gephi.timeline.api.TimelineModelEvent;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;

@ServiceProviders(value={
    @ServiceProvider(service = TimelineController.class, supersedes = {"org.gephi.timeline.TimelineControllerImpl"}),
    @ServiceProvider(service = TimelineControllerRunner.class)
})

public class TimelineControllerRunnerImpl extends TimelineControllerImpl implements TimelineControllerRunner {
    private final WeakHashMap<Runnable, Void> runners;
    private ScheduledExecutorService playExecutor;

    public TimelineControllerRunnerImpl() {
        super();
        this.setEnabled(true);
        runners = new WeakHashMap<Runnable, Void>();
    }

    private boolean playStep() {
        TimelineModel model = getModel();

        double min = model.getCustomMin();
        double max = model.getCustomMax();
        double duration = max - min;
        double step = (duration * model.getPlayStep()) * 0.95;
        double from = model.getIntervalStart();
        double to = model.getIntervalEnd();
        boolean bothBounds = model.getPlayMode().equals(TimelineModel.PlayMode.TWO_BOUNDS);
        boolean someAction = false;

        if (bothBounds) {
            if (step > 0 && to < max) {
                from += step;
                to += step;
                someAction = true;
            } else if (step < 0 && from > min) {
                from += step;
                to += step;
                someAction = true;
            }
        } else {
            if (step > 0 && to < max) {
                to += step;
                someAction = true;
            } else if (step < 0 && from > min) {
                from += step;
                someAction = true;
            }
        }

        if (someAction) {
            from = Math.max(from, min);
            to = Math.min(to, max);
            setInterval(from, to);
            return true;
        } else {
            stopPlay();
            return false;
        }
    }
    
    private synchronized void executeRunners() {
        Iterator<Runnable> iterator = runners.keySet().iterator();
        while (iterator.hasNext()) {
            Runnable runnable = iterator.next();
            if (runnable != null) {
                runnable.run();
            }
        }
    }

    @Override
    public void startPlay() {
         TimelineModelImpl model = (TimelineModelImpl) getModel();
         
        if (model != null && !model.isPlaying()) {
            model.setPlaying(true);
            playExecutor = Executors.newScheduledThreadPool(1, new ThreadFactory() {

                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, "Timeline animator");
                }
            });
         
            playExecutor.scheduleAtFixedRate(new Runnable() {

                @Override
                public void run() {
                    do {
                        executeRunners();
                    } while(playStep());
                }
            }, model.getPlayDelay(), model.getPlayDelay(), TimeUnit.MILLISECONDS);
            
            fireTimelineModelEvent(new TimelineModelEvent(TimelineModelEvent.EventType.PLAY_START, model, null));
        }
    }
    
    @Override
    public void stopPlay() {
        TimelineModelImpl model = (TimelineModelImpl) getModel();
        if (model != null && model.isPlaying()) {
            model.setPlaying(false);
            
        fireTimelineModelEvent(new TimelineModelEvent(TimelineModelEvent.EventType.PLAY_STOP, model, null));
        if (playExecutor != null)
            playExecutor.shutdown();

        }
    }

    @Override
    public synchronized void addRunner(TimelineModelRunner r) {
        if (!runners.containsKey(r))
            runners.put(r, null);
    }
    
    @Override
    public synchronized void removeRunner(TimelineModelRunner r) {
        runners.remove(r);
    }
}
