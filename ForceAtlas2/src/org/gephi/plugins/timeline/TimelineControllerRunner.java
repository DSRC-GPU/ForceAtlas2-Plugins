/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.gephi.plugins.timeline;

import org.gephi.timeline.api.TimelineController;

public interface TimelineControllerRunner extends TimelineController {
   
    public void addRunner(TimelineModelRunner r);   
    
    public void removeRunner(TimelineModelRunner r);
}
