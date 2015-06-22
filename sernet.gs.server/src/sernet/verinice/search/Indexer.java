/*******************************************************************************
 * Copyright (c) 2015 Daniel Murygin.
 *
 * This program is free software: you can redistribute it and/or 
 * modify it under the terms of the GNU Lesser General Public License 
 * as published by the Free Software Foundation, either version 3 
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,    
 * but WITHOUT ANY WARRANTY; without even the implied warranty 
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. 
 * If not, see <http://www.gnu.org/licenses/>.
 * 
 * Contributors:
 *     Daniel Murygin <dm[at]sernet[dot]de> - initial API and implementation
 ******************************************************************************/
package sernet.verinice.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;
import org.elasticsearch.action.ActionResponse;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.security.context.SecurityContext;
import org.springframework.security.context.SecurityContextHolder;

import sernet.gs.server.security.DummyAuthentication;
import sernet.gs.service.RetrieveInfo;
import sernet.gs.service.ServerInitializer;
import sernet.gs.service.TimeFormatter;
import sernet.verinice.interfaces.IBaseDao;
import sernet.verinice.interfaces.IElementTitleCache;
import sernet.verinice.model.bsi.ITVerbund;
import sernet.verinice.model.common.CnATreeElement;
import sernet.verinice.model.iso27k.Organization;

/**
 * @author Daniel Murygin <dm[at]sernet[dot]de>
 */
public class Indexer {

    private static final Logger LOG = Logger.getLogger(Indexer.class);

    private static final int DEFAULT_NUMBER_OF_THREADS = 8;

    private IBaseDao<CnATreeElement, Integer> elementDao;

    private IElementTitleCache titleCache;

    private long indexingStart;

    private DummyAuthentication authentication = new DummyAuthentication();

    /**
     * Factory to create {@link IndexThread} instances configured in
     * veriniceserver-search.xml
     */
    private ObjectFactory indexThreadFactory;

    private int amountOfIndexThreads;

    private SecurityContext ctx;

    private boolean dummyAuthAdded;

    /**
     * Creates an index in an non blocking way, means this method creates all
     * necessary index threads and returns immediately. It gives no guarantee
     * that an index is available after this method is finished. The index will
     * be available after an arbitrary amount of time.
     *
     * <p>
     * If you are using log4j in debug method the time consumption of the index
     * process is logged, but the non blocking behavior will stay the same.
     * </p>
     *
     *
     * <p>
     * If you need to know, when indexing is finished take a look at
     * {@link #blockingIndexing()}
     * </p>
     *
     */
    public void nonBlockingIndexing() {

        try {
            ServerInitializer.inheritVeriniceContextState();
            initializeSecurityIndex();

            CompletionService<CnATreeElement> completionService = doIndex();
            logNonBlockingIndexingTermination(completionService);
        } catch (Exception e) {
            LOG.error("Error while indexing elements.", e);
        } finally {
            removeDummyAuthentication();
        }
    }

    private void initializeSecurityIndex() {
        dummyAuthAdded = false;
        ctx = SecurityContextHolder.getContext();

        if (ctx.getAuthentication() == null) {
            ctx.setAuthentication(authentication);
            dummyAuthAdded = true;
        }
    }

    @SuppressWarnings("unchecked")
    private CompletionService<CnATreeElement> doIndex() throws InterruptedException, ExecutionException {

        indexingStart = System.currentTimeMillis();

        ExecutorService executorService = createExecutor();
        CompletionService<CnATreeElement> completionService = new ExecutorCompletionService<CnATreeElement>(executorService);

        List<CnATreeElement> elementList = getElementDao().findAll(RetrieveInfo.getPropertyInstance().setPermissions(true));

        if (LOG.isInfoEnabled()) {
            LOG.info("Elements: " + elementList.size() + ", indexingStart indexing...");
        }

        getTitleCache().load(new String[] { ITVerbund.TYPE_ID_HIBERNATE, Organization.TYPE_ID });
        Collection<IndexThread> indexThreads = createIndexThreads(elementList);
        amountOfIndexThreads = indexThreads.size();

        for (IndexThread indexThread : indexThreads) {
            completionService.submit(indexThread);
        }

        return completionService;
    }

    private void logNonBlockingIndexingTermination(final CompletionService<CnATreeElement> completionService) {
        if (LOG.isDebugEnabled()) {
            Executors.newFixedThreadPool(1).execute(new Runnable() {
                @Override
                public void run() {
                    ServerInitializer.inheritVeriniceContextState();
                    awaitIndexingTermination(completionService);
                    printIndexingTimeConsumption();
                }
            });
        }
    }

    private void removeDummyAuthentication() {
        if (dummyAuthAdded) {
            ctx.setAuthentication(null);
            dummyAuthAdded = false;
        }
    }

    private void printIndexingTimeConsumption() {
        long end = System.currentTimeMillis();
        long ms = end - indexingStart;
        LOG.debug("All threads created, runtime: " + ms + " ms, readable time: " + TimeFormatter.getHumanRedableTime(ms));
    }

    /**
     * Creates an elastic search in a blocking manner. After this method is
     * finished the index will be available.
     *
     * <p>
     * *Note*: The indexing is done concurrently, so it is still fast. The only
     * restriction of this blocking method is, that is waiting until the last
     * {@link IndexThread} is completed.
     * </p>
     *
     * <p>If you want to test indexing with junit, this is method to go.</p>
     *
     */
    public void blockingIndexing() {
        try {
            doBlockingIndexing();
        } catch (InterruptedException e) {
            LOG.error("blocking indexing failed: " + e.getLocalizedMessage(), e);
        } catch (ExecutionException e) {
            LOG.error("blocking indexing failed: " + e.getLocalizedMessage(), e);
        }
    }

    private void doBlockingIndexing() throws InterruptedException, ExecutionException {

        ServerInitializer.inheritVeriniceContextState();
        CompletionService<CnATreeElement> completionService = doIndex();

        // This call causes the blocking since it takes every completed task
        // from the executor queue.
        awaitIndexingTermination(completionService);

        printIndexingTimeConsumption();
    }

    private void awaitIndexingTermination(CompletionService<CnATreeElement> completionService) {
        while (Indexer.this.amountOfIndexThreads > 0) {
            try {
                Future<CnATreeElement> future = completionService.take();
                CnATreeElement element = future.get();
                Indexer.this.amountOfIndexThreads--;
                LOG.debug("element was indexed " + element.getTitle() + " - uuid " + element.getUuid());
            } catch (InterruptedException e) {
                LOG.error("indexing tracking failed", e);
            } catch (ExecutionException ex) {
                LOG.error("future task execution failed: " + ex.getLocalizedMessage(), ex);
            }
        }
    }

    private Collection<IndexThread> createIndexThreads(List<CnATreeElement> elementList) {
        Collection<IndexThread> indexThreads = new ArrayList<IndexThread>(0);

        for (CnATreeElement element : elementList) {
            IndexThread indexThread = (IndexThread) indexThreadFactory.getObject();
            indexThread.setElement(element);
            indexThreads.add(indexThread);
        }

        return indexThreads;
    }

    private ExecutorService createExecutor() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Number of threads: " + getMaxNumberOfThreads());
        }
        return Executors.newFixedThreadPool(getMaxNumberOfThreads());
    }

    private int getMaxNumberOfThreads() {
        return DEFAULT_NUMBER_OF_THREADS;
    }

    public ObjectFactory getIndexThreadFactory() {
        return indexThreadFactory;
    }

    public void setIndexThreadFactory(ObjectFactory indexThreadFactory) {
        this.indexThreadFactory = indexThreadFactory;
    }

    public IBaseDao<CnATreeElement, Integer> getElementDao() {
        return elementDao;
    }

    public void setElementDao(IBaseDao<CnATreeElement, Integer> elementDao) {
        this.elementDao = elementDao;
    }

    public IElementTitleCache getTitleCache() {
        return titleCache;
    }

    public void setTitleCache(IElementTitleCache titleCache) {
        this.titleCache = titleCache;
    }

    class LastThread implements Callable<ActionResponse> {

        long startTime;

        private LastThread(long startTime) {
            super();
            this.startTime = startTime;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.concurrent.Callable#call()
         */
        @Override
        public ActionResponse call() throws Exception {
            if (LOG.isInfoEnabled()) {
                long ms = System.currentTimeMillis() - startTime;
                String message = "Indexing finished, runtime: " + TimeFormatter.getHumanRedableTime(ms);
                LOG.info(message);
            }
            return null;
        }

    }

}
