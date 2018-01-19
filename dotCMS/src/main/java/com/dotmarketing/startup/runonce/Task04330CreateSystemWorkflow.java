

package com.dotmarketing.startup.runonce;

import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.repackage.com.fasterxml.jackson.databind.DeserializationFeature;
import com.dotcms.repackage.com.fasterxml.jackson.databind.ObjectMapper;
import com.dotcms.util.ConversionUtils;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.startup.StartupTask;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import org.apache.velocity.util.ClassUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This upgrade task will creates the system workflow
 *
 * @author Jose Orsini
 * @version 4.3.0
 * @since Jan 11, 2018
 *
 */
public class Task04330CreateSystemWorkflow implements StartupTask {


    public    static final String SYSTEMWORKFLOW_JSON_PATH = "com/dotmarketing/startup/runonce/json/systemworkflow.json";
    protected static final String SELECT_SCHEME_SQL        = "select * from workflow_scheme where id = ?";
    protected static final String INSERT_SCHEME            = "insert into workflow_scheme (id, name, description, archived, mandatory, entry_action_id, default_scheme, mod_date) values (?,?,?,?,?,?,?,?)";
    protected static final String INSERT_STEP              = "insert into workflow_step (id, name, scheme_id,my_order,resolved,escalation_enable,escalation_action,escalation_time) values (?, ?, ?, ?, ?, ?, ?, ?) ";
    protected static final String INSERT_ACTION            = "insert into workflow_action (id, scheme_id, name, condition_to_progress, next_step_id, next_assign, my_order, assignable, commentable, icon, use_role_hierarchy_assign, requires_checkout, show_on) values (?, ?, ?, ?, ?, ?, ?,?, ?, ?,?,?,?)";
    protected static final String INSERT_ACTION_FOR_STEP   = "insert into workflow_action_step(action_id, step_id, action_order) values (?,?,?)";
    protected static final String INSERT_ACTION_CLASS      = "insert into workflow_action_class (id, action_id, name, my_order, clazz) values (?,?, ?, ?, ?)";
    protected static final String DELIMITER                = ",";


    @Override
    public boolean forceRun() {
        return Boolean.TRUE;
    }

    @Override
    @CloseDBIfOpened
    public void executeUpgrade() throws DotDataException {

        final Map systemWorkflowMap         = this.getSystemWorkflowMap();
        boolean   doesNotHaveSystemWorkflow = false;

        if (UtilMethods.isSet (systemWorkflowMap)) {

            Logger.debug(this, "The System Workflow does not exists, creating it");
            final List schemeList = (List) systemWorkflowMap.get("schemes");

            if (UtilMethods.isSet(schemeList)) {

                final Map schemeMap = (Map)schemeList.get(0);
                if (!this.existsSystemWorkflow((String) schemeMap.get("id"))) {

                    this.createScheme       (schemeMap);
                    this.createSteps        ((List)systemWorkflowMap.get("steps"));
                    this.createActions      ((List)systemWorkflowMap.get("actions"));
                    this.createActionSteps  ((List)systemWorkflowMap.get("actionSteps"));
                    this.createActionClasses((List)systemWorkflowMap.get("actionClasses"));
                }
            } else {
                doesNotHaveSystemWorkflow = true;
            }
        } else {
            doesNotHaveSystemWorkflow = true;
        }

        if (doesNotHaveSystemWorkflow) {

            Logger.warn(this, "The resource: "
                    + SYSTEMWORKFLOW_JSON_PATH + ", does not have any system workflow");
        }
    } // executeUpgrade.


    private void createActionClasses(final List actionClasses) throws DotDataException  {

        for (Object actionClass : actionClasses) {

            this.createActionClass((Map)actionClass);
        }
    }

    private void createActionClass(final Map actionClass) throws DotDataException {

        new DotConnect().setSQL(INSERT_ACTION_CLASS)
                .addParam(actionClass.get("id"))
                .addParam(actionClass.get("actionId"))
                .addParam(actionClass.get("name"))
                .addParam(ConversionUtils.toInt(actionClass.get("order").toString(), 0))
                .addParam(actionClass.get("clazz"))
                .loadResult();
    }

    private void createActionSteps(final List actionSteps)  throws DotDataException {

        for (Object actionStep : actionSteps) {

            this.createActionStep((Map)actionStep);
        }
    }

    private void createActionStep(final Map actionStep) throws DotDataException {

        new DotConnect().setSQL(INSERT_ACTION_FOR_STEP)
                .addParam(actionStep.get("actionId"))
                .addParam(actionStep.get("stepId"))
                .addParam(ConversionUtils.toInt(actionStep.get("actionOrder"), 0))
                .loadResult();
    }

    private void createActions(final List actions) throws DotDataException  {

        for (Object action : actions) {

            this.createAction((Map)action);
        }
    }

    private void createAction(final Map action) throws DotDataException {

        new DotConnect().setSQL(INSERT_ACTION)
                .addParam((String)action.get("id"))
                .addParam((String)action.get("schemeId"))
                .addParam((String)action.get("name"))
                .addParam((String)action.get("condition"))
                .addParam((String)action.get("nextStep"))
                .addParam((String)action.get("nextAssign"))
                .addParam(ConversionUtils.toInt(action.get("order").toString(), 0))
                .addParam(Boolean.valueOf(action.get("assignable").toString()))
                .addParam(Boolean.valueOf(action.get("commentable").toString()))
                .addParam((String)action.get("icon"))
                .addParam(Boolean.valueOf(action.get("roleHierarchyForAssign").toString()))
                .addParam(Boolean.valueOf(action.get("requiresCheckout").toString()))
                .addParam(this.getShowOn((List<String>)action.get("showOn")))
                .loadResult();
    }

    private String getShowOn(final List<String> showOn) {

        return showOn.stream().collect(Collectors.joining(DELIMITER));
    }

    private void createSteps(final List steps)   throws DotDataException {

        for (Object step : steps) {

            this.createStep((Map)step);
        }
    }

    private void createStep(final Map step)  throws DotDataException  {

        new DotConnect().setSQL(INSERT_STEP)
                .addParam((String)step.get("id"))
                .addParam((String)step.get("name"))
                .addParam((String)step.get("schemeId"))
                .addParam(ConversionUtils.toInt(step.get("myOrder"), 0))
                .addParam(Boolean.valueOf(step.get("resolved").toString()))
                .addParam(false)
                .addParam((Object)null)
                .addParam(0)
                .loadResult();
    }

    private void createScheme(final Map schemeMap) throws DotDataException {

        final Date modDate = new Date(ConversionUtils.toLong(schemeMap.get("modDate").toString()));
        new DotConnect().setSQL(INSERT_SCHEME)
                .addParam((String)schemeMap.get("id"))
                .addParam((String)schemeMap.get("name"))
                .addParam((String)schemeMap.get("description"))
                .addParam(Boolean.valueOf(schemeMap.get("archived").toString()))
                .addParam(Boolean.valueOf(schemeMap.get("mandatory").toString()))
                .addParam((String)schemeMap.get("entryActionId"))
                .addParam(Boolean.valueOf(schemeMap.get("defaultScheme").toString()))
                .addParam(modDate)
                .loadResult();
    }

    private boolean existsSystemWorkflow(final String workflowId) throws DotDataException {

         return UtilMethods.isSet(new DotConnect().setSQL(SELECT_SCHEME_SQL)
                 .addParam(workflowId).loadResults());
    }

    private Map getSystemWorkflowMap () throws DotDataException {

        final ObjectMapper mapper          = new ObjectMapper();
        Map   systemWorkflowMap            = null;

        try (final InputStream inputStream = ClassUtils.getResourceAsStream(this.getClass(), SYSTEMWORKFLOW_JSON_PATH)){

            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            systemWorkflowMap = mapper.readValue(inputStream, Map.class);
        } catch (IOException e) {
            Logger.error(this,"Does not exists the resource: " + SYSTEMWORKFLOW_JSON_PATH, e);
            throw new DotDataException(e);
        }

        return systemWorkflowMap;
    }

}
