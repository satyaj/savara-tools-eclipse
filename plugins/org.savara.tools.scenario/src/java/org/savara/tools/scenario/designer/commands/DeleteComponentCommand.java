/*
 * Copyright 2005-7 Pi4 Technologies Ltd
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
 * Feb 20, 2007 : Initial version created by gary
 */
package org.savara.tools.scenario.designer.commands;

import org.savara.scenario.model.*;
import org.savara.tools.scenario.designer.model.*;
import org.eclipse.gef.commands.Command;

/**
 * This class implements the activity deletion command.
 */
public class DeleteComponentCommand
			extends org.eclipse.gef.commands.Command {
	
	public DeleteComponentCommand() {
	}
	
	public boolean canExecute() {
		boolean ret=false;
		
		if (m_parent != null && m_child != null) {
			ret = true;
		}
		
		return(ret);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.gef.commands.Command#execute()
	 */
	public void execute() {
		
		if (m_child instanceof MessageEvent) {
			
			//ModelSupport.getSourceConnections(scenario, m_child);
			/* TODO: GPB: need links associated with an event
			
			java.util.List list=((MessageEvent)m_child).getSourceMessageLinks();
			
			for (int i=list.size()-1; i >= 0; i--) {
				Link link=(Link)list.get(i);
				
				link.setSource(null);
				link.setTarget(null);
				
				((MessageEvent)m_child).getScenario().getLinks().remove(link);
			}
			
			list=((MessageEvent)m_child).getTargetMessageLinks();
			
			for (int i=list.size()-1; i >= 0; i--) {
				Link link=(Link)list.get(i);
				
				link.setSource(null);
				link.setTarget(null);
				
				((MessageEvent)m_child).getScenario().getLinks().remove(link);
			}
			*/
		} else if (m_child instanceof Role) {
			// Construct deletion commands for each nessage event
			// related to the participant
			
			/* TODO: GPB: need scenario and visitor mechanism

			((Role)m_child).getScenario().visit(new DefaultScenarioVisitor() {

				public void messageEvent(MessageEvent message) {
					
					if (message.getRole() == m_child) {
						DeleteComponentCommand command=
							new DeleteComponentCommand();
						
						command.setChild(message);
						command.setParent(message.eContainer());
						
						command.setIndex(ModelSupport.getChildIndex(
								message.eContainer(), message));
						
						m_propagatedCommands.add(command);
					}
				}
			});
			*/
		}
		
		for (int i=0; i < m_propagatedCommands.size(); i++) {
			Command command=(Command)m_propagatedCommands.get(i);
			
			command.execute();
		}

		ModelSupport.removeChild(m_parent, m_child);
		
		if (m_child instanceof Group) {
			Scenario scenario=null;
			
			if (m_parent instanceof Scenario) {
				scenario = (Scenario)m_parent;
			} else if (m_parent instanceof Event) {
				/* TODO: GPB: need scenario
				scenario = ((Event)m_parent).getScenario();
				*/
			}
			
			// Scan list of message links to see if any no longer have
			// a message event that is attached to the scenario - and
			// then save these in case of an undo
			for (int i=scenario.getLink().size()-1;
						i >= 0; i--) {
				Link link=(Link)scenario.getLink().get(i);
				
				/* TODO: GPB: need scenario
				if ((link.getSource() != null &&
						link.getSource().getScenario() == null) ||
					(link.getTarget() != null &&
						link.getTarget().getScenario() == null)) {
					
					// Remove link
					scenario.getLinks().remove(link);
					
					m_removedMessageLinks.add(0, link);
				}
				*/
			}
		}
	}
	
	public Object getParent() {
		return(m_parent);
	}
	
	public void redo() {
		execute();
	}
	
	public void setChild(Object newNode) {
		m_child = newNode;
		
		// Determine connected children
		if (newNode instanceof MessageEvent) {
			/* TODO: GPB: need source/target message links
			java.util.List list=((MessageEvent)newNode).getSourceMessageLinks();
			
			for (int i=0; i < list.size(); i++) {
				Link link=(Link)list.get(i);
				
				m_targetConnectedEvents.add(link.getTarget());
			}
			
			list=((MessageEvent)newNode).getTargetMessageLinks();
			
			for (int i=0; i < list.size(); i++) {
				Link link=(Link)list.get(i);
				
				m_sourceConnectedEvents.add(link.getSource());
			}
			*/
		}
	}
	
	public void setIndex(int index) {
		m_index = index;
	}
	
	public void setParent(Object newParent) {
		m_parent = newParent;
	}
	
	public void undo() {
		
		ModelSupport.addChild(m_parent, m_child, m_index);
		
		for (int i=m_propagatedCommands.size()-1; i >= 0; i--) {
			Command command=(Command)m_propagatedCommands.get(i);
			
			command.undo();
		}
		
		m_propagatedCommands.clear();
		
		if (m_child instanceof MessageEvent) {
			/* TODO: GPB: need scenario
			for (int i=0; i < m_sourceConnectedEvents.size(); i++) {
				Link link=new Link();
				
				link.setSource((MessageEvent)m_sourceConnectedEvents.get(i));
				link.setTarget((MessageEvent)m_child);
				
				((MessageEvent)m_child).getScenario().getLinks().add(link);
			}
			
			for (int i=0; i < m_targetConnectedEvents.size(); i++) {
				Link link=new Link();
				
				link.setSource((MessageEvent)m_child);
				link.setTarget((MessageEvent)m_targetConnectedEvents.get(i));
				
				((MessageEvent)m_child).getScenario().getLinks().add(link);
			}
			*/
		} else if (m_child instanceof Group) {
			
			for (int i=0; i < m_removedMessageLinks.size(); i++) {
				/* TODO: GPB: need scenario
				((Group)m_child).getScenario().getMessageLinks().add(
						m_removedMessageLinks.get(i));
				*/
			}
			
			m_removedMessageLinks.clear();
		}
	}

	private Object m_child=null;
	private Object m_parent=null;
	private int m_index = -1;
	private java.util.Vector<MessageEvent> m_sourceConnectedEvents=new java.util.Vector<MessageEvent>();
	private java.util.Vector<MessageEvent> m_targetConnectedEvents=new java.util.Vector<MessageEvent>();
	private java.util.Vector<Link> m_removedMessageLinks=new java.util.Vector<Link>();
	private java.util.Vector m_propagatedCommands=new java.util.Vector();
}
