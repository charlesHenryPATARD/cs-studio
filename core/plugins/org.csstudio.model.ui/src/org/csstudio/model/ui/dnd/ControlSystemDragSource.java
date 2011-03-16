/*******************************************************************************
 * Copyright (c) 2011 Oak Ridge National Laboratory.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.model.ui.dnd;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.csstudio.model.ControlSystemObjectAdapter;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Control;

import static org.csstudio.model.ReflectUtil.*;

/** Utility for allowing Drag-and-Drop "Drag" of Control System Items.
 *
 *  Provides data as control system item as well as string for non-CSS targets.
 *
 *  @author Gabriele Carcassi
 *  @author Kay Kasemir
 */
abstract public class ControlSystemDragSource
{
    final private DragSource source;

    /** Initialize 'drag' source
     *  @param control Control from which control system items may be dragged
     */
    public ControlSystemDragSource(final Control control)
    {
        source = new DragSource(control, DND.DROP_COPY);

        source.addDragListener(new DragSourceAdapter()
        {
        	@Override
        	public void dragStart(DragSourceEvent event) {
        		Object selection = getSelection();
        		
        		// No selection, don't start the drag
        		if (selection == null) {
        			event.doit = false;
        			return;
        		}
        		
        		// Calculate the transfer types:
        		source.setTransfer(supportedTransfers(selection));
        	}
        	
            @Override
            public void dragSetData(final DragSourceEvent event)
            {   // Drag has been performed, provide data
            	Object selection = getSelection();
            	for (Transfer transfer : supportedTransfers(selection)) {
            		if (transfer.isSupportedType(event.dataType)) {
            			if (transfer instanceof SerializableItemTransfer) {
            				SerializableItemTransfer objectTransfer = (SerializableItemTransfer) transfer;
            				event.data = ControlSystemObjectAdapter.convert(selection, objectTransfer.getTragetClass());
            			} else if (transfer instanceof TextTransfer) {
            				// TextTransfer needs String
            				event.data = selection.toString();
            			}
            		}
            	}
            }
        });
    }
    
    private static Collection<String> toArrayClasses(Collection<String> classes) {
    	Collection<String> arrayClasses = new ArrayList<String>();
    	for (String clazz : classes) {
    		arrayClasses.add(toArrayClass(clazz));
    	}
    	return arrayClasses;
    }
    
    private static String toArrayClass(String className) {
		return "[L" + className + ";";
    }
    
    private static List<String> arrayClasses(String[] classes) {
    	List<String> arrayClasses = new ArrayList<String>();
    	for (String clazz : classes) {
    		if (isArray(clazz)) {
        		arrayClasses.add(clazz);
    		}
    	}
    	return arrayClasses;
    }
    
    private static List<String> simpleClasses(String[] classes) {
    	List<String> arrayClasses = new ArrayList<String>();
    	for (String clazz : classes) {
    		if (!isArray(clazz)) {
        		arrayClasses.add(clazz);
    		}
    	}
    	return arrayClasses;
    }
    
    private static List<Transfer> supportedSingleTransfers(Object selection) {
    	if (selection.getClass().isArray())
    		throw new RuntimeException("Something wrong: you are asking for single transfers for an array");
		String[] types = ControlSystemObjectAdapter.getAdaptableTypes(selection.getClass());
		List<Transfer> supportedTransfers = new ArrayList<Transfer>();
		if (selection instanceof Serializable) {
			supportedTransfers.add(SerializableItemTransfer.getTransfer(((Serializable)selection).getClass()));
		}
		supportedTransfers.addAll(SerializableItemTransfer.getTransfers(simpleClasses(types)));
		supportedTransfers.add(TextTransfer.getInstance());
		return supportedTransfers;
    }
    
    private static List<Transfer> supportedArrayTransfers(Object[] selection) {
    	if (!selection.getClass().isArray())
    		throw new RuntimeException("Something wrong: you are asking for array transfers for an single object");
		String[] types = ControlSystemObjectAdapter.getAdaptableTypes(selection[0].getClass());
		List<Transfer> supportedTransfers = new ArrayList<Transfer>();
		if (Serializable.class.isAssignableFrom(selection.getClass().getComponentType())) {
			supportedTransfers.add(SerializableItemTransfer.getTransfer(((Serializable)selection).getClass()));
		}
		supportedTransfers.addAll(SerializableItemTransfer.getTransfers(toArrayClasses(simpleClasses(types))));
		supportedTransfers.addAll(SerializableItemTransfer.getTransfers(arrayClasses(types)));
		supportedTransfers.add(TextTransfer.getInstance());
		return supportedTransfers;
    }
    
    private static Transfer[] supportedTransfers(Object selection) {
    	Object singleSelection;
    	Object[] arraySelection;
    	if (selection instanceof Object[]) {
    		// Selection is an array
    		arraySelection = (Object[]) selection;
    		
    		if (Array.getLength(selection) == 0) {
    			// if empty, no transfers
    			return new Transfer[0];
    		} else if (Array.getLength(selection) == 1) {
    			// if size one, set single selection
    			singleSelection = Array.get(selection, 0);
    		} else {
    			// no single selection
    			singleSelection = null;
    		}
    	} else {
    		// If it's a single value, the single selection is the
    		// object and the array is an array with just one element
    		singleSelection = selection;
    		arraySelection = (Object[]) Array.newInstance(selection.getClass(), 1);
    		arraySelection[0] = singleSelection;
    	}
    	
		Set<Transfer> supportedTransfers = new HashSet<Transfer>();
		// Add single type support, if needed
		if (singleSelection != null) {
			supportedTransfers.addAll(supportedSingleTransfers(singleSelection));
		}
		// Add array type support
		supportedTransfers.addAll(supportedArrayTransfers(arraySelection));
    	System.out.println("Selection " + selection + " supported " + supportedTransfers);
		return supportedTransfers.toArray(new Transfer[supportedTransfers.size()]);
    }

    /** To be implemented by derived class:
     *  Provide the control system items that should be 'dragged'
     *  from this drag source
     *  @return the selection (can be single object or array)
     */
    abstract public Object getSelection();
	
}
