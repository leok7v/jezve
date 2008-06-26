/**
* Copyright (c) 2007-2008, jezve.org and its Contributors
* All rights reserved.
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions are met:
*     * Redistributions of source code must retain the above copyright
*       notice, this list of conditions and the following disclaimer.
*     * Redistributions in binary form must reproduce the above copyright
*       notice, this list of conditions and the following disclaimer in the
*       documentation and/or other materials provided with the distribution.
*     * Neither the name of the jezve.org nor the
*       names of its contributors may be used to endorse or promote products
*       derived from this software without specific prior written permission.
*
* THIS SOFTWARE IS PROVIDED BY jezve.org AND SOFTWARE CONTRIBUTORS ``AS IS'' AND ANY
* EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
* WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
* DISCLAIMED. IN NO EVENT SHALL jezve.org or CONTRIBUTORS BE LIABLE FOR ANY
* DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
* (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
* LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
* ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
* (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
* SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package org.jezve.notepad;

import java.awt.*;
import java.io.InputStream;
import java.io.IOException;
import javax.swing.*;

final class Resources {

    public static ImageIcon getImageIcon(String name) {
        return new ImageIcon(getImage(name));
    }

    public static boolean hasImageIcon(String name) {
        return  Resources.class.getResource("resources/" + name + ".png") != null;
    }

    public static ImageIcon getImageIcon(String name, int size) {
        return new ImageIcon(getImage(name, size));
    }

    public static Image getImage(String name, int size) {
        Image image = getImage(name);
        return image.getScaledInstance(size, size, Image.SCALE_AREA_AVERAGING);
    }

    public static Image getImage(String name) {
        return getImage(name, "png");
    }

    public static Image getImage(String name, String ext) {
        String location = "resources/" + name + "." + ext;
        return Toolkit.getDefaultToolkit().createImage(readBytes(location));
    }

    public static String getUrl(String name) {
        String location = "./resources/" + name + ".png";
        java.net.URL url = Resources.class.getResource(location);
        assert  url != null : location;
        return url.toExternalForm();
    }

    public static byte[] getBytes(String name) {
        String location = "./resources/" + name + ".png";
        return readBytes(location);
    }

    public static byte[] readBytes(String location) {
        InputStream s = null;
        try {
            s = Resources.class.getResourceAsStream(location);
            byte[] b = new byte[s.available()];
            int n = s.read(b);
            assert n == b.length;
            return b;
        }
        catch (IOException e) {
            throw new Error(e);
        }
        finally {
            if (s != null) {
                try { s.close(); } catch (IOException e) { /*ignore*/ }
            }
        }
    }

}
