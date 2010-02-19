package org.csstudio.trends.databrowser.ui;

import org.csstudio.platform.model.IArchiveDataSource;
import org.csstudio.swt.xygraph.undo.IUndoableCommand;
import org.csstudio.swt.xygraph.undo.OperationsManager;
import org.csstudio.trends.databrowser.Messages;
import org.csstudio.trends.databrowser.model.ArchiveDataSource;
import org.csstudio.trends.databrowser.model.AxisConfig;
import org.csstudio.trends.databrowser.model.FormulaInput;
import org.csstudio.trends.databrowser.model.FormulaItem;
import org.csstudio.trends.databrowser.model.Model;
import org.csstudio.trends.databrowser.model.ModelItem;
import org.csstudio.trends.databrowser.model.PVItem;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;

/** Undo-able command to add a ModelItem to the Model
 *  @author Kay Kasemir
 */
public class AddModelItemCommand implements IUndoableCommand
{
    final private Shell shell;
    final private Model model;
    final private ModelItem item;

    /** Create PV via undo-able AddModelItemCommand,
     *  displaying errors in dialog
     *  @param shell Shell used for error dialogs
     *  @param operations_manager OperationsManager where command will be reg'ed
     *  @param model Model were PV is to be added
     *  @param pv_name Name of new PV
     *  @param period scan period
     *  @param axis Axis
     *  @param archive Archive data source
     *  @return AddModelItemCommand or <code>null</code> on error
     */
    public static AddModelItemCommand forPV(final Shell shell, 
            final OperationsManager operations_manager,
            final Model model,
            final String pv_name,
            final double period,
            final AxisConfig axis,
            final IArchiveDataSource archive)
    {
        // Create item
        final PVItem item;
        try
        {
            item = new PVItem(pv_name, period);
            if (archive != null)
                item.addArchiveDataSource(new ArchiveDataSource(archive));
            else
                item.useDefaultArchiveDataSources();
            item.setAxis(axis);
        }
        catch (Exception ex)
        {
            MessageDialog.openError(shell,
                    Messages.Error,
                    NLS.bind(Messages.AddItemErrorFmt, pv_name, ex.getMessage()));
            return null;
        }
        // Add to model via undo-able command
        return new AddModelItemCommand(shell, operations_manager, model, item);
    }

    /** Create PV via undo-able AddModelItemCommand,
     *  displaying errors in dialog
     *  @param shell Shell used for error dialogs
     *  @param operations_manager OperationsManager where command will be reg'ed
     *  @param model Model were PV is to be added
     *  @param axis Axis
     *  @return AddModelItemCommand or <code>null</code> on error
     */
    public static AddModelItemCommand forFormula(final Shell shell, 
            final OperationsManager operations_manager,
            final Model model,
            final String formula_name,
            final AxisConfig axis)
    {
        // Create item
        final FormulaItem item;
        try
        {
            item = new FormulaItem(formula_name, "0", new FormulaInput[0]); //$NON-NLS-1$
            item.setAxis(axis);
        }
        catch (Exception ex)
        {
            MessageDialog.openError(shell,
                    Messages.Error,
                    NLS.bind(Messages.AddItemErrorFmt, formula_name, ex.getMessage()));
            return null;
        }
        // Add to model via undo-able command
        return new AddModelItemCommand(shell, operations_manager, model, item);
    }

    
    /** Register and perform the command
     *  @param shell Shell used for error dialogs
     *  @param operations_manager OperationsManager where command will be reg'ed
     *  @param model Model were PV is to be added
     *  @param item Item to add
     */
    public AddModelItemCommand(final Shell shell, 
            final OperationsManager operations_manager,
            final Model model,
            final ModelItem item)
    {
        this.shell = shell;
        this.model = model;
        this.item = item;
        try
        {
            model.addItem(item);
        }
        catch (Exception ex)
        {
            MessageDialog.openError(shell,
                    Messages.Error,
                    NLS.bind(Messages.AddItemErrorFmt, item.getName(), ex.getMessage()));
            // Exit before registering for undo because there's nothing to undo
            return;
        }
        operations_manager.addCommand(this);
    }
    
    public ModelItem getItem()
    {
        return item;
    }

    /** {@inheritDoc} */
    public void redo()
    {
        try
        {
            model.addItem(item);
        }
        catch (Exception ex)
        {
            MessageDialog.openError(shell,
                    Messages.Error,
                    NLS.bind(Messages.AddItemErrorFmt, item.getName(), ex.getMessage()));
        }
    }

    /** {@inheritDoc} */
    public void undo()
    {
        model.removeItem(item);
    }
    
    /** @return Command name that appears in undo/redo menu */
    @Override
    public String toString()
    {
        return Messages.AddPV;
    }
}
