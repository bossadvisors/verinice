package sernet.gs.ui.rcp.main.service.statscommands;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import sernet.gs.service.NumericStringComparator;
import sernet.verinice.interfaces.GenericCommand;
import sernet.verinice.interfaces.IAuthAwareCommand;
import sernet.verinice.interfaces.IAuthService;
import sernet.verinice.interfaces.IBaseDao;
import sernet.verinice.iso27k.service.ControlMaturityService;
import sernet.verinice.model.common.CnATreeElement;
import sernet.verinice.model.iso27k.ControlGroup;
import sernet.verinice.model.iso27k.IControl;
import sernet.verinice.model.samt.SamtTopic;

@SuppressWarnings("serial")
/**
 * Returns two values: total topics and answered topics (state != default value).
 * 
 */
public class SamtProgressSummary extends GenericCommand implements IAuthAwareCommand {
    /**
     * 
     */
    public static final String ANSWERED = "Answered";

    /**
     * 
     */
    public static final String UNANSWERED = "Unanswered";

    // FIXME externalize strings
    
    private transient Logger log = Logger.getLogger(SamtProgressSummary.class);

    public Logger getLog() {
        if (log == null) {
            log = Logger.getLogger(SamtProgressSummary.class);
        }
        return log;
    }

    private transient IAuthService authService;

    private Integer id;

    private Map<String, Integer> result;

    public SamtProgressSummary(ControlGroup cg) {
        id = cg.getDbId();
    }

    @Override
    public void execute() {
        result = new HashMap<String, Integer>();
        result.put(UNANSWERED, 0);
        result.put(ANSWERED, 0);

        IBaseDao<ControlGroup, Serializable> dao = getDaoFactory().getDAO(ControlGroup.class);
        ControlGroup cg = dao.findById(id);
        loadSamtTopics(cg);
    }

    private void loadSamtTopics(ControlGroup cg) {
        if (cg == null)
            return;
        ControlMaturityService maturityService = new ControlMaturityService();

        for (CnATreeElement e : cg.getChildren()) {
            if (e instanceof SamtTopic) {
                SamtTopic st = (SamtTopic) e;
                // ignore chapters 0.x (Copyright et al):
                if (!st.getTitle().startsWith("0")) {
                    if (maturityService.getImplementationState(st) == IControl.IMPLEMENTED_NOTEDITED) {
                        result.get(UNANSWERED);
                    }
                    else {
                        result.get(ANSWERED);
                    }
                }
            } else if (e instanceof ControlGroup) {
                loadSamtTopics((ControlGroup) e);
            } else {
                log.warn("found unexpected child for control group: " + e);
            }

        }
    }

    /**
     * @return the result
     */
    public Map<String, Integer> getResult() {
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * sernet.gs.ui.rcp.main.service.commands.IAuthAwareCommand#getAuthService()
     */
    public IAuthService getAuthService() {
        return authService;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * sernet.gs.ui.rcp.main.service.commands.IAuthAwareCommand#setAuthService
     * (sernet.gs.ui.rcp.main.service.IAuthService)
     */
    public void setAuthService(IAuthService service) {
        this.authService = service;
    }

}