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

package org.jezve.app;

import org.jezve.util.*;

import java.util.jar.*;
import java.util.zip.*;
import java.io.*;
import java.awt.*;

public class Resources {

    /* On Mac OS X 10.5 non priviliged user may (and will) download application
       on Desktop or Downloads folder, start it and, while it is running,
       move it to /Applications folder. Unless jar file is kept open this
       move will proceed and next call to any ClassLoader.getResourceAsStream
       will fail. Thus, deliberatly keep jar file open.
    */
    private static JarFile jar;

    private static JarFile getJarFile(String u) throws IOException {
        if (jar == null) {
            int sep = u.lastIndexOf('!');
            String j = u.substring(0, sep);
            if (j.startsWith("jar:file:")) {
                j = j.substring("jar:file:".length());
            }
            if (j.startsWith("file:")) {
                j = j.substring("file:".length());
            }
            jar = new JarFile(j);
        }
        return jar;
    }

    public static InputStream getResourceAsStream(String location) {
        assert EventQueue.isDispatchThread();
        try {
            java.net.URL url = Resources.class.getResource(location);
            assert url != null : location;
            String u = url.getFile().replaceAll("%20", " ");
            InputStream s;
            // see: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4730642
            if (u.toLowerCase().indexOf(".jar!/") >= 0) {
                JarFile jar = getJarFile(u);
                ZipEntry ze = jar.getEntry(u.substring(u.lastIndexOf('!') + 2));
                s = jar.getInputStream(ze);
            } else {
                s = Resources.class.getResourceAsStream(location);
            }
            assert s != null : location;
            return s;
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    public static byte[] readBytes(String location) {
        InputStream s = getResourceAsStream(location);
        try {
            s = getResourceAsStream(location);
            assert s != null : location;
            return IO.readFully(s);
        } catch (IOException e) {
            throw new Error(e);
        } finally {
            IO.close(s);
        }
    }


}
