/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.ihcserver.config;

/**
 * @author Paul Dhaene
 */
public class IHCServerBridgeConfiguration {

    public String url;

    public int tcpPort;

    public String password;

    @Override
    public String toString() {
        return "IHCServerBridgeConfiguration [url=" + url
                + "tcpPort=" + tcpPort
                + "password=" + password
                + "]";
    }
}
