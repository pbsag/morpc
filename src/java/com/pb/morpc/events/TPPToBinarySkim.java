/*
 * Copyright  2005 PB Consult Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
/*
 * Created on Aug 4, 2004
 * Use this class to convert TPP or TRANPLAN skims to binary skims.
 */

package com.pb.morpc.events;

/**
 * @author SunW based Jim Hick's codes
 * <sunw@pbworld.com>
 */

import java.util.Vector;
import java.util.HashMap;

public class TPPToBinarySkim {
	
	protected HashMap propertyMap;
	protected Vector skims;
	protected String tppDir;
	protected String binDir;
	protected String TPP_TO_BINARY_PROGRAM_DIRECTORY;
	protected String TPP_TO_BINARY_PROGRAM;
	
	public TPPToBinarySkim(HashMap propertyMap, String tppDir, String binDir){
		this.propertyMap=propertyMap;	
		this.tppDir=tppDir;
		this.binDir=binDir;
        TPP_TO_BINARY_PROGRAM_DIRECTORY = (String) propertyMap.get("TPP_TO_BINARY_PROGRAM_DIRECTORY");
        TPP_TO_BINARY_PROGRAM = (String) propertyMap.get("TPP_TO_BINARY_PROGRAM");
	}
	
	public void convert(){
        DOSCommandExecutor.runDOSCommand(TPP_TO_BINARY_PROGRAM_DIRECTORY + "\\" + TPP_TO_BINARY_PROGRAM + " " + tppDir + " " + binDir);		
	}
}