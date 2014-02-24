/*
 *  $Id$
 *
 *  This file is part of the OpenLink Software Virtuoso Open-Source (VOS)
 *  project.
 *
 *  Copyright (C) 1998-2013 OpenLink Software
 *
 *  This project is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the
 *  Free Software Foundation; only version 2 of the License, dated June 1991.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 *
 */
package be.ugent.mmlab.rawbase.jena;


import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.sparql.core.DatasetGraph;
import virtuoso.jena.driver.VirtGraph;

public class RawbaseQueryExecutionFactory extends QueryExecutionFactory{

    private RawbaseQueryExecutionFactory() {
    }

    static public RawbaseQueryExecution create(Query query, VirtGraph graph, Integer[] vPath) {
        RawbaseQueryExecution ret = new RawbaseQueryExecution(query.toString(), graph, vPath);
        return ret;
    }

    static public RawbaseQueryExecution create(String query, VirtGraph graph, Integer[] vPath) {
        RawbaseQueryExecution ret = new RawbaseQueryExecution(query, graph, vPath);
        return ret;
    }

    /* TODO */
    static public QueryExecution create(Query query, Dataset dataset, Integer[] vPath) {
        DatasetGraph dsg = dataset.asDatasetGraph();

        if (dsg.getDefaultGraph() instanceof RawbaseDataSet) {
            RawbaseQueryExecution ret = new RawbaseQueryExecution(query.toString(), (VirtGraph) dsg.getDefaultGraph(), vPath);
            return ret;
        } else {
            return make(query, dataset);
        }
    }

//    static public QueryExecution create(String queryStr, Dataset dataset, Integer[] vPath) {
//        DatasetGraph dsg = dataset.asDatasetGraph();
//
//        if (dsg.getDefaultGraph() instanceof VirtDataset) {
//            RawbaseQueryExecution ret = new RawbaseQueryExecution(queryStr, (VirtGraph) dsg.getDefaultGraph(), vPath);
//            return ret;
//        } else {
//            return make(makeQuery(queryStr), dataset);
//        }
//    }
//
//    static public QueryExecution create(Query query, FileManager fm) {
//        checkArg(query);
//        QueryExecution qe = make(query);
//        if (fm != null) {
//            qe.setFileManager(fm);
//        }
//        return qe;
//    }
//
//    static public QueryExecution create(String queryStr, FileManager fm) {
//        checkArg(queryStr);
//        return create(makeQuery(queryStr), fm);
//    }
//
//    // ---------------- Query + Model
//    static public QueryExecution create(Query query, Model model) {
//        checkArg(query);
//        checkArg(model);
//
//        if (model.getGraph() instanceof VirtGraph) {
//            RawbaseQueryExecution ret = new RawbaseQueryExecution(query.toString(), (VirtGraph) model.getGraph());
//            return ret;
//        } else {
//            return make(query, new DatasetImpl(model));
//        }
//    }
//
//    static public QueryExecution create(String queryStr, Model model) {
//        checkArg(queryStr);
//        checkArg(model);
//        if (model.getGraph() instanceof VirtGraph) {
//            RawbaseQueryExecution ret = new RawbaseQueryExecution(queryStr, (VirtGraph) model.getGraph());
//            return ret;
//        } else {
//            return create(makeQuery(queryStr), model);
//        }
//    }
//
//
//    // ---------------- Internal routines
//    // Make query
//    static private Query makeQuery(String queryStr) {
//        return QueryFactory.create(queryStr);
//    }
//
//    // ---- Make executions
//    static protected QueryExecution make(Query query) {
//        return make(query, null);
//    }
//
//    static protected QueryExecution make(Query query, Dataset dataset) {
//        return make(query, dataset, null);
//    }
//
//    //MVS: These need to be reviewed
//    static protected QueryExecution make(Query query, Dataset dataset, Context context)
//    {
//	    query.setResultVars() ;
//        if ( context == null )
//            context = ARQ.getContext();  // .copy done in QueryExecutionBase -> Context.setupContext. 
//        DatasetGraph dsg = null ;
//        if ( dataset != null )
//            dsg = dataset.asDatasetGraph() ;
//        QueryEngineFactory f = findFactory(query, dsg, context);
//        if ( f == null )
//        {
//            Log.warn(QueryExecutionFactory.class, "Failed to find a QueryEngineFactory for query: "+query) ;
//            return null ;
//        }
//        return new QueryExecutionBase(query, dataset, context, f) ;
//    }
//    //MVS: These need to be reviewed
//    static private QueryEngineFactory findFactory(Query query, DatasetGraph dataset, Context context)
//    {
//        return QueryEngineRegistry.get().find(query, dataset, context);
//    }
//
//    static private QueryEngineHTTP makeServiceRequest(String service, Query query) {
//        return new QueryEngineHTTP(service, query);
//    }
//
//    static private void checkNotNull(Object obj, String msg) {
//        if (obj == null) {
//            throw new IllegalArgumentException(msg);
//        }
//    }
//
//    static private void checkArg(Model model) {
//        checkNotNull(model, "Model is a null pointer");
//    }
//
//    static private void checkArg(String queryStr) {
//        checkNotNull(queryStr, "Query string is null");
//    }
//
//    static private void checkArg(Query query) {
//        checkNotNull(query, "Query is null");
//    }
}
