package org.gephi.plugins.forceAtlas2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.Graph;
import org.gephi.graph.api.GraphModel;
import org.gephi.layout.spi.LayoutBuilder;
import org.gephi.layout.spi.LayoutProperty;
import org.gephi.timeline.api.TimelineController;
import org.gephi.timeline.api.TimelineModelEvent;
import org.gephi.timeline.api.TimelineModelListener;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;

public class ForceAtlas2_EdgeWeight extends ForceAtlas2 {

    private final ForceAtlas2_EdgeWeightBuilder layoutBuilder;
    private TimelineController timelineControler;
    private TimelineModelListener timelineModelListener;
    private double[] lastInterval;
    private int edgeWeightWindow;
    private GraphModel graphModel;

    public ForceAtlas2_EdgeWeight(ForceAtlas2_EdgeWeightBuilder layoutBuilder) {
        super(null);
        this.layoutBuilder = layoutBuilder;
    }

    @Override
    public void initAlgo() {
        super.initAlgo();
        graphModel = super.getGraphModel();
        timelineControler = Lookup.getDefault().lookup(TimelineController.class);

        timelineModelListener = new TimelineModelListener() {

            @Override
            public void timelineModelChanged(TimelineModelEvent event) {
                switch (event.getEventType()) {
                    case INTERVAL:
                        double interval[] = (double[]) event.getData();
                        if (lastInterval == null) {
                            lastInterval = Arrays.copyOf(interval, interval.length);
                            return;
                        }

                        float weight = (float) Math.abs(interval[1] - lastInterval[1]);
                        Graph visibleGraph = graphModel.getGraphVisible();

                        for (Edge e : graphModel.getGraph().getEdges()) {
                            if (visibleGraph.contains(e) == false) {
                                e.setWeight((float) 1);
                                continue;
                            }

                            if (lastInterval[0] > interval[0] && lastInterval[1] > interval[1]) {
                                float newWeight = e.getWeight() - weight;
                                e.setWeight(newWeight <= 1 ? 1 : newWeight);
                            } else {
                                e.setWeight(Math.min(weight + e.getWeight(), edgeWeightWindow));
                            }
                        }

                        lastInterval = Arrays.copyOf(interval, interval.length);
                        break;
                }
            }
        };

        timelineControler.addListener(timelineModelListener);
    }
    
            /*   
        System.out.println("stats: ");
        System.out.println("\ttotalSwinging: " + totalSwinging);
        System.out.println("\ttotalEffectiveTraction: " + totalEffectiveTraction);
        System.out.println("\ttargetSpeed: " + targetSpeed);
        System.out.println("\tspeed: " + speed);
        System.out.println("\ttargetSpeed - speed: " + (targetSpeed - speed));
        */
    
    @Override
    public void endAlgo() {
        super.endAlgo();
        timelineControler.removeListener(timelineModelListener);
    }

    @Override
    public LayoutProperty[] getProperties() {
        final String FORCEATLAS2_EDGEWEIGHTS = NbBundle.getMessage(getClass(), "ForceAtlas2.edgeweights");
       
        LayoutProperty[] props = super.getProperties();      
        List<LayoutProperty> properties = new ArrayList<LayoutProperty>();
        properties.addAll(Arrays.asList(props));

        try {
            properties.add(LayoutProperty.createProperty(
                    this, Integer.class,
                    NbBundle.getMessage(getClass(), "ForceAtlas2_EdgeWeight.window.name"),
                    FORCEATLAS2_EDGEWEIGHTS,
                    "ForceAtlas2_EdgeWeight.window.name",
                    NbBundle.getMessage(getClass(), "ForceAtlas2_EdgeWeight.window.desc"),
                    "getEdgeWeightWindow", "setEdgeWeightWindow"));

        } catch (Exception e) {
            e.printStackTrace();
        }

        return properties.toArray(
                new LayoutProperty[0]);
    }

    @Override
    public void resetPropertiesValues() {
        super.resetPropertiesValues();
        setEdgeWeightWindow(5);
    }

    @Override
    public LayoutBuilder getBuilder() {
        return layoutBuilder;
    }

    public void setEdgeWeightWindow(Integer edgeWeightWindow) {
        this.edgeWeightWindow = edgeWeightWindow;
    }

    public Integer getEdgeWeightWindow() {
        return this.edgeWeightWindow;
    }
}
