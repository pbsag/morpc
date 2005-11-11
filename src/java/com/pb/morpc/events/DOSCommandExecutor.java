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
 * Created on Jul 26, 2004
 * A call for executing DOD command from Java.
 */

package com.pb.morpc.events;

/**
 * @author Wu Sun Based on Jim Hick's MORPC codes
 *	<sunw@pbworld.com>
 */

import org.apache.log4j.Logger;
import java.util.HashMap;
import com.pb.common.util.ResourceUtil;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class DOSCommandExecutor {
	
    protected static Logger logger = Logger.getLogger(DOSCommandExecutor.class);
	protected static HashMap propertyMap;
	protected static String CMD_LOCATION;
	
	static {
	    propertyMap = ResourceUtil.getResourceBundleAsHashMap("MiamiEvent");	
		CMD_LOCATION = (String) propertyMap.get("CMD_LOCATION");
	}
	
	public static HashMap getPropertyMap(){
		return propertyMap;
	}

    public static void runDOSCommand(String command) {
        try {
            logger.info ("issuing DOS command: " + command);
            sendCommand(command);
        } catch (InterruptedException e) {
            System.out.println("Interrupted exception ocurred for command: " + command);
        }
    }

    public static void sendCommand(String command) throws InterruptedException {
        try {

            String s;
            Process proc = Runtime.getRuntime().exec(CMD_LOCATION + "\\cmd.exe /C " + command);

            BufferedReader stdout = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            BufferedReader stderr = new BufferedReader(new InputStreamReader(proc.getErrorStream()));

            while ((s = stdout.readLine()) != null) {
                logger.warn(s);
            }

            while ((s = stderr.readLine()) != null) {
                logger.warn(s);
            }
        }
        catch (IOException e) {
            System.err.println("exception: " + e);
        }
    }
}
