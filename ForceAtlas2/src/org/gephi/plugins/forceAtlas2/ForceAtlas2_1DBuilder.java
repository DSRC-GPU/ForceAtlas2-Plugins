package org.gephi.plugins.forceAtlas2;

import org.gephi.layout.spi.LayoutBuilder;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;


@ServiceProvider(service = LayoutBuilder.class)
public class ForceAtlas2_1DBuilder extends ForceAtlas2Builder {

    @Override
    public String getName() {
        return NbBundle.getMessage(ForceAtlas2_1D.class, "ForceAtlas2_1D.name");
    }
    
    @Override
    public ForceAtlas2_1D buildLayout() {
        ForceAtlas2_1D layout = new ForceAtlas2_1D(this);
        return layout;
    }
}
