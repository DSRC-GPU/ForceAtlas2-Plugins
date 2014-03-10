package org.gephi.plugins.filters.ForceAtlasFilter;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.swing.Icon;
import javax.swing.JPanel;
import org.gephi.data.attributes.api.AttributeController;
import org.gephi.data.attributes.api.AttributeOrigin;
import org.gephi.data.attributes.api.AttributeTable;
import org.gephi.data.attributes.api.AttributeType;
import org.gephi.data.attributes.type.FloatList;
import org.gephi.filters.api.FilterLibrary;
import org.gephi.filters.spi.Category;
import org.gephi.filters.spi.ComplexFilter;
import org.gephi.filters.spi.Filter;
import org.gephi.filters.spi.FilterBuilder;
import org.gephi.filters.spi.FilterProperty;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.Graph;
import org.gephi.graph.api.Node;
import org.gephi.plugins.forceAtlas2.ForceAtlas2LayoutData;
import org.gephi.plugins.forceAtlas2.ForceFactory;
import org.gephi.plugins.forceAtlas2.NodesThread;
import org.gephi.plugins.forceAtlas2.Region;
import org.gephi.timeline.api.TimelineController;
import org.gephi.timeline.api.TimelineModelEvent;
import org.gephi.timeline.api.TimelineModelListener;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = FilterBuilder.class)
public class ForceAtlasFilterBuilder implements FilterBuilder {

    @Override
    public Category getCategory() {
        return FilterLibrary.TOPOLOGY;
    }

    @Override
    public String getName() {
        return "ForceAtlas2";
    }

    @Override
    public Icon getIcon() {
        return null;
    }

    @Override
    public String getDescription() {
        return "";
    }

    @Override
    public Filter getFilter() {
        return new ForceAtlasFilter();
    }

    @Override
    public JPanel getPanel(Filter filter) {
        ForceAtlasFilterUI ui = Lookup.getDefault().lookup(ForceAtlasFilterUI.class);
        if (ui != null) {
            return ui.getPanel((ForceAtlasFilter) filter);
        }
        return null;
    }

    @Override
    public void destroy(Filter filter) {
        ((ForceAtlasFilter)filter).destroy();
    }

    public static class ForceAtlasFilter implements ComplexFilter {

        private final ForceAtlasAlgorithm algo;

        public ForceAtlasFilter() {
            algo = new ForceAtlasAlgorithm();
        }

        @Override
        public Graph filter(Graph graph) {
            algo.setGraph(graph);

            long i = 0;
            algo.initAlgo();
            while (true) {
                algo.goAlgo();
                i++;
                if (algo.getNumIterations() == i) {
                    break;
                }
            }
            algo.endAlgo();

            return graph;
        }

        @Override
        public String getName() {
            return "ForceAtlas2";
        }

        @Override
        public FilterProperty[] getProperties() {
            return null;
        }

        private void destroy() {
            algo.destroy();
        }
    }

    public static class ForceAtlasAlgorithm {

        private Graph graph;
        private double edgeWeightInfluence;
        private double jitterTolerance;
        private double scalingRatio;
        private double gravity;
        private double speed;
        private boolean outboundAttractionDistribution;
        private boolean adjustSizes;
        private boolean barnesHutOptimize;
        private double barnesHutTheta;
        private boolean linLogMode;
        private boolean strongGravityMode;
        private int threadCount;
        private int numIterations;
        private int currentThreadCount;
        private Region rootRegion;
        private ExecutorService pool;
        private final TimelineModelListener timelineModelListener;
        private final TimelineController timelineController;
        double outboundAttCompensation = 1;

        public ForceAtlasAlgorithm() {
            this.threadCount = Math.min(4, Math.max(1, Runtime.getRuntime().availableProcessors() - 1));

            timelineController = Lookup.getDefault().lookup(TimelineController.class);
            AttributeController attributeController = Lookup.getDefault().lookup(AttributeController.class);

            AttributeTable nodesTable = attributeController.getModel().getNodeTable();
            if (nodesTable.hasColumn("Direction") == false) {
                FloatList def = new FloatList(new Float[]{0f, 0f});
                nodesTable.addColumn("Direction", "Direction", AttributeType.LIST_FLOAT, AttributeOrigin.COMPUTED, def);
            }

            timelineModelListener = new TimelineModelListener() {
                @Override
                public void timelineModelChanged(TimelineModelEvent tme) {
                    if (tme.getEventType() == TimelineModelEvent.EventType.PLAY_START) {
                        System.err.println("PLAY_START");
                        for (Node n : graph.getNodes()) {
                            n.getAttributes().setValue("Direction", new FloatList(new Float[]{-n.getNodeData().x(), -n.getNodeData().y()}));
                        }
                    }
                }
            };

            timelineController.addListener(timelineModelListener);
        }
        
        public void destroy() {
            timelineController.removeListener(timelineModelListener);
        }

        public void setGraph(Graph graph) {
            this.graph = graph;
            resetPropertiesValues();
        }

        public void initAlgo() {
            speed = 1.;

            if (graph == null) {
                throw new IllegalArgumentException("graph variable not set");
            }

            graph.readLock();
            Node[] nodes = graph.getNodes().toArray();

            // Initialise layout data
            for (Node n : nodes) {
                if (n.getNodeData().getLayoutData() == null || !(n.getNodeData().getLayoutData() instanceof ForceAtlas2LayoutData)) {
                    ForceAtlas2LayoutData nLayout = new ForceAtlas2LayoutData();
                    n.getNodeData().setLayoutData(nLayout);
                }
                ForceAtlas2LayoutData nLayout = n.getNodeData().getLayoutData();
                nLayout.mass = 1 + graph.getDegree(n);
                nLayout.old_dx = 0;
                nLayout.old_dy = 0;
                nLayout.dx = 0;
                nLayout.dy = 0;
            }

            pool = Executors.newFixedThreadPool(threadCount);
            currentThreadCount = threadCount;
        }

        public void goAlgo() {
            graph.readLock();
            Node[] nodes = graph.getNodes().toArray();
            Edge[] edges = graph.getEdges().toArray();

            // Initialise layout data
            for (Node n : nodes) {
                if (n.getNodeData().getLayoutData() == null || !(n.getNodeData().getLayoutData() instanceof ForceAtlas2LayoutData)) {
                    ForceAtlas2LayoutData nLayout = new ForceAtlas2LayoutData();
                    n.getNodeData().setLayoutData(nLayout);
                }
                ForceAtlas2LayoutData nLayout = n.getNodeData().getLayoutData();
                nLayout.mass = 1 + graph.getDegree(n);
                nLayout.old_dx = nLayout.dx;
                nLayout.old_dy = nLayout.dy;
                nLayout.dx = 0;
                nLayout.dy = 0;
            }

            // If Barnes Hut active, initialize root region
            if (isBarnesHutOptimize()) {
                rootRegion = new Region(nodes);
                rootRegion.buildSubRegions();
            }

            // If outboundAttractionDistribution active, compensate.
            if (isOutboundAttractionDistribution()) {
                outboundAttCompensation = 0;
                for (Node n : nodes) {
                    ForceAtlas2LayoutData nLayout = n.getNodeData().getLayoutData();
                    outboundAttCompensation += nLayout.mass;
                }
                outboundAttCompensation /= nodes.length;
            }

            // Repulsion (and gravity)
            // NB: Muti-threaded
            ForceFactory.RepulsionForce Repulsion = ForceFactory.builder.buildRepulsion(isAdjustSizes(), getScalingRatio());

            int taskCount = 8 * currentThreadCount;  // The threadPool Executor Service will manage the fetching of tasks and threads.
            // We make more tasks than threads because some tasks may need more time to compute.
            ArrayList<Future> threads = new ArrayList();
            for (int t = taskCount; t > 0; t--) {
                int from = (int) Math.floor(nodes.length * (t - 1) / taskCount);
                int to = (int) Math.floor(nodes.length * t / taskCount);
                Future future = pool.submit(new NodesThread(nodes, from, to, isBarnesHutOptimize(), getBarnesHutTheta(), getGravity(), (isStrongGravityMode()) ? (ForceFactory.builder.getStrongGravity(getScalingRatio())) : (Repulsion), getScalingRatio(), rootRegion, Repulsion));
                threads.add(future);
            }
            for (Future future : threads) {
                try {
                    future.get();
                } catch (InterruptedException ex) {
                    Exceptions.printStackTrace(ex);
                } catch (ExecutionException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }

            // Attraction
            ForceFactory.AttractionForce Attraction = ForceFactory.builder.buildAttraction(isLinLogMode(), isOutboundAttractionDistribution(), isAdjustSizes(), 1 * ((isOutboundAttractionDistribution()) ? (outboundAttCompensation) : (1)));
            if (getEdgeWeightInfluence() == 0) {
                for (Edge e : edges) {
                    Attraction.apply(e.getSource(), e.getTarget(), 1);
                }
            } else if (getEdgeWeightInfluence() == 1) {
                for (Edge e : edges) {
                    Attraction.apply(e.getSource(), e.getTarget(), e.getWeight());
                }
            } else {
                for (Edge e : edges) {
                    Attraction.apply(e.getSource(), e.getTarget(), Math.pow(e.getWeight(), getEdgeWeightInfluence()));
                }
            }

            // Auto adjust speed
            double totalSwinging = 0d;  // How much irregular movement
            double totalEffectiveTraction = 0d;  // Hom much useful movement
            for (Node n : nodes) {
                ForceAtlas2LayoutData nLayout = n.getNodeData().getLayoutData();
                if (!n.getNodeData().isFixed()) {
                    double swinging = Math.sqrt(Math.pow(nLayout.old_dx - nLayout.dx, 2) + Math.pow(nLayout.old_dy - nLayout.dy, 2));
                    totalSwinging += nLayout.mass * swinging;   // If the node has a burst change of direction, then it's not converging.
                    totalEffectiveTraction += nLayout.mass * 0.5 * Math.sqrt(Math.pow(nLayout.old_dx + nLayout.dx, 2) + Math.pow(nLayout.old_dy + nLayout.dy, 2));
                }
            }
            // We want that swingingMovement < tolerance * convergenceMovement
            double targetSpeed = getJitterTolerance() * getJitterTolerance() * totalEffectiveTraction / totalSwinging;

            // But the speed shoudn't rise too much too quickly, since it would make the convergence drop dramatically.
            double maxRise = 0.5;   // Max rise: 50%
            speed = speed + Math.min(targetSpeed - speed, maxRise * speed);

            // Apply forces
            if (isAdjustSizes()) {
                // If nodes overlap prevention is active, it's not possible to trust the swinging mesure.
                for (Node n : nodes) {
                    ForceAtlas2LayoutData nLayout = n.getNodeData().getLayoutData();
                    if (!n.getNodeData().isFixed()) {

                        // Adaptive auto-speed: the speed of each node is lowered
                        // when the node swings.
                        double swinging = Math.sqrt((nLayout.old_dx - nLayout.dx) * (nLayout.old_dx - nLayout.dx) + (nLayout.old_dy - nLayout.dy) * (nLayout.old_dy - nLayout.dy));
                        double factor = 0.1 * speed / (1f + speed * Math.sqrt(swinging));

                        double df = Math.sqrt(Math.pow(nLayout.dx, 2) + Math.pow(nLayout.dy, 2));
                        factor = Math.min(factor * df, 10.) / df;

                        double x = n.getNodeData().x() + nLayout.dx * factor;
                        double y = n.getNodeData().y() + nLayout.dy * factor;

                        n.getNodeData().setX((float) x);
                        n.getNodeData().setY((float) y);
                    }
                }
            } else {
                for (Node n : nodes) {
                    ForceAtlas2LayoutData nLayout = n.getNodeData().getLayoutData();
                    if (!n.getNodeData().isFixed()) {

                        // Adaptive auto-speed: the speed of each node is lowered
                        // when the node swings.
                        double swinging = Math.sqrt((nLayout.old_dx - nLayout.dx) * (nLayout.old_dx - nLayout.dx) + (nLayout.old_dy - nLayout.dy) * (nLayout.old_dy - nLayout.dy));
                        //double factor = speed / (1f + Math.sqrt(speed * swinging));
                        double factor = speed / (1f + speed * Math.sqrt(swinging));

                        double x = n.getNodeData().x() + nLayout.dx * factor;
                        double y = n.getNodeData().y() + nLayout.dy * factor;

                        n.getNodeData().setX((float) x);
                        n.getNodeData().setY((float) y);

                    }
                }
            }
            graph.readUnlockAll();
        }

        public void endAlgo() {

            for (Node n : graph.getNodes()) {
                FloatList vals = (FloatList) n.getAttributes().getValue("Direction");
                float x = vals.getItem(0) + n.getNodeData().x();
                float y = vals.getItem(1) + n.getNodeData().y();
                n.getAttributes().setValue("Direction", new FloatList(new Float[]{x, y}));
                n.getNodeData().setLayoutData(null);
            }

            pool.shutdown();
            graph.readUnlockAll();
        }

        public void resetPropertiesValues() {
            int nodesCount = graph.getNodeCount();

            // Tuning
            if (nodesCount >= 100) {
                setScalingRatio(2.0);
            } else {
                setScalingRatio(10.0);
            }
            setStrongGravityMode(false);
            setGravity(1.);

            // Behavior
            setOutboundAttractionDistribution(false);
            setLinLogMode(false);
            setAdjustSizes(false);
            setEdgeWeightInfluence(1.);
            setNumIterations(100);

            // Performance
            if (nodesCount >= 50000) {
                setJitterTolerance(10d);
            } else if (nodesCount >= 5000) {
                setJitterTolerance(1d);
            } else {
                setJitterTolerance(0.1d);
            }
            if (nodesCount >= 1000) {
                setBarnesHutOptimize(true);
            } else {
                setBarnesHutOptimize(false);
            }
            setBarnesHutTheta(1.2);
            setThreadsCount(2);
        }

        public Double getBarnesHutTheta() {
            return barnesHutTheta;
        }

        public void setBarnesHutTheta(Double barnesHutTheta) {
            this.barnesHutTheta = barnesHutTheta;
        }

        public Double getEdgeWeightInfluence() {
            return edgeWeightInfluence;
        }

        public void setEdgeWeightInfluence(Double edgeWeightInfluence) {
            this.edgeWeightInfluence = edgeWeightInfluence;
        }

        public Double getJitterTolerance() {
            return jitterTolerance;
        }

        public void setJitterTolerance(Double jitterTolerance) {
            this.jitterTolerance = jitterTolerance;
        }

        public Boolean isLinLogMode() {
            return linLogMode;
        }

        public void setLinLogMode(Boolean linLogMode) {
            this.linLogMode = linLogMode;
        }

        public Double getScalingRatio() {
            return scalingRatio;
        }

        public void setScalingRatio(Double scalingRatio) {
            this.scalingRatio = scalingRatio;
        }

        public Boolean isStrongGravityMode() {
            return strongGravityMode;
        }

        public void setStrongGravityMode(Boolean strongGravityMode) {
            this.strongGravityMode = strongGravityMode;
        }

        public Double getGravity() {
            return gravity;
        }

        public void setGravity(Double gravity) {
            this.gravity = gravity;
        }

        public Integer getThreadsCount() {
            return threadCount;
        }

        public void setThreadsCount(Integer threadCount) {
            if (threadCount < 1) {
                setThreadsCount(1);
            } else {
                this.threadCount = threadCount;
            }
        }

        public Boolean isOutboundAttractionDistribution() {
            return outboundAttractionDistribution;
        }

        public void setOutboundAttractionDistribution(Boolean outboundAttractionDistribution) {
            this.outboundAttractionDistribution = outboundAttractionDistribution;
        }

        public Boolean isAdjustSizes() {
            return adjustSizes;
        }

        public void setAdjustSizes(Boolean adjustSizes) {
            this.adjustSizes = adjustSizes;
        }

        public Boolean isBarnesHutOptimize() {
            return barnesHutOptimize;
        }

        public void setBarnesHutOptimize(Boolean barnesHutOptimize) {
            this.barnesHutOptimize = barnesHutOptimize;
        }

        public int getNumIterations() {
            return numIterations;
        }

        public void setNumIterations(Integer numIterations) {
            this.numIterations = numIterations;
        }

    }

}
