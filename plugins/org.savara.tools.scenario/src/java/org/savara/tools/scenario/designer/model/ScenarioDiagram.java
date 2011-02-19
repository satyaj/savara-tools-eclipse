/*
 * Copyright 2005-6 Pi4 Technologies Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * Change History:
 * 15 Feb 2007 : Initial version created by gary
 */
package org.savara.tools.scenario.designer.model;

import org.eclipse.draw2d.geometry.Point;
import org.savara.tools.scenario.designer.parts.ScenarioBaseEditPart;


public interface ScenarioDiagram {

	public int getHeight();
	
	public void update();
	
	public org.savara.scenario.model.Scenario getScenario();
	
	public ScenarioBaseEditPart findEditPartAtLocation(Point loc,
						Class modelClass);
	
	public org.savara.tools.scenario.designer.simulate.ScenarioSimulation getSimulation();
	
}
