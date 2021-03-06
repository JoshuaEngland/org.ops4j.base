/*
 * Copyright 2007 Alin Dreghiciu.
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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.ops4j.lang.NullArgumentException;

/**
 * Implementation of lister that list content of a system file directory.
 *
 * @author Alin Dreghiciu
 * @since September 04, 2007
 */
public class DirectoryLister
    implements Lister
{

    /**
     * The root directory to be listed.
     */
    private final File m_dir;
    /**
     * File path include filters.
     */
    private final Pattern[] m_includes;
    /**
     * File path exclude filters.
     */
    private final Pattern[] m_excludes;

    /**
     * Creates a new directory lister.
     *
     * @param dir    the base directory from where the files should be listed
     * @param filter filter to be used to filter entries from the directory
     */
    public DirectoryLister( final File dir, final Pattern filter )
    {
        NullArgumentException.validateNotNull( dir, "Directory" );
        NullArgumentException.validateNotNull( filter, "Filter" );

        m_dir = dir;
        m_includes = new Pattern[]{ filter };
        m_excludes = new Pattern[0];
    }

    /**
     * Creates a new directory lister.
     *
     * @param dir      the base directory from where the files should be listed
     * @param includes filters to be used to include entries from the directory
     * @param excludes filters to be used to exclude entries from the directory
     */
    public DirectoryLister( final File dir,
                            final Pattern[] includes,
                            final Pattern[] excludes )
    {
        NullArgumentException.validateNotNull( dir, "Directory" );
        NullArgumentException.validateNotNull( includes, "Include filters" );
        NullArgumentException.validateNotNull( includes, "Exclude filters" );

        m_dir = dir;
        m_includes = includes;
        m_excludes = excludes;
    }

    /**
     * {@inheritDoc}
     */
    public List<URL> list()
        throws MalformedURLException
    {
        final List<URL> content = new ArrayList<URL>();
        // first we get all files
        final List<String> fileNames = listFiles( m_dir, "" );
        // then we filter them based on configured filter
        for( String fileName : fileNames )
        {
            if( matchesIncludes( fileName ) && !matchesExcludes( fileName ) )
            {
                File fileToAdd = new File( m_dir, fileName );
                if( !fileToAdd.isHidden() && !fileName.startsWith( "." ) )
                {
                    content.add( fileToAdd.toURI().toURL() );
                }
            }
        }
        return content;
    }

    /**
     * Checks if the file name matches inclusion patterns.
     *
     * @param fileName file name to be matched
     *
     * @return true if matches, false otherwise
     */
    private boolean matchesIncludes( final String fileName )
    {
        if( m_includes.length == 0 )
        {
            return true;
        }
        for( Pattern include : m_includes )
        {
            if( include.matcher( fileName ).matches() )
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the file name matches exclusion patterns.
     *
     * @param fileName file name to be matched
     *
     * @return true if matches, false otherwise
     */
    private boolean matchesExcludes( final String fileName )
    {
        for( Pattern include : m_excludes )
        {
            if( include.matcher( fileName ).matches() )
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Lists recursively files form a directory
     *
     * @param dir        the directory to list
     * @param parentName name of the parent; to be used in construction the relative path of the returned files
     *
     * @return a list foi files from the dir and any sub-folders
     */
    private List<String> listFiles( final File dir, final String parentName )
    {
        final List<String> fileNames = new ArrayList<String>();
        File[] files = null;
        if( dir.canRead() )
        {
            files = dir.listFiles();
        }
        if( files != null )
        {
            for( File file : files )
            {
                if( file.isDirectory() )
                {
                    fileNames.addAll( listFiles( file, parentName + file.getName() + "/" ) );
                }
                else
                {
                    fileNames.add( parentName + file.getName() );
                }
            }
        }
        return fileNames;
    }

}
