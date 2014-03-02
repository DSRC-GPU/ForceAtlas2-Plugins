package org.gephi.plugins.forceAtlas2;

import org.gephi.layout.spi.LayoutBuilder;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;


@ServiceProvider(service = LayoutBuilder.class)
public class ForceAtlas2_EdgeWeightBuilder extends ForceAtlas2Builder {

    @Override
    public String getName() {
        return NbBundle.getMessage(ForceAtlas2_1D.class, "ForceAtlas2_EdgeWeight.name");
    }
    
    @Override
    public ForceAtlas2_EdgeWeight buildLayout() {
        ForceAtlas2_EdgeWeight layout = new ForceAtlas2_EdgeWeight(this);
        return layout;
    }
}