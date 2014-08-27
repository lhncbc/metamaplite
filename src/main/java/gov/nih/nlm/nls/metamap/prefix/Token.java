package gov.nih.nlm.nls.metamap.prefix;

import java.io.Serializable;

/**
 * Describe interface Token here.
 *
 *
 * Created: Thu Jan 17 13:05:20 2013
 *
 * @author <a href="mailto:wjrogers@mail.nih.gov">Willie Rogers</a>
 * @version 1.0
 */
public interface Token extends Serializable {
  String getTokenText();
}
