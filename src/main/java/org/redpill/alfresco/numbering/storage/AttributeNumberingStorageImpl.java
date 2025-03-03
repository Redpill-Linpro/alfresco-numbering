package org.redpill.alfresco.numbering.storage;

import org.alfresco.repo.lock.JobLockService;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.cmr.attributes.AttributeService;
import org.alfresco.service.namespace.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * EXPERIMENTAL! Store numbers using attribute service. Currently not well
 * tested. Do not use in production. It is not cluster safe.
 *
 * @author Marcus Svartmark - Redpill Linpro AB
 */
public class AttributeNumberingStorageImpl implements NumberingStorage, InitializingBean {

  private static final Logger LOG = LoggerFactory.getLogger(AttributeNumberingStorageImpl.class);

  protected static long DEFAULT_LOCK_TTL = 30000L;
  protected ThreadLocal<String> lockThreadLocal = new ThreadLocal<String>();

  protected long lockTTL = DEFAULT_LOCK_TTL;

  protected JobLockService jobLockService;
  protected RetryingTransactionHelper retryingTransactionHelper;

  protected AttributeService attributeService;

  @Override
  public long getNextNumber(final long initialValue, final String id) {
    QName lockName = QName.createQName(ATTR_ID + "." + id + ".lock");
    jobLockService.getLock(lockName, lockTTL, 100, 100);
    return retryingTransactionHelper.doInTransaction(new RetryingTransactionHelper.RetryingTransactionCallback<Long>() {
      @Override
      public Long execute() throws Throwable {

        if (!attributeService.exists(ATTR_ID, id)) {
          if (LOG.isDebugEnabled()) {
            LOG.debug("Creating attribute for counter with id " + ATTR_ID + "." + id + ": " + initialValue);
          }
          attributeService.createAttribute(initialValue, ATTR_ID, id);
          return initialValue;
        } else {
          Long attributeValue = (Long) attributeService.getAttribute(ATTR_ID, id);

          attributeService.setAttribute(++attributeValue, ATTR_ID, id);
          if (LOG.isTraceEnabled()) {
            LOG.trace("Increased counter with id " + ATTR_ID + "." + id + " to " + attributeValue);
          }
          return attributeValue;
        }
      }
    }, false, true);

  }

  /**
   * this method generate the next number based on optionValue, option value is based on selected value from drop down list
   */
  @Override
  public long getNextNumber(final long initialValue, final String ids,final String optionValue) {
    QName lockName = QName.createQName(ATTR_ID + "." + optionValue + ".lock");
    jobLockService.getLock(lockName, lockTTL, 100, 100);
    return retryingTransactionHelper.doInTransaction(new RetryingTransactionHelper.RetryingTransactionCallback<Long>() {
      @Override
      public Long execute() throws Throwable {

        if (!attributeService.exists(ATTR_ID, optionValue)) {
          if (LOG.isDebugEnabled()) {
            LOG.debug("Creating attribute for counter with id " + ATTR_ID + "." + optionValue + ": " + initialValue);
          }
          attributeService.createAttribute(initialValue, ATTR_ID, optionValue);
          return initialValue;
        } else {
          Long attributeValue = (Long) attributeService.getAttribute(ATTR_ID, optionValue);

          attributeService.setAttribute(++attributeValue, ATTR_ID, optionValue);
          if (LOG.isTraceEnabled()) {
            LOG.trace("Increased counter with id " + ATTR_ID + "." + optionValue + " to " + attributeValue);
          }
          return attributeValue;
        }
      }
    }, false, true);

  }

  
  
  public void setJobLockService(JobLockService jobLockService) {
    this.jobLockService = jobLockService;
  }

  public void setRetryingTransactionHelper(RetryingTransactionHelper retryingTransactionHelper) {
    this.retryingTransactionHelper = retryingTransactionHelper;
  }

  public void setAttributeService(AttributeService attributeService) {
    this.attributeService = attributeService;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    Assert.notNull(jobLockService);
    Assert.notNull(retryingTransactionHelper);
    Assert.notNull(attributeService);
  }

}
