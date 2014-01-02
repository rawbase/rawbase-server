/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package be.ugent.mmlab.rawbase.jena;

import be.ugent.mmlab.rawbase.jena.vocabulary.PROV;
import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.impl.PropertyImpl;
import com.hp.hpl.jena.rdf.model.impl.ResourceImpl;
import com.hp.hpl.jena.sparql.core.Quad;
import com.hp.hpl.jena.vocabulary.DCTerms;
import com.hp.hpl.jena.vocabulary.RDF;
import java.security.MessageDigest;
import java.util.ArrayList;
import org.apache.commons.codec.digest.DigestUtils;
import org.joda.time.DateTime;

/**
 *
 * @author mielvandersande
 */
public abstract class RawbaseCommit {
    //Namespaces
    private final String VERSION_NS = "http://example.com/graphs/versions/";
    private final String COMMIT_NS = "http://example.com/commits/";
    
    //constants
    public static final Resource PROV_GRAPHNAME = new ResourceImpl("urn:rawbase:provenance");
    public static final Resource SYSTEM_GRAPHNAME = new ResourceImpl("urn:rawbase:system");
    public static final Property DELTA_KEY_PROPERTY = new PropertyImpl("http://rawbase.github.io/#isDelta");
    
    //Metadata
    private Resource author;
    private Resource committer;
    private Resource parent;
    private String message;
    private DateTime datetime;
    private Resource version;
    private Resource commit;

    public RawbaseCommit(Resource author, Resource committer, String message, DateTime datetime, Resource parent) {
        this.author = author;
        this.committer = committer;
        this.message = message;
        this.datetime = datetime;

        this.parent = parent;

        String[] fields = {author.getURI(), committer.getURI(), message, datetime.toString()};
        String h = hash(fields);

        this.version = new ResourceImpl(VERSION_NS + h);
        this.commit = new ResourceImpl(COMMIT_NS + h);
    }

    private String hash(String[] values) {
        String hash = "";
        for (String value : values) {
            hash += DigestUtils.md5Hex(value);
        }
        return DigestUtils.md5Hex(hash);
    }

    public Resource getHash() {
        return version;
    }

    public Resource getParent() {
        return parent;
    }

    private Quad asQuad(Resource g, Resource s, Property p, Resource o) {
        return new Quad(g.asNode(), new Triple(s.asNode(), p.asNode(), o.asNode()));
    }

    private Quad asQuad(Resource g, Resource s, Property p, String o, RDFDatatype dt) {
        return new Quad(g.asNode(), new Triple(s.asNode(), p.asNode(), NodeFactory.createLiteral(o, dt)));
    }

    public ArrayList<Quad> getPROV(int delta) {
        ArrayList<Quad> quads = new ArrayList<>();

        //Prov activity
        quads.add(asQuad(PROV_GRAPHNAME, commit, RDF.type, PROV.Activity));
        quads.add(asQuad(PROV_GRAPHNAME, commit, PROV.atTime, datetime.toString(), XSDDatatype.XSDdateTime));
        quads.add(asQuad(PROV_GRAPHNAME, commit, PROV.generated, version));
        quads.add(asQuad(PROV_GRAPHNAME, commit, DCTerms.title, message, XSDDatatype.XSDstring));
        quads.add(asQuad(PROV_GRAPHNAME, commit, PROV.wasAssociatedWith, author));

        //Prov person
        quads.add(asQuad(PROV_GRAPHNAME, author, RDF.type, PROV.Person));
        if (committer != null || committer.equals(author)) {
            quads.add(asQuad(PROV_GRAPHNAME, committer, RDF.type, PROV.Person));
            quads.add(asQuad(PROV_GRAPHNAME, committer, PROV.actedOnBehalfOf, author));
        }

        //Prov entities
        quads.add(asQuad(PROV_GRAPHNAME, version, RDF.type, PROV.Entity));
        if (parent != null) {
            quads.add(asQuad(PROV_GRAPHNAME, commit, PROV.used, parent));
            quads.add(asQuad(PROV_GRAPHNAME, parent, RDF.type, PROV.Entity));
            quads.add(asQuad(PROV_GRAPHNAME, version, PROV.wasDerivedFrom, parent));
        }

        //Rawbase delta
        quads.add(asQuad(SYSTEM_GRAPHNAME, version, DELTA_KEY_PROPERTY, "" + delta, XSDDatatype.XSDint));

        return quads;
    }

    public static String asNTriple(Triple t) {
        return asNTriple(t.getSubject(), t.getPredicate(), t.getObject());
    }

    public static String asNTriple(Node s, Node p, Node o) {
        if (o.isURI()) {
            return "<" + s.getURI() + "> <" + p.getURI() + "> <" + o.getURI() + ">. ";
        }

        String dtURI = o.getLiteralDatatypeURI();

        if (dtURI == null) {
            return "<" + s.getURI() + "> <" + p.getURI() + "> \"" + o.getLiteralValue() + "\". ";
        }

        return "<" + s.getURI() + "> <" + p.getURI() + "> \"" + o.getLiteralValue() + "\"^^<" + o.getLiteralDatatypeURI() + ">. ";
    }
    
//    abstract void store();
//    
//    abstract void rollback();

    abstract ArrayList<Quad> getAdditions();

    abstract ArrayList<Quad> getDeletions();

    abstract void addAddition(Quad q);

    abstract void addDeletion(Quad q);
}
