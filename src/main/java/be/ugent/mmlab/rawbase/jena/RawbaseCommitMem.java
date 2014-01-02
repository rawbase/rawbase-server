/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package be.ugent.mmlab.rawbase.jena;

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.core.Quad;
import java.util.ArrayList;
import org.joda.time.DateTime;

/**
 *
 * @author mielvandersande
 */
public class RawbaseCommitMem extends RawbaseCommit{

    private ArrayList<Quad> additions;
    private ArrayList<Quad> deletions;

    public RawbaseCommitMem(Resource author, Resource committer, String message) {
        this(author, committer, message, new DateTime(), null);
    }

    public RawbaseCommitMem(Resource author, Resource committer, String message, Resource parent) {
        this(author, committer, message, new DateTime(), parent);
    }

    public RawbaseCommitMem(Resource author, Resource committer, String message, DateTime datetime) {
        this(author, committer, message, datetime, null);
    }

    public RawbaseCommitMem(Resource author, Resource committer, String message, DateTime datetime, Resource parent) {
        super(author, committer, message, datetime, parent);
        
        this.additions = new ArrayList<>();
        this.deletions = new ArrayList<>();

    }

    @Override
    public void addAddition(Quad q) {
        this.additions.add(q);
    }

    @Override
    public void addDeletion(Quad q) {
        this.deletions.add(q);
    }

    @Override
    public ArrayList<Quad> getAdditions() {
        return this.additions;
    }

    @Override
    public ArrayList<Quad> getDeletions() {
        return this.deletions;
    }
}
