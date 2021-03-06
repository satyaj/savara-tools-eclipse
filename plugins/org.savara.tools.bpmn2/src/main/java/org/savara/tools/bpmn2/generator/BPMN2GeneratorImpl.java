/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008, Red Hat Middleware LLC, and others contributors as indicated
 * by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.savara.tools.bpmn2.generator;

import java.io.ByteArrayOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.savara.bpmn2.generation.process.ProtocolToBPMN2ProcessModelGenerator;
import org.savara.bpmn2.model.TDefinitions;
import org.savara.bpmn2.model.util.BPMN2ModelUtil;
import org.savara.common.logging.FeedbackHandler;
import org.savara.common.logging.MessageFormatter;
import org.savara.tools.bpmn2.properties.BPMN2Properties;
import org.savara.tools.common.ArtifactType;
import org.savara.tools.common.generation.AbstractGenerator;
import org.scribble.protocol.model.*;
import org.eclipse.core.runtime.*;

/**
 * This class provides the mechanism for generating BPMN2
 * service artefacts.
 */
public class BPMN2GeneratorImpl extends AbstractGenerator {

	private static final String GENERATOR_NAME = "BPMN2 Process";

	private static Logger logger = Logger.getLogger(BPMN2GeneratorImpl.class.getName());

	/**
	 * This is the constructor for the generator.
	 * 
	 */
	public BPMN2GeneratorImpl() {
		super(GENERATOR_NAME);
	}
	
	/**
	 * This method returns the artifact type that will be generated.
	 * 
	 * @return The artifact type
	 */
	public ArtifactType getArtifactType() {
		return(ArtifactType.ServiceImplementation);
	}
	
	/**
	 * This method generates some artifacts based on the supplied model and
	 * role.
	 * 
	 * If specified, the optional project name will be used to create a
	 * new Eclipse project for the generated artifacts. If no project name
	 * is specified, then the artifacts will be created in the model
	 * resource's project.
	 * 
	 * @param model The protocol model
	 * @param role The role
	 * @param projectName The optional project name
	 * @param modelResource The resource associated with the model
	 * @param handler The feedback handler for reporting issues
	 */
	public void generate(ProtocolModel model, Role role, String projectName,
						IResource modelResource, FeedbackHandler handler) {
		
		if (logger.isLoggable(Level.FINE)) {
			logger.fine("Generate local model '"+role+"' for: "+model);
		}
		
		ProtocolModel local=null;
		
		if (model.isLocated()) {
			if (role == null || role.equals(model.getProtocol().getLocatedRole())) {
				local = model;
			} else {
				handler.error(MessageFormatter.format(java.util.PropertyResourceBundle.getBundle(
						"org.savara.tools.bpmn2.Messages"), "SAVARA-BPMN2TOOLS-00003",
									model.getProtocol().getLocatedRole(), role), null);
			}
		} else {
			local = getProtocolModelForRole(model, role, modelResource, handler);
		}
		
		if (local != null) {
			ProtocolToBPMN2ProcessModelGenerator generator=new ProtocolToBPMN2ProcessModelGenerator();
			
			generator.setMessageBasedInvocation(BPMN2Properties.isGenerateMessageBasedInvocation(modelResource));
			
			java.util.Map<String,Object> target=generator.generate(local, handler, null);
			
			if (target != null && target.size() > 0) {
				try {
					generateRoleProject(model, projectName, role, target,
							local, modelResource, handler);
				} catch(Exception e) {
					logger.log(Level.SEVERE, "Failed to create BPMN2 project '"+projectName+"'", e);
					
					handler.error(MessageFormatter.format(java.util.PropertyResourceBundle.getBundle(
							"org.savara.tools.bpmn2.Messages"), "SAVARA-BPMN2TOOLS-00001",
										projectName), null);
				}
			}
		}
	}
	
	protected void generateRoleProject(ProtocolModel model, String projectName, Role role,
			java.util.Map<String,Object> target, ProtocolModel localcm,
					IResource resource, FeedbackHandler journal) throws Exception {
		
		final IProject proj=createProject(resource, projectName, journal);
		
		if (proj != null) {

			for (String modelName : target.keySet()) {
				Object val=target.get(modelName);
				
				if (val instanceof TDefinitions) {
					TDefinitions process=(TDefinitions)val;
					
					// Store BPMN2 process
					IPath bpmn2Path=proj.getFullPath().append(modelName);
					
					IFile bpmn2File=proj.getProject().getWorkspace().getRoot().getFile(bpmn2Path);
					
					bpmn2File.create(null, true,
							new org.eclipse.core.runtime.NullProgressMonitor());
					
					//String bpelText=XMLUtils.toText(bpelProcess.getDOMElement());
					ByteArrayOutputStream os=new ByteArrayOutputStream();
					BPMN2ModelUtil.serialize(process, os, BPMN2GeneratorImpl.class.getClassLoader());
					
					os.close();
					
					bpmn2File.setContents(new java.io.ByteArrayInputStream(
								os.toByteArray()), true, false,
								new org.eclipse.core.runtime.NullProgressMonitor());
				}
			}
		}
	}
	
	protected IProject createProject(IResource resource, String projectName, FeedbackHandler journal)
											throws Exception {
		// Create project
		IProject project = resource.getWorkspace().getRoot().getProject(projectName);
		project.create(new org.eclipse.core.runtime.NullProgressMonitor());
		
		// Open the project
		project.open(new org.eclipse.core.runtime.NullProgressMonitor());
		
		return(project);
	}
}
