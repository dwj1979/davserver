package davserver.protocol;

import java.text.ParseException;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.http.Header;
import org.apache.http.HttpRequest;

import davserver.DAVException;
import davserver.DAVUrl;
import davserver.protocol.header.IfHeader;
import davserver.repository.IRepository;
import davserver.repository.LockEntry;
import davserver.repository.Resource;
import davserver.repository.error.NotAllowedException;
import davserver.repository.error.NotFoundException;

/**
 * Helper class for common request functions 
 * 
 * @author tkrieger
 *
 */
public class DAVRequest {
	
	/**
	 * Help method to test lock (precondition)
	 * 
	 * @param req
	 * @param repos
	 * @param url
	 * @throws DAVException
	 */
	public static void checkLock(HttpRequest req,IRepository repos,DAVUrl url) throws DAVException {
		Resource r = null;
		
		// get resource 
		try {
			r = repos.locate(url.getResref());			
		} catch (NotAllowedException e) {
			
		} catch (NotFoundException e) {
			
		}
		
		// check locks
		if (repos.supportLocks()) {
			HashMap<String,LockEntry> le = repos.getLockManager().checkLocked(url.getResref());
			if (le != null && le.size() > 0) {
				// check precondition
				Header hif = req.getFirstHeader("If");
				if (hif == null) {
					throw new DAVException(423,"no lock token submitted");
				} 
				try {
					IfHeader lh = IfHeader.parseIfHeader(hif.getValue());
					if (lh.getResource() == null && lh.getConditions() == null) {
						throw new DAVException(400,"bad request");
					}
					if (lh.getResource() != null) {
						DAVUrl curl = new DAVUrl(lh.getResource().getPath(), url.getPrefix());
						if (url.getResref().compareTo(curl.getResref())!=0) {
							System.out.println(curl.getResref());
							throw new DAVException(400,"bad if uri");
						}						
					}
					HashSet<String> tokens = lh.evaluate(le, (r != null ? r.getETag() : null));
					if (tokens == null) {
						throw new DAVException(412,"precondition failed");
					}
				} catch (ParseException e) {
					e.printStackTrace();
					throw new DAVException(400,"bad request");
				}
			}
		}		
	}

}