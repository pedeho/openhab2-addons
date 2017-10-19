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
public class IHCServerIOConfiguration {

    public int module;
    public int input;
    public int output;
    public boolean button;
    public int stateModule;
    public int stateOutput;

    @Override
    public String toString() {
        return "IHCServerIOConfiguration "
                + "module=" + this.module
                + ", input=" + this.input
                + ", output=" + this.output
                + ", button=" + this.button
                + ", stateModule=" + this.stateModule
                + ", stateOutput=" + this.stateOutput
                + "]";
    }
}
