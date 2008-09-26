/*
 * Copyright 2006 Niclas Hedhman.
 *
 * Licensed  under the  Apache License,  Version 2.0  (the "License");
 * you may not use  this file  except in  compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed  under the  License is distributed on an "AS IS" BASIS,
 * WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */
package org.ops4j.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * TODO add unit tests
 * TODO add Javadoc
 */
public class Pipe
    implements Runnable
{

    private final InputStream m_in;
    private final OutputStream m_out;
    private Object m_processStream;

    private volatile Thread m_thread;

    public Pipe( InputStream processStream, OutputStream systemStream )
    {
        m_in = processStream;
        m_out = systemStream;
        m_processStream = m_in;
    }

    public Pipe( OutputStream processStream, InputStream systemStream )
    {
        m_in = systemStream;
        m_out = processStream;
        m_processStream = m_out;
    }

    public synchronized Pipe start( final String name )
    {
        if( null == m_processStream || null != m_thread )
        {
            return this;
        }

        if( m_in != m_processStream )
        {
            try
            {
                m_in.available();
            }
            catch( IOException e )
            {
                return this;
            }
        }

        m_thread = new Thread( this, name );
        m_thread.setDaemon( true );
        m_thread.start();
        return this;
    }

    public synchronized void stop()
    {
        if( null == m_processStream || null == m_thread )
        {
            return;
        }

        Thread t = m_thread;
        m_thread = null;

        t.interrupt();
    }

    public void run()
    {
        /*
         * Note: The original code only read characters from the stream one at a
         * time. This corrupted the output by interleaving data from System.out
         * and System.err.  The below reads bytes from in blocks as they are made
         * available by the Platform process.  This groups the data as appropriate
         * to prevent this interleaving.  The bugs related to this are:
         * PAXRUNNER-68: http://issues.ops4j.org/jira/browse/PAXRUNNER-68
         * PAXRUNNER-80: http://issues.ops4j.org/jira/browse/PAXRUNNER-80
         */
        byte[] cbuf = new byte[8192];
        while( Thread.currentThread() == m_thread )
        {
            try
            {
                if( m_in.available() == 0 )
                {
                    // don't block in case this thread is being stopped...
                    try {Thread.sleep(100);} catch (InterruptedException e) {}
                    continue;
                }
                int bytesRead = m_in.read( cbuf, 0, 8192 );
                if( bytesRead == -1 )
                {
                    break;
                }
                m_out.write( cbuf, 0, bytesRead );
                m_out.flush();
            }
            catch( IOException e )
            {
                if( Thread.currentThread() == m_thread )
                {
                    e.printStackTrace();
                }
            }
        }

        try
        {
            if( m_in == m_processStream )
            {
                m_in.close();
            }
        }
        catch( IOException e )
        {
            // ignore
        }
        finally
        {
            m_processStream = null;
        }
    }
}
