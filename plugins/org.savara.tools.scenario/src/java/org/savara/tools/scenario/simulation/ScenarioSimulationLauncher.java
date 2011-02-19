/*
 * Copyright 2004-5 Enigmatec Corporation Ltd
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
 * Feb 17, 2005 : Initial version created by gary
 */
package org.savara.tools.scenario.simulation;

import java.io.File;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.IStreamListener;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;
import org.eclipse.jdt.launching.ExecutionArguments;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.VMRunnerConfiguration;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.savara.scenario.simulation.ScenarioSimulator;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Path;

/**
 * This class is responsible for launching a scenario test against
 * a test scenario.
 */
public class ScenarioSimulationLauncher
			extends AbstractJavaLaunchConfigurationDelegate {

	/**
	 * This is the default constructor.
	 *
	 */
	public ScenarioSimulationLauncher() {
	}
	
	/**
	 * This method launches the scenario test.
	 * 
	 * @param configuration The launch configuration
	 * @param mode The mode (run or debug)
	 * @param launch The launch object
	 * @param monitor The optional progress monitor
	 */
	public void launch(ILaunchConfiguration configuration,
            String mode, ILaunch launch, IProgressMonitor monitor)
						throws CoreException {
		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}
		
		monitor.beginTask(MessageFormat.format("{0}...", new String[]{configuration.getName()}), 3); //$NON-NLS-1$
		// check for cancellation
		if (monitor.isCanceled()) {
			return;
		}
		
		monitor.subTask("Verifying launch configuration....");
						
		String mainTypeName = ScenarioSimulator.class.getName(); 

		IVMInstall vm = verifyVMInstall(configuration);

		IVMRunner runner = vm.getVMRunner(mode);
		if (runner == null) {
			abort("VM runner does not exist",
					null, IJavaLaunchConfigurationConstants.ERR_VM_RUNNER_DOES_NOT_EXIST); //$NON-NLS-1$
		}

		File workingDir = verifyWorkingDirectory(configuration);
		String workingDirName = null;
		if (workingDir != null) {
			workingDirName = workingDir.getAbsolutePath();
		}
		
		// Environment variables
		String[] envp= DebugPlugin.getDefault().getLaunchManager().getEnvironment(configuration);
		
		// Program & VM args
		String filename=configuration.getAttribute(
				ScenarioSimulationLaunchConfigurationConstants.ATTR_PROJECT_NAME, "")+
				"/"+configuration.getAttribute(
				ScenarioSimulationLaunchConfigurationConstants.ATTR_SCENARIO, "");
		
		String execServices=configuration.getAttribute(
				ScenarioSimulationLaunchConfigurationConstants.ATTR_EXECUTE_SERVICES,
					"");
		
		if (execServices.length() != 0) {
			execServices = " \""+execServices+"\"";
		}
		
		String pgmArgs="\""+getPathForScenario(filename)+
						"\""+execServices;
		
		logger.fine("Launching scenario test with args: "+pgmArgs);
			
		String vmArgs = getVMArguments(configuration);
		ExecutionArguments execArgs = new ExecutionArguments(vmArgs, pgmArgs);
		
		// VM-specific attributes
		Map vmAttributesMap = getVMSpecificAttributesMap(configuration);
		
		// Classpath
		String[] classpath = getClasspath(configuration);
		
		// Create VM config
		VMRunnerConfiguration runConfig = new VMRunnerConfiguration(mainTypeName, classpath);
		runConfig.setProgramArguments(execArgs.getProgramArgumentsArray());
		runConfig.setEnvironment(envp);
		runConfig.setVMArguments(execArgs.getVMArgumentsArray());
		runConfig.setWorkingDirectory(workingDirName);
		runConfig.setVMSpecificAttributesMap(vmAttributesMap);

		// Bootpath
		runConfig.setBootClassPath(getBootpath(configuration));
				
		// check for cancellation
		if (monitor.isCanceled()) {
			return;
		}		
		
		// stop in main
		prepareStopInMain(configuration);
		
		// done the verification phase
		monitor.worked(1);
		
		// Launch the configuration - 1 unit of work
		runner.run(runConfig, launch, monitor);
		
		IProcess[] processes=launch.getProcesses();
		if (processes.length > 0) {
			processes[0].getStreamsProxy().getOutputStreamMonitor().
						addListener(new IStreamListener() {
				public void streamAppended(String str, IStreamMonitor mon) {
					handleResults(str, false);
				}
			});
			processes[0].getStreamsProxy().getErrorStreamMonitor().
						addListener(new IStreamListener() {
				public void streamAppended(String str, IStreamMonitor mon) {
					handleResults(str, true);
				}
			});
		}
		
		// check for cancellation
		if (monitor.isCanceled()) {
			return;
		}	
		
		monitor.done();
	}
	
	/**
	 * This method handles the results produced by the launched
	 * test.
	 * 
	 * @param results The results
	 * @param errorStream Whether the results are from the error
	 * 						stream
	 */
	protected void handleResults(String results, boolean errorStream) {
		System.out.println(results);
	}
	
	/**
	 * This method returns the full path to the scenario.
	 * 
	 * @param relativePath The is the scenario path begining at
	 * 					the project
	 * @return The full path
	 */
	protected String getPathForScenario(String relativePath) {
		String ret=null;
		
		IFile file=ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(relativePath));
		if (file != null && file.exists()) {
			ret = file.getLocation().toString();
		}
		
		return(ret);
	}
	
	/**
	 * This method derives the classpath required to run the 
	 * ScenarioTester utility.
	 * 
	 * @param configuration The launch configuation
	 * @return The list of classpath entries
	 */
	public String[] getClasspath(ILaunchConfiguration configuration) {
		String[] ret=null;
		java.util.Vector<String> classpathEntries=new java.util.Vector<String>();
					
		// Add classpath entry for current Java project
		try {
			String projname=configuration.getAttribute(
				ScenarioSimulationLaunchConfigurationConstants.ATTR_PROJECT_NAME, "");
		
			IProject project=
				ResourcesPlugin.getWorkspace().getRoot().getProject(projname);

			IJavaProject jproject=JavaCore.create(project); 
			
			// Add output location
			IPath outputLocation=jproject.getOutputLocation();
			
			IFolder folder=
				ResourcesPlugin.getWorkspace().getRoot().getFolder(outputLocation);
			
			String path=folder.getLocation().toString();

			classpathEntries.add(path);
			
			// Add other libraries to the classpath
			IClasspathEntry[] curclspath=jproject.getRawClasspath();
			for (int i=0; curclspath != null &&
						i < curclspath.length; i++) {
				
				if (curclspath[i].getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
					IFile file=
						ResourcesPlugin.getWorkspace().
							getRoot().getFile(curclspath[i].getPath());

					if (file.exists()) {
						// Library is within the workspace
						classpathEntries.add(file.getLocation().toString());
					} else {
						// Assume library is external to workspace
						classpathEntries.add(curclspath[i].getPath().toString());
					}
					
				} else if (curclspath[i].getEntryKind() ==
								IClasspathEntry.CPE_CONTAINER) {
					// Container's not currently handled - but
					// problem need to retrieve from project and
					// iterate over container entries
				}
			}
			
		} catch(Exception e) {
			// TODO: report error
		}
		
		buildClassPath("org.pi4soa.scenario", classpathEntries);
		buildClassPath("org.pi4soa.service", classpathEntries);
		buildClassPath("org.pi4soa.common", classpathEntries);
		buildClassPath("org.pi4soa.cdl", classpathEntries);
		buildClassPath("org.eclipse.emf.ecore", classpathEntries);
		buildClassPath("org.eclipse.emf.ecore.xmi", classpathEntries);
		buildClassPath("org.eclipse.emf.common", classpathEntries);
		buildClassPath("org.apache.xalan", classpathEntries);
		buildClassPath("org.apache.xml.serializer", classpathEntries);
		
		ret = new String[classpathEntries.size()];
		classpathEntries.copyInto(ret);
		
		if (logger.isLoggable(Level.FINEST)) {
			logger.finest("Scenario Simulation Classpath:");
			for (int i=0; i < ret.length; i++) {
				logger.finest("    ["+i+"] "+ret[i]);
			}
		}
		
		return(ret);
	}
	
	protected void buildClassPath(String bundleId, java.util.List<String> entries) {
		Bundle bundle= Platform.getBundle(bundleId);
		if (bundle != null) {
			java.net.URL installLocation= bundle.getEntry("/");
			java.net.URL local= null;
			try {
				local= Platform.asLocalURL(installLocation);
			} catch (java.io.IOException e) {
				e.printStackTrace();
			}
			
			String baseLocation = local.getFile();

			try {
				String requires = (String)bundle.getHeaders().get(Constants.BUNDLE_CLASSPATH);
				ManifestElement[] elements = ManifestElement.parseHeader(Constants.BUNDLE_CLASSPATH, requires);
				
				for (int i=0; elements != null && i < elements.length; i++) {
					
					String path=baseLocation+elements[i].getValue();
					
					// Check if path is for a Jar and that the
					// file exists - if not see if a classes
					// directory exists
					if (path.endsWith(".jar")) {
						
						if ((new File(path)).exists() == false) {
							if ((new File(baseLocation+"classes")).exists()) {
								path = baseLocation+"classes";
							}
						}
					}
					
					if (entries.contains(path) == false) {
						if (logger.isLoggable(Level.FINE)) {
							logger.fine("Adding classpath entry '"+
									path+"'");
						}
						entries.add(path);
						
						if (elements[i].getValue().equals(".")) {
							if ((new File(baseLocation+"classes")).exists()) {
								path = baseLocation+"classes";
								
								entries.add(path);
							}
						}
					}
				}
				
				if (elements == null) {
					if (logger.isLoggable(Level.FINE)) {
						logger.fine("Adding classpath entry '"+
								baseLocation+"'");
					}
					entries.add(baseLocation);
				}
				
				/*
				requires = (String)bundle.getHeaders().get(Constants.REQUIRE_BUNDLE);
			    elements = ManifestElement.parseHeader(Constants.REQUIRE_BUNDLE, requires);

				for (int i=0; recursive &&
						elements != null && i < elements.length; i++) {
					buildClasspathEntries(elements[i].getValue(),
							entries, false);
				}
				*/
				
			} catch(Exception e) {
				logger.severe("Failed to construct classpath: "+e);
				e.printStackTrace();
			}
		}
	}
	
	private static Logger logger = Logger.getLogger("org.pi4soa.service.test.eclipse");
}