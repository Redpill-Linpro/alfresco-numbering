package org.redpill.alfresco.numbering.decorator;

import java.text.SimpleDateFormat;
import java.util.Date;
import org.alfresco.service.cmr.repository.NodeRef;

/**
 * Number prefixed with a date
 *
 * @author Marcus Svartmark
 */
public class CurrentDatePrefixDecorator extends PrefixDecorator implements Decorator {

  protected String datePattern = "yyyy-MM-dd";

  public void setDatePattern(String datePattern) {
    this.datePattern = datePattern;
  }

  protected String getDatePrefix() {
    SimpleDateFormat df = new SimpleDateFormat(datePattern);
    return df.format(new Date());
  }

  @Override
  public String decorate(String number, NodeRef nodeRef) {
    return decorate(number, nodeRef, getDatePrefix());
  }

}
