package org.redpill.alfresco.numbering.it.component;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.site.SiteInfo;
import org.apache.log4j.Logger;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.redpill.alfresco.numbering.component.NumberingComponent;
import org.redpill.alfresco.test.AbstractRepoIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.annotation.Repeat;

/**
 * This component handles numbering of objects
 *
 * @author Marcus Svensson - Redpill Linpro AB
 *
 */
public class NumberingComponentIntegrationTest extends AbstractRepoIntegrationTest {

  private static final Logger LOG = Logger.getLogger(NumberingComponentIntegrationTest.class);

  private static final String DEFAULT_USERNAME = "testuser_" + System.currentTimeMillis();

  private static SiteInfo site;

  private static final String TEST_DOC = "test.pdf";

  private static final int THREADS = 100;
  private static final int REPEAT = 10;

  @Autowired
  @Qualifier("rl.exampleBasicNumberingComponent")
  protected NumberingComponent numberingComponent;

  @Autowired
  @Qualifier("rl.examplePrefixNumberingComponent")
  protected NumberingComponent prefixNumberingComponent;
  
  @Autowired
  @Qualifier("rl.exampleCurrentDatePrefixNumberingComponent")
  protected NumberingComponent currentDatePrefixNumberingComponent;
  
  
  static FileInfo uploadDocument;

  
  @Test
  public void testPrefixNumbering() {
    long currentNumber = prefixNumberingComponent.getNextNumber(uploadDocument.getNodeRef());
    long nextNumber = prefixNumberingComponent.getNextNumber(uploadDocument.getNodeRef());
    assertEquals(currentNumber+1,nextNumber);
    String decoratedNextNumber = prefixNumberingComponent.getDecoratedNextNumber(uploadDocument.getNodeRef());

    String expected = "D-"+(nextNumber+1);
    assertEquals(expected, decoratedNextNumber);
  }
  
  @Test
  public void testCurrentDatePrefixNumbering() {
    long currentNumber = currentDatePrefixNumberingComponent.getNextNumber(uploadDocument.getNodeRef());
    long nextNumber = currentDatePrefixNumberingComponent.getNextNumber(uploadDocument.getNodeRef());
    assertEquals(currentNumber+1,nextNumber);
    String decoratedNextNumber = currentDatePrefixNumberingComponent.getDecoratedNextNumber(uploadDocument.getNodeRef());
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
    String expected = sdf.format(new Date());
    expected = expected+"-"+(nextNumber+1);
    assertEquals(expected, decoratedNextNumber);
  }
  
  @Test
  @Repeat(value = REPEAT)
  public void testNumbering() {
    long currentNumber = numberingComponent.getNextNumber(uploadDocument.getNodeRef());
    long nextNumber = numberingComponent.getNextNumber(uploadDocument.getNodeRef());
    assertEquals("Expected the next number " + nextNumber + " to be greater than the current number " + currentNumber, currentNumber + 1, nextNumber);
  }

  @Test
  public void testNumberingMultiThread() throws Exception {
    long currentNumber = numberingComponent.getNextNumber(uploadDocument.getNodeRef());
    ArrayList<RunnableThread> threadList = new ArrayList<RunnableThread>(THREADS);
    for (int i = 0; i < THREADS; i++) {
      threadList.add(new RunnableThread("thread" + i));
    }
    try {
      while (threadList.size() > 0) {
        RunnableThread runnableThread = threadList.get(0);
        if (runnableThread.getStatus() == Thread.State.TERMINATED) {
          threadList.remove(runnableThread);
          Exception exception = runnableThread.getException();
          if (exception != null) {
            // throw new Exception("Exception occured",exception);
            throw exception;
          }
        } else {
          LOG.info("Waiting for all threads to complete");
          Thread.currentThread().sleep(100);
        }
      }
    } catch (InterruptedException e) {
    }

    long nextNumber = numberingComponent.getNextNumber(uploadDocument.getNodeRef());

    assertEquals(currentNumber + THREADS * REPEAT + 1, nextNumber);
    String nextNumberString = numberingComponent.getDecoratedNextNumber(uploadDocument.getNodeRef());
    
    //Test that the decorated number also works
    nextNumber = Long.parseLong(nextNumberString);
    assertEquals(currentNumber + THREADS * REPEAT + 2, nextNumber);
  }

  class RunnableThread implements Runnable {

    Thread runner;
    Exception exception = null;

    public RunnableThread() {
    }

    public RunnableThread(String threadName) {
      runner = new Thread(this, threadName); // (1) Create a new thread.
      runner.start(); // (2) Start the thread.
    }

    public void run() {
      try {

        _authenticationComponent.setCurrentUser(AuthenticationUtil.getAdminUserName());
        LOG.debug("Generating " + REPEAT + " numbers in a separate thread");
        for (int i = 0; i < REPEAT; i++) {
          Thread.sleep(1);
          NodeRef nodeRef = uploadDocument.getNodeRef();
          numberingComponent.getNextNumber(nodeRef);
        }
      } catch (Exception e) {
        exception = e;
      }
    }

    public Thread.State getStatus() {
      return runner.getState();
    }

    public Exception getException() {
      return exception;
    }
  }

  @Override
  public void beforeClassSetup() {
    super.beforeClassSetup();

    createUser(DEFAULT_USERNAME);

    _authenticationComponent.setCurrentUser(DEFAULT_USERNAME);

    site = createSite();
    uploadDocument = uploadDocument(site, TEST_DOC);
  }

  @Override
  public void afterClassSetup() {
    deleteSite(site);

    _authenticationComponent.setCurrentUser(_authenticationComponent.getSystemUserName());

    deleteUser(DEFAULT_USERNAME);

    _authenticationComponent.clearCurrentSecurityContext();
  }
}
