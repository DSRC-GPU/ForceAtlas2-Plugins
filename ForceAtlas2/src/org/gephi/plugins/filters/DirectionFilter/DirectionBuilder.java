package org.gephi.plugins.filters.DirectionFilter;

import javax.swing.Icon;
import javax.swing.JPanel;
import org.gephi.data.attributes.api.AttributeController;
import org.gephi.data.attributes.api.AttributeTable;
import org.gephi.data.attributes.type.FloatList;
import org.gephi.filters.api.FilterLibrary;
import org.gephi.filters.spi.Category;
import org.gephi.filters.spi.ComplexFilter;
import org.gephi.filters.spi.Filter;
import org.gephi.filters.spi.FilterBuilder;
import org.gephi.filters.spi.FilterProperty;
import org.gephi.graph.api.Graph;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.Node;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = FilterBuilder.class)
public class DirectionBuilder implements FilterBuilder {

    public Category getCategory() {
        return FilterLibrary.TOPOLOGY;
    }

    @Override
    public String getName() {
        return NbBundle.getMessage(DirectionBuilder.class, "DirectionBuilder.name");
    }

    @Override
    public Icon getIcon() {
        return null;
    }

    @Override
    public String getDescription() {
        return NbBundle.getMessage(DirectionBuilder.class, "DirectionBuilder.desc");
    }

    @Override
    public Filter getFilter() {
        return new DirectionFilter();
    }

    @Override
    public JPanel getPanel(Filter filter) {
        DirectionUI ui = Lookup.getDefault().lookup(DirectionUI.class);
        if (ui != null) {
            return ui.getPanel((DirectionFilter) filter);
        }
        return null;
    }

    @Override
    public void destroy(Filter filter) {
    }

    public static class DirectionFilter implements ComplexFilter {

        private final int axisLen = 500;
        private boolean normalized = false;

        private void CreateAxis(Graph graph) {
            Graph mainGraph = graph.getView().getGraphModel().getGraph();
            GraphModel model = graph.getGraphModel();

            Node origin = mainGraph.getNode("origin");
            if (origin == null) {
                origin = model.factory().newNode("origin");
                origin.getNodeData().setX(0);
                origin.getNodeData().setY(0);
                mainGraph.addNode(origin);
            }

            Node up = mainGraph.getNode("up");
            if (up == null) {
                up = model.factory().newNode("up");
                up.getNodeData().setX(0);
                up.getNodeData().setY(axisLen);
                mainGraph.addNode(up);
            }

            Node left = mainGraph.getNode("left");
            if (left == null) {
                left = model.factory().newNode("left");
                left.getNodeData().setX(-axisLen);
                left.getNodeData().setY(0);
                mainGraph.addNode(left);
            }

            Node right = mainGraph.getNode("right");
            if (right == null) {
                right = model.factory().newNode("right");
                right.getNodeData().setX(axisLen);
                right.getNodeData().setY(0);
                mainGraph.addNode(right);
            }

            Node down = mainGraph.getNode("down");
            if (down == null) {
                down = model.factory().newNode("down");
                down.getNodeData().setX(0);
                down.getNodeData().setY(-axisLen);
                mainGraph.addNode(down);
            }

            if (mainGraph.getEdge(origin, down) == null) {
                mainGraph.addEdge(model.factory().newEdge(origin, down, 5.0f, false));
            }

            if (mainGraph.getEdge(origin, up) == null) {
                mainGraph.addEdge(model.factory().newEdge(origin, up, 5.0f, false));
            }

            if (mainGraph.getEdge(origin, right) == null) {
                mainGraph.addEdge(model.factory().newEdge(origin, right, 5.0f, false));
            }

            if (mainGraph.getEdge(origin, left) == null) {
                mainGraph.addEdge(model.factory().newEdge(origin, left, 5.0f, false));
            }
        }

        private void scaleDirection(Graph graph) {
            float maxX, maxY;
            float minX, minY;

            minX = minY = Float.MAX_VALUE;
            maxX = maxY = Float.MIN_VALUE;

            for (Node n : graph.getNodes()) {
                FloatList vals = (FloatList) n.getAttributes().getValue("Direction");
                if (vals == null) {
                    continue;
                }

                float x = vals.getItem(0);
                float y = vals.getItem(1);

                if (x > maxX) {
                    maxX = x;
                } else if (x < minX) {
                    minX = x;
                }

                if (y > maxY) {
                    maxY = y;
                } else if (y < minY) {
                    minY = y;
                }
            }

            for (Node n : graph.getNodes()) {
                FloatList vals = (FloatList) n.getAttributes().getValue("Direction");
                if (vals == null) {
                    continue;
                }
                float x = (axisLen * (vals.getItem(0) - minX)) / (maxX - minX);
                float y = (axisLen * (vals.getItem(1) - minY)) / (maxY - minY);
                n.getNodeData().setX(x);
                n.getNodeData().setY(y);
            }

        }

        @Override
        public Graph filter(Graph graph) {
            AttributeController atributeController = Lookup.getDefault().lookup(AttributeController.class);
            AttributeTable nodesTable = atributeController.getModel().getNodeTable();
            if (nodesTable.hasColumn("Direction") == false) {
                return graph;
            }
            
            graph.clearEdges();

            
            if (!normalized) {
                scaleDirection(graph);
            } else {
                normalizeDirection(graph);
            }

            return graph;
        }

        @Override
        public String getName() {
            return NbBundle.getMessage(DirectionBuilder.class, "DirectionBuilder.name");
        }

        @Override
        public FilterProperty[] getProperties() {
            try {
                return new FilterProperty[]{
                    FilterProperty.createProperty(this, Boolean.class, "normalized")
                };
            } catch (NoSuchMethodException ex) {
                Exceptions.printStackTrace(ex);
            }
            return new FilterProperty[0];
        }

        public void setNormalized(boolean normalized) {
            this.normalized = normalized;
        }

        public boolean getNormalized() {
            return normalized;
        }

        private void normalizeDirection(Graph graph) {
            for (Node n : graph.getNodes()) {
                FloatList vals = (FloatList) n.getAttributes().getValue("Direction");
                if (vals == null) {
                    continue;
                }

                float x = vals.getItem(0);
                float y = vals.getItem(1);

                double mag = Math.sqrt(x * x + y * y);
                if (mag != 0) {
                    x /= mag;
                    y /= mag;
                }

                n.getNodeData().setX(x * axisLen);
                n.getNodeData().setY(y * axisLen);
            }
        }

    }
}
