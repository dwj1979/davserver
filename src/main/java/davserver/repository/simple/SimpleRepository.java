package davserver.repository.simple;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.io.IOUtils;

import davserver.DAVUtil;
import davserver.repository.Collection;
import davserver.repository.ILockManager;
import davserver.repository.IRepository;
import davserver.repository.Resource;
import davserver.repository.error.ConflictException;
import davserver.repository.error.NotAllowedException;
import davserver.repository.error.NotFoundException;
import davserver.repository.error.ResourceExistsException;
import davserver.utils.SimpleLockManager;

public class SimpleRepository implements IRepository {

	private SimpleCollection root;
	
	private SimpleLockManager lmanager;
	
	public SimpleRepository() {
		root    = new SimpleCollection("");
		lmanager = new SimpleLockManager();
	}
	
	public boolean supportLocks() {
		return false;
	}
	
	public Collection createCollection(String uri) throws ResourceExistsException,NotAllowedException,ConflictException {
		List<String> comps = DAVUtil.getPathComps(uri);
		
		if (comps.size() == 0) {
			throw new ResourceExistsException("resource exists");
		} 
		
		SimpleCollection cur = root;
		for (int i=0;i<comps.size()-1;i++) {
			Resource r = cur.getChild(comps.get(i));
			if (r == null) {
				throw new ConflictException("no parent found");
			}
			if (!(r instanceof SimpleCollection)) {
				throw new NotAllowedException("parent is no collection");
			}
			cur = (SimpleCollection)r;
		}
		
		if (cur == null) {
			throw new ConflictException("no parent found");
		}
		
		String   last   = comps.get(comps.size()-1);
		Resource active = cur.getChild(last);
		
		if (active != null) {
			if (active instanceof SimpleCollection)
				return (SimpleCollection)active;
			else
				throw new ConflictException("exists as file");
		}
		
		SimpleCollection coll = new SimpleCollection(last);
		cur.addChild(last, coll);
		
		System.out.println("added child collection :" + last);
		return coll;
	}
	
	public Resource createResource(String ref,InputStream data) throws ConflictException,NotAllowedException,IOException {
		List<String> comps = DAVUtil.getPathComps(ref);
		
		if (comps.size() == 0) {
			throw new ConflictException("cannot write to root resource");
		} 
				
		SimpleCollection cur = root;
		for (int i=0;i<comps.size()-1;i++) {
			Resource r = cur.getChild(comps.get(i));
			if (!(r instanceof SimpleCollection)) {
				throw new NotAllowedException("parent is no collection");
			}
			cur = (SimpleCollection)r;
		}
		
		if (cur == null) {
			throw new ConflictException("no parent found");
		}
		
		String   last   = comps.get(comps.size()-1);
		Resource active = cur.getChild(last);
		
		if (active == null) {
			active = new SimpleResource(last);
			cur.addChild(last, active);
			System.out.println("added: " + last);
		} else if (active instanceof SimpleCollection) {
			throw new NotAllowedException("cannot write to an collection");
		} 
		
		if (active instanceof SimpleResource) {
			String strdata = IOUtils.toString(data, "utf-8");
			System.out.println(strdata);
			((SimpleResource)active).setContent(strdata);
		}
		
		return active;
	}
		
	public void remove(String uri) throws NotFoundException, NotAllowedException {
		System.out.println("locate resource: " + uri);
		
		// check root collection
		if (uri.compareTo("/")==0) {
			throw new NotAllowedException("root cannot be removed");
		}
		
		List<String> comps = DAVUtil.getPathComps(uri);
		System.out.println("path: " + comps.size() + ":" + root);
		SimpleCollection cur = root;
		for (int i=0;i<comps.size()-1;i++) {
			if (cur == null) {
				throw new NotFoundException(uri + " not found");
			}
			System.out.println("check " + comps.get(i));
			Resource r = cur.getChild(comps.get(i));
			if (r instanceof SimpleCollection) {
				cur = (SimpleCollection)r;
			} else {
				cur = null;
			}
		}
		
		if (cur == null) {
			throw new NotFoundException("resource not found");
		} else {
			String k = comps.get(comps.size()-1);
			if (!cur.getChilds().containsKey(k)) {
				throw new NotFoundException("not found");
			} else {
				cur.getChilds().remove(k);
			}
		}
	}

	public Resource locate(String uri) throws NotFoundException, NotAllowedException {
		System.out.println("locate resource: " + uri);
		
		// check root collection
		if (uri.compareTo("/")==0) {
			System.out.println("located root");
			return root;
		}
		
			List<String> comps = DAVUtil.getPathComps(uri);
		SimpleCollection cur = root;
		for (int i=0;i<comps.size()-1;i++) {
			if (cur == null) {
				throw new NotFoundException(uri + " not found");
			}
			Resource r = cur.getChild(comps.get(i));
			if (r instanceof SimpleCollection) {
				cur = (SimpleCollection)r;
			} else {
				cur = null;
			}
		}
		
		if (cur == null) {
			throw new NotFoundException("resource not found");
		} else {
			Resource r = cur.getChild(comps.get(comps.size()-1));
			if (r == null) {
				throw new NotFoundException("not found");
			} else {
				return r;
			}
		}
	}
	
	public ILockManager getLockManager() {
		return lmanager;
	}
	
	@Override
	public String toString() {
		return root.toString();
	}


}
