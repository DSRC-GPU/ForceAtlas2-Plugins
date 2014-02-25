/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.gephi.plugins.forceAtlas2;

import javax.swing.Icon;
import javax.swing.JPanel;
import org.gephi.layout.spi.Layout;
import org.gephi.layout.spi.LayoutBuilder;
import org.gephi.layout.spi.LayoutUI;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author vlad
 */
@ServiceProvider(service = LayoutBuilder.class)
public class ForceAtlas2_EdgeWeightBuilder implements LayoutBuilder{
    private ForceAtlas2UI ui = new ForceAtlas2UI();

    @Override
    public String getName() {
        return NbBundle.getMessage(ForceAtlas2_1D.class, "ForceAtlas2_EdgeWeight.name");
    }

    @Override
    public LayoutUI getUI() {
        return ui;
    }

    @Override
    public ForceAtlas2_EdgeWeight buildLayout() {
        ForceAtlas2_EdgeWeight layout = new ForceAtlas2_EdgeWeight(this);
        return layout;
    }

    private class ForceAtlas2UI implements LayoutUI {

        @Override
        public String getDescription() {
            return NbBundle.getMessage(ForceAtlas2_1D.class, "ForceAtlas2.description");
        }

        @Override
        public Icon getIcon() {
            return null;
        }

        @Override
        public JPanel getSimplePanel(Layout layout) {
            return null;
        }

        @Override
        public int getQualityRank() {
            return 4;
        }

        @Override
        public int getSpeedRank() {
            return 4;
        }
    }
}
