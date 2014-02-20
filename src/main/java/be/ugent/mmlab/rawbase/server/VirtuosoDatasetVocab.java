/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package be.ugent.mmlab.rawbase.server;

import org.apache.jena.fuseki.FusekiException ;
import org.apache.jena.iri.IRI ;
import org.apache.jena.riot.system.IRIResolver ;

import com.hp.hpl.jena.rdf.model.Property ;
import com.hp.hpl.jena.rdf.model.Resource ;
import com.hp.hpl.jena.rdf.model.ResourceFactory ;

public class VirtuosoDatasetVocab
{
    public static String NS = "http://jena.apache.org/fusekivirtuosoExt#" ;

    public static final Resource tDataset = resource("VirtuosoDataset") ;

    public static final Property pjdbcURL = property("jdbcURL") ;
    public static final Property puser = property("user") ;
    public static final Property ppassword = property("password") ;
    public static final Property pgraphName = property("graphName") ;
    public static final Property preadAllGraphs = property("readAllGraphs") ;


    private static Resource resource(String localname) { return ResourceFactory.createResource(iri(localname)) ; }
    private static Property property(String localname) { return ResourceFactory.createProperty(iri(localname)) ; }
        
    private static String iri(String localname)
    {
        String uri = NS+localname ;
        IRI iri = IRIResolver.parseIRI(uri) ;
        if ( iri.hasViolation(true) )
            throw new FusekiException("Bad IRI: "+iri) ;
        if ( ! iri.isAbsolute() )
            throw new FusekiException("Bad IRI: "+iri) ;
        
        return uri ;
    }
}
