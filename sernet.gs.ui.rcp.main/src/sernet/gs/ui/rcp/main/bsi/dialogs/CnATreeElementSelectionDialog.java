/*******************************************************************************
 * Copyright (c) 2010 Alexander Koderman <ak@sernet.de>.
 * This program is free software: you can redistribute it and/or 
 * modify it under the terms of the GNU General Public License 
 * as published by the Free Software Foundation, either version 3 
 * of the License, or (at your option) any later version.
 *     This program is distributed in the hope that it will be useful,    
 * but WITHOUT ANY WARRANTY; without even the implied warranty 
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 * See the GNU General Public License for more details.
 *     You should have received a copy of the GNU General Public 
 * License along with this program. 
 * If not, see <http://www.gnu.org/licenses/>.
 * 
 * Contributors:
 *     Alexander Koderman <ak@sernet.de> - initial API and implementation
 ******************************************************************************/
package sernet.gs.ui.rcp.main.bsi.dialogs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import sernet.gs.ui.rcp.main.Activator;
import sernet.gs.ui.rcp.main.ExceptionUtil;
import sernet.gs.ui.rcp.main.ImageCache;
import sernet.gs.ui.rcp.main.bsi.editors.InputHelperFactory;
import sernet.gs.ui.rcp.main.bsi.model.TodoViewItem;
import sernet.gs.ui.rcp.main.common.model.CnAPlaceholder;
import sernet.gs.ui.rcp.main.common.model.PlaceHolder;
import sernet.gs.ui.rcp.main.service.ServiceFactory;
import sernet.gs.ui.rcp.main.service.crudcommands.LoadCnAElementByEntityTypeId;
import sernet.gs.ui.rcp.main.service.crudcommands.LoadCnATreeElementTitles;
import sernet.gs.ui.rcp.main.service.taskcommands.FindMassnahmenForITVerbund;
import sernet.hui.common.connect.Entity;
import sernet.hui.swt.widgets.Colors;
import sernet.hui.swt.widgets.HitroUIComposite;
import sernet.snutils.DBException;
import sernet.verinice.interfaces.CommandException;
import sernet.verinice.model.common.CnATreeElement;

/**
 * Dialog to allow speedy selection of multiple elements of a given type, with filter function.
 * 
 * @author koderman@sernet.de
 * @version $Rev$ $LastChangedDate$ 
 * $LastChangedBy$
 *
 */
public class CnATreeElementSelectionDialog extends Dialog {

    private String entityType;
    private List<CnATreeElement> selectedElements = new ArrayList<CnATreeElement>();

    private TableViewer viewer;
    private TableViewerColumn column1;
    private TableViewerColumn column2;
    private Text text;
    private CnaTreeElementTitleFilter filter;
    private List<CnATreeElement> input;
    private static final String COLUMN_IMG = "_img";
    private static final String COLUMN_LABEL = "_label";
    
    /**
     * @param shell
     * @param selectedType
     */
    public CnATreeElementSelectionDialog(Shell shell, String selectedType) {
        super(shell);
        setShellStyle(SWT.MAX | SWT.CLOSE | SWT.TITLE | SWT.BORDER | SWT.APPLICATION_MODAL | SWT.RESIZE);
        this.entityType = selectedType;
    }
    
    
    
    @Override
    protected void configureShell(Shell newShell) {
        // FIXME externalize strings
        super.configureShell(newShell);
        newShell.setText("Choose elements");
        newShell.setSize(400, 500);
        
        // open the window right under the mouse pointer:
        Point cursorLocation = Display.getCurrent().getCursorLocation();
        newShell.setLocation(new Point(cursorLocation.x-200, cursorLocation.y-250));
    }
    
    @Override
    protected Control createDialogArea(Composite parent) {
        Composite container = (Composite) super.createDialogArea(parent);
        container.setLayout(new FormLayout());
        
        // FIXME externalize strings
        
        Label label1 = new Label(container, SWT.NULL);
        label1.setText("Enter name prefix or pattern: ");

        FormData formData = new FormData();
        formData.top = new FormAttachment(0, 5);
        formData.left = new FormAttachment(0, 5);
        label1.setLayoutData(formData);
        label1.pack();
        
        text = new Text(container, SWT.BORDER);
        FormData formData2 = new FormData();
        formData2.top = new FormAttachment(0, 5);
        formData2.left = new FormAttachment(label1, 5);
        formData2.right = new FormAttachment(100, -5);
        text.setLayoutData(formData2);
        text.addKeyListener(new KeyListener() {
            @Override
            public void keyReleased(KeyEvent e) {
                filter.setPattern(text.getText());
            }
            
            @Override
            public void keyPressed(KeyEvent e) {
            }
        });
        
      
        
        viewer = new TableViewer(container, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
        FormData formData3 = new FormData();
        formData3.top = new FormAttachment(text, 5);
        formData3.left = new FormAttachment(0, 5);
        formData3.right = new FormAttachment(100, -5);
        formData3.bottom = new FormAttachment(100, -5);
        viewer.getTable().setLayoutData(formData3);
        viewer.getTable().setHeaderVisible(false);
        viewer.getTable().setLinesVisible(true);
        
        // image column:
        column1 = new TableViewerColumn(viewer, SWT.LEFT);
        column1.getColumn().setText("");
        column1.getColumn().setWidth(25);
        column1.getColumn().setResizable(false);
        column1.setLabelProvider(new CellLabelProvider() {
            public void update(ViewerCell cell) {
                if (cell.getElement() instanceof PlaceHolder)
                    return;
                
                String typeId = ((CnATreeElement)cell.getElement()).getTypeId();
                cell.setImage(ImageCache.getInstance().getObjectTypeImage(typeId));
            }
        });
        
        // label column:
        column2 = new TableViewerColumn(viewer, SWT.LEFT);
        column2.getColumn().setWidth(200);
        column2.setLabelProvider(new CellLabelProvider() {
            public void update(ViewerCell cell) {
                if (cell.getElement() instanceof PlaceHolder) {
                    cell.setText( ((PlaceHolder)cell.getElement()).getTitle() );
                    return;
                }
                    
                cell.setText( ((CnATreeElement)cell.getElement()).getTitle() );
            }
        });
        
        viewer.setColumnProperties(new String[] {COLUMN_IMG, COLUMN_LABEL});
        viewer.setContentProvider(new ArrayContentProvider());
        filter = new CnaTreeElementTitleFilter(viewer);
        viewer.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                selectedElements = ((IStructuredSelection)viewer.getSelection()).toList();
            }
        });
        
        loadElements();
        
        return container;
    }

    /**
     * @return
     */
    public List<CnATreeElement> getSelectedElements() {
        return selectedElements;
    }



    /**
     * Loads all elements for element type.
     */
    private void loadElements() {
        if (entityType == null || entityType.length()==0)
            return;
        
        ArrayList temp = new ArrayList(1);
        temp.add(new PlaceHolder("Loading elements..."));
        viewer.setInput(temp);
        
        // FIXME externalize strings
        
        WorkspaceJob job = new WorkspaceJob("Loading elements") {
            @Override
            public IStatus runInWorkspace(final IProgressMonitor monitor) {
                Activator.inheritVeriniceContextState();

                try {
                    monitor.setTaskName("Loading elements");
                    LoadCnAElementByEntityTypeId command = new LoadCnAElementByEntityTypeId(entityType);
                    command = ServiceFactory.lookupCommandService().executeCommand(command);
                    input = command.getElements();
                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            viewer.setInput(input);
                        }
                    });
                } catch (Exception e) {
                    ExceptionUtil.log(e, "Could not load elements from database.");
                }
                return Status.OK_STATUS;
            }
        };
        job.setUser(false);
        job.schedule();
        
    }

}

